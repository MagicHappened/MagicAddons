package org.magic.magicaddons.data.greenhouse.transfer

import java.math.BigInteger
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction

/** skylayouts.io links: `1<mutation><interval>~p<board>~<board>~<board>` */
object SkyLayoutsFormat : LayoutFormat {

    override val displayName: String = "SkyLayouts"

    private const val SHARE_URL: String = "https://skylayouts.io/l/"

    private const val ALPHABET: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val ALPHABET_BASE: BigInteger = BigInteger.valueOf(64)
    private val LETTER_MASK: BigInteger = BigInteger.valueOf(63)

    private val SITE_CROP_IDS: List<String> = listOf(
        "ALL_IN_ALOE", "ASHWREATH", "BLASTBERRY", "BROWN_MUSHROOM", "CACTUS", "CARROT", "CHEESEBITE",
        "CHLORONITE", "CHOCOBERRY", "CHOCONUT", "CHORUS_FRUIT", "CINDERSHADE", "COALROOT", "COCOA_BEANS",
        "CREAMBLOOM", "DEAD_PLANT", "DEVOURER", "DO_NOT_EAT_SHROOM", "DUSKBLOOM", "DUSTGRAIN", "FERMENTO",
        "FIRE", "FLESHTRAP", "GLASSCORN", "GLOOMGOURD", "GODSEED", "LONELILY", "MAGIC_JELLYBEAN", "MELON",
        "MOONFLOWER", "NETHER_WART", "NOCTILUME", "PHANTOMLEAF", "PLANTBOY_ADVANCE", "POTATO", "PUFFERCLOUD",
        "PUMPKIN", "RED_MUSHROOM", "SCOURROOT", "SHADEVINE", "SHELLFRUIT", "SNOOZLING", "SOGGYBUD",
        "STARTLEVINE", "STOPLIGHT_PETAL", "SUGAR_CANE", "SUNFLOWER", "THORNSHADE", "THUNDERLING", "TIMESTALK",
        "TURTLELLINI", "VEILSHROOM", "WHEAT", "WILD_ROSE", "WITHERBLOOM", "ZOMBUD"
    )

    private const val SITE_DEFAULT_VISIT_INTERVAL: Int = 8

    private val siteCropIndexOf: Map<CropDefinition, Int> by lazy {
        buildMap { SITE_CROP_IDS.forEachIndexed { index, id -> CropRegistry.findByLooseName(id)?.let { putIfAbsent(it, index) } } }
    }

    private fun alphabetLetter(value: Int): Char = ALPHABET[value]
    private fun alphabetValue(letter: Char): Int = ALPHABET.indexOf(letter)

    override fun canImport(text: String): Boolean = linkCodeOf(text) != null

    private fun linkCodeOf(text: String): String? {
        val raw = text.trim().substringAfter("/l/", text.trim()).substringBefore('?').substringBefore('#')
        val head = raw.substringBefore('~')
        if (head.length < 3 || head[0] != '1' || head.any { alphabetValue(it) < 0 }) return null
        if (!raw.substringAfter('~', "").startsWith("p")) return null
        return raw
    }

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val code = linkCodeOf(text) ?: return LayoutTransferResult.Failure("Invalid SkyLayouts link.")
        val notes = mutableListOf<String>()

        val boards = code.substringAfter('~').drop(1).split('~').filter { it.isNotEmpty() }
        if (boards.isEmpty()) return LayoutTransferResult.Failure("The SkyLayouts link holds no plot.")

        val head = code.substringBefore('~')
        val target = SITE_CROP_IDS.getOrNull(alphabetValue(head[1]) - 1)?.let { CropRegistry.findByLooseName(it) }
        val spotsFromNeighbours = target?.spawnRule?.let { it.needsNoNeighbours || it.requiredNeighbourCells.isNotEmpty() } == true
        if (target != null && !spotsFromNeighbours) {
            notes.add("${target.name} spawn spots cannot be determined automatically, add them by hand.")
        }

        val layouts = boards.take(GreenhouseLayout.MAX_PLOTS).mapIndexed { number, board ->
            readBoardIntoPlot(board, GreenhouseLayout.plotId(layoutId, number), target.takeIf { spotsFromNeighbours }, notes)
                ?: return LayoutTransferResult.Failure("Could not read plot ${number + 1} of the SkyLayouts link.")
        }
        if (boards.size > GreenhouseLayout.MAX_PLOTS) notes.add("Only the first ${GreenhouseLayout.MAX_PLOTS} plots were taken.")

        if (layouts.size > 1) notes.add("Imported ${layouts.size} plots as one preset.")

        return LayoutTransferResult.Imported(layouts.first(), notes, layouts.drop(1))
    }

    private fun readBoardIntoPlot(board: String, id: String, target: CropDefinition?, notes: MutableList<String>): PlotLayout? {
        if (board.length < 3 || board[0] != '3') return null
        val boardSize = alphabetValue(board[1])
        val cropTypeCount = alphabetValue(board[2])
        if (boardSize <= 0 || cropTypeCount < 0 || board.length < 3 + cropTypeCount) return null

        val cropTypes = board.substring(3, 3 + cropTypeCount).map { SITE_CROP_IDS.getOrNull(alphabetValue(it)) ?: return null }

        var packedCells = BigInteger.ZERO
        for (letter in board.substring(3 + cropTypeCount)) {
            val value = alphabetValue(letter)
            if (value < 0) return null
            packedCells = packedCells.multiply(ALPHABET_BASE).add(BigInteger.valueOf(value.toLong()))
        }

        // the cell digits follow a leading 1 and are read last cell first
        val cellDigitBase = BigInteger.valueOf(cropTypeCount + 1L)
        val cellCropTypeIndices = ArrayList<Int>(boardSize * boardSize)
        while (packedCells > BigInteger.ONE) {
            val (rest, digit) = packedCells.divideAndRemainder(cellDigitBase)
            cellCropTypeIndices.add(digit.toInt() - 1)
            packedCells = rest
        }
        cellCropTypeIndices.reverse()
        if (cellCropTypeIndices.size != boardSize * boardSize) return null

        val layout = PlotLayout(id = id)
        val unknownCropIds = mutableSetOf<String>()

        for (y in 0 until minOf(boardSize, layout.size)) {
            for (x in 0 until minOf(boardSize, layout.size)) {
                val cropTypeIndex = cellCropTypeIndices[y * boardSize + x]
                if (cropTypeIndex < 0 || cropTypeIndex >= cropTypes.size || layout.plantCovering(x, y) != null) continue

                val definition = CropRegistry.findByLooseName(cropTypes[cropTypeIndex])
                if (definition == null) {
                    unknownCropIds.add(cropTypes[cropTypeIndex])
                    continue
                }
                layout.placeImportedPlant(definition, x, y, null)
            }
        }
        unknownCropIds.forEach { notes.add("Unknown crop: $it") }

        if (target != null) placeTargetSpots(layout, target)
        return layout
    }

    private fun placeTargetSpots(layout: PlotLayout, target: CropDefinition) {
        val footprint = target.footprint
        for (y in 0..layout.size - footprint.height) {
            for (x in 0..layout.size - footprint.width) {
                val isFree = footprint.cellsFrom(x, y).all { (cellX, cellY) -> layout.plantCovering(cellX, cellY) == null }
                if (isFree && PlotPrediction.missingNeighbourConditions(layout, target, x, y).isEmpty()) {
                    layout.placeImportedPlant(target, x, y, LayoutSlot.Marking.Target)
                }
            }
        }
    }

    override fun export(layout: PlotLayout): LayoutTransferResult = writeLink(listOf(layout))

    override fun exportAll(master: GreenhouseLayout): LayoutTransferResult = writeLink(master.plots)

    private fun writeLink(plots: List<PlotLayout>): LayoutTransferResult {
        val notes = mutableListOf<String>()

        val target = plots.flatMap { it.plants }
            .firstOrNull { it.slot.mark == LayoutSlot.Marking.Target && it.cropDef.isMutation }
            ?.let { siteCropIndexOf[it.cropDef] }
        val head = "1" + alphabetLetter(target?.plus(1) ?: 0) + alphabetLetter(SITE_DEFAULT_VISIT_INTERVAL)

        val boards = plots.take(GreenhouseLayout.MAX_PLOTS).map { writeBoard(it, notes) }
        if (plots.size > GreenhouseLayout.MAX_PLOTS) notes.add("Only the first ${GreenhouseLayout.MAX_PLOTS} plots were written.")

        return LayoutTransferResult.Exported(SHARE_URL + head + "~p" + boards.joinToString("~"), notes)
    }

    private fun writeBoard(layout: PlotLayout, notes: MutableList<String>): String {
        val cropTypes = mutableListOf<Int>()
        val cellCropTypeIndices = IntArray(layout.size * layout.size) { -1 }

        layout.plants.forEach { plant ->
            if (plant.slot.mark == LayoutSlot.Marking.Target && plant.cropDef.isMutation) return@forEach

            val siteCropIndex = siteCropIndexOf[plant.cropDef] ?: run {
                notes.add("${plant.cropDef.name} is not on SkyLayouts and was left out.")
                return@forEach
            }
            val cropTypeIndex = cropTypes.indexOf(siteCropIndex).takeIf { it >= 0 } ?: cropTypes.size.also { cropTypes.add(siteCropIndex) }
            for (dx in 0 until plant.cropDef.footprint.width) {
                for (dy in 0 until plant.cropDef.footprint.height) {
                    val x = plant.slot.x + dx
                    val y = plant.slot.y + dy
                    if (x < layout.size && y < layout.size) cellCropTypeIndices[y * layout.size + x] = cropTypeIndex
                }
            }
        }

        val cellDigitBase = BigInteger.valueOf(cropTypes.size + 1L)
        var packedCells = BigInteger.ONE
        cellCropTypeIndices.forEach { packedCells = packedCells.multiply(cellDigitBase).add(BigInteger.valueOf(it + 1L)) }

        val boardLetters = StringBuilder()
        while (packedCells > BigInteger.ZERO) {
            boardLetters.append(alphabetLetter(packedCells.and(LETTER_MASK).toInt()))
            packedCells = packedCells.shiftRight(6)
        }

        return "3" + alphabetLetter(layout.size) + alphabetLetter(cropTypes.size) + cropTypes.joinToString("") { alphabetLetter(it).toString() } + boardLetters.reverse()
    }
}
