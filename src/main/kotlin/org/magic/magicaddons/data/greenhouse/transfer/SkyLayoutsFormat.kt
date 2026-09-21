package org.magic.magicaddons.data.greenhouse.transfer

import java.math.BigInteger
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout

/**
 * Layouts as skylayouts.io shares them: `1<mutation><interval>~p<board>~<board>~<board>`, one
 * board a plot. A board is `3<size><kinds><kind ids…>` then the cells as one big number, base
 * kinds + 1, written in the site's 64-letter alphabet. A wide plant is written in every cell it covers.
 */
object SkyLayoutsFormat : LayoutFormat {

    override val displayName: String = "SkyLayouts"

    private const val URL: String = "https://skylayouts.io/l/"

    private const val ALPHABET: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    /** The site's crop ids, in the order it numbers them. */
    private val KINDS: List<String> = listOf(
        "ALL_IN_ALOE", "ASHWREATH", "BLASTBERRY", "BROWN_MUSHROOM", "CACTUS", "CARROT", "CHEESEBITE",
        "CHLORONITE", "CHOCOBERRY", "CHOCONUT", "CHORUS_FRUIT", "CINDERSHADE", "COALROOT", "COCOA_BEANS",
        "CREAMBLOOM", "DEAD_PLANT", "DEVOURER", "DO_NOT_EAT_SHROOM", "DUSKBLOOM", "DUSTGRAIN", "FERMENTO",
        "FIRE", "FLESHTRAP", "GLASSCORN", "GLOOMGOURD", "GODSEED", "LONELILY", "MAGIC_JELLYBEAN", "MELON",
        "MOONFLOWER", "NETHER_WART", "NOCTILUME", "PHANTOMLEAF", "PLANTBOY_ADVANCE", "POTATO", "PUFFERCLOUD",
        "PUMPKIN", "RED_MUSHROOM", "SCOURROOT", "SHADEVINE", "SHELLFRUIT", "SNOOZLING", "SOGGYBUD",
        "STARTLEVINE", "STOPLIGHT_PETAL", "SUGAR_CANE", "SUNFLOWER", "THORNSHADE", "THUNDERLING", "TIMESTALK",
        "TURTLELLINI", "VEILSHROOM", "WHEAT", "WILD_ROSE", "WITHERBLOOM", "ZOMBUD"
    )

    /** The site's default for how often the greenhouse is visited, in stages; the mod tracks no such thing. */
    private const val VISIT_INTERVAL: Int = 8

    private val kindOf: Map<CropDefinition, Int> by lazy {
        buildMap { KINDS.forEachIndexed { index, id -> CropRegistry.findByLooseName(id)?.let { putIfAbsent(it, index) } } }
    }

    private fun letter(index: Int): Char = ALPHABET[index]
    private fun index(letter: Char): Int = ALPHABET.indexOf(letter)

    override fun canImport(text: String): Boolean = codeOf(text) != null

    /** The share code out of a link or pasted on its own. */
    private fun codeOf(text: String): String? {
        val raw = text.trim().substringAfter("/l/", text.trim()).substringBefore('?').substringBefore('#')
        val head = raw.substringBefore('~')
        if (head.length < 3 || head[0] != '1' || head.any { index(it) < 0 }) return null
        if (!raw.substringAfter('~', "").startsWith("p")) return null
        return raw
    }

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val code = codeOf(text) ?: return LayoutTransferResult.Failure("Invalid SkyLayouts link.")
        val notes = mutableListOf<String>()

        val boards = code.substringAfter('~').drop(1).split('~').filter { it.isNotEmpty() }
        if (boards.isEmpty()) return LayoutTransferResult.Failure("The SkyLayouts link holds no plot.")

        // the link names the mutation the layout grows; the site shows it at every empty cell
        val head = code.substringBefore('~')
        val target = KINDS.getOrNull(index(head[1]) - 1)?.let { CropRegistry.findByLooseName(it) }

        val layouts = boards.take(GreenhouseLayout.MAX_PLOTS).mapIndexed { number, board ->
            readBoard(board, GreenhouseLayout.plotId(layoutId, number), target, notes)
                ?: return LayoutTransferResult.Failure("Could not read plot ${number + 1} of the SkyLayouts link.")
        }
        if (boards.size > GreenhouseLayout.MAX_PLOTS) notes.add("Only the first ${GreenhouseLayout.MAX_PLOTS} plots were taken.")

        if (layouts.size > 1) notes.add("Imported ${layouts.size} plots as one preset.")

        return LayoutTransferResult.Imported(layouts.first(), notes, layouts.drop(1))
    }

    /**
     * One plot's cells into a layout; a plant covering several cells is one plant here. Every
     * empty cell gets the [target] mutation, marked as the target, the way the site shows spawn spots.
     */
    private fun readBoard(board: String, id: String, target: CropDefinition?, notes: MutableList<String>): PlotLayout? {
        if (board.length < 3 || board[0] != '3') return null
        val size = index(board[1])
        val kindCount = index(board[2])
        if (size <= 0 || kindCount < 0 || board.length < 3 + kindCount) return null

        val kinds = board.substring(3, 3 + kindCount).map { KINDS.getOrNull(index(it)) ?: return null }

        var number = BigInteger.ZERO
        val sixtyFour = BigInteger.valueOf(64)
        for (letter in board.substring(3 + kindCount)) {
            val value = index(letter)
            if (value < 0) return null
            number = number.multiply(sixtyFour).add(BigInteger.valueOf(value.toLong()))
        }

        // the cells sit above a leading one, least significant last, so they come out reversed
        val base = BigInteger.valueOf(kindCount + 1L)
        val cells = ArrayList<Int>(size * size)
        while (number > BigInteger.ONE) {
            val (rest, digit) = number.divideAndRemainder(base)
            cells.add(digit.toInt() - 1)
            number = rest
        }
        cells.reverse()
        if (cells.size != size * size) return null

        val layout = PlotLayout(id = id)
        val taken = Array(layout.size) { BooleanArray(layout.size) }
        val unknown = mutableSetOf<String>()

        for (y in 0 until minOf(size, layout.size)) {
            for (x in 0 until minOf(size, layout.size)) {
                val kind = cells[y * size + x]
                if (kind < 0 || kind >= kinds.size || taken[x][y]) continue

                val def = CropRegistry.findByLooseName(kinds[kind])
                if (def == null) {
                    unknown.add(kinds[kind])
                    continue
                }
                val slot = layout.getSlot(x, y) ?: continue

                layout.plants.add(Plant(def.elementId, slot, cropDef = def))
                val soil = def.requiredSoil.firstOrNull()?.defaultBlockState()
                for (dx in 0 until def.footprint.width) {
                    for (dy in 0 until def.footprint.height) {
                        if (x + dx >= layout.size || y + dy >= layout.size) continue
                        taken[x + dx][y + dy] = true
                        soil?.let { layout.getSlot(x + dx, y + dy)?.soil = it }
                    }
                }
            }
        }
        unknown.forEach { notes.add("Unknown crop: $it") }

        if (target != null && target.footprint.width == 1 && target.footprint.height == 1) {
            for (y in 0 until layout.size) {
                for (x in 0 until layout.size) {
                    if (taken[x][y]) continue
                    val slot = layout.getSlot(x, y) ?: continue
                    slot.mark = LayoutSlot.Marking.Target
                    target.requiredSoil.firstOrNull()?.let { slot.soil = it.defaultBlockState() }
                    layout.plants.add(Plant(target.elementId, slot, cropDef = target))
                }
            }
        } else if (target != null) {
            notes.add("${target.name} spawn spots were left empty: it is wider than one cell.")
        }
        return layout
    }

    override fun export(layout: PlotLayout): LayoutTransferResult = exportPlots(listOf(layout))

    override fun exportAll(master: GreenhouseLayout): LayoutTransferResult = exportPlots(master.plots)

    private fun exportPlots(plots: List<PlotLayout>): LayoutTransferResult {
        val notes = mutableListOf<String>()

        // the site files a layout under the mutation it grows: the plant marked as the target
        val target = plots.flatMap { it.plants }
            .firstOrNull { it.slot.mark == LayoutSlot.Marking.Target && it.cropDef.isMutation }
            ?.let { kindOf[it.cropDef] }
        val head = "1" + letter(target?.plus(1) ?: 0) + letter(VISIT_INTERVAL)

        val boards = plots.take(GreenhouseLayout.MAX_PLOTS).map { writeBoard(it, notes) }
        if (plots.size > GreenhouseLayout.MAX_PLOTS) notes.add("Only the first ${GreenhouseLayout.MAX_PLOTS} plots were written.")

        return LayoutTransferResult.Exported(URL + head + "~p" + boards.joinToString("~"), notes)
    }

    private fun writeBoard(layout: PlotLayout, notes: MutableList<String>): String {
        val kinds = mutableListOf<Int>()
        val cells = IntArray(layout.size * layout.size) { -1 }

        layout.plants.forEach { instance ->
            // the site keeps the target's spots empty and names the mutation in the link instead
            if (instance.slot.mark == LayoutSlot.Marking.Target && instance.cropDef.isMutation) return@forEach

            val kind = kindOf[instance.cropDef] ?: run {
                notes.add("${instance.cropDef.name} is not on SkyLayouts and was left out.")
                return@forEach
            }
            val at = kinds.indexOf(kind).takeIf { it >= 0 } ?: kinds.size.also { kinds.add(kind) }
            for (dx in 0 until instance.cropDef.footprint.width) {
                for (dy in 0 until instance.cropDef.footprint.height) {
                    val x = instance.slot.x + dx
                    val y = instance.slot.y + dy
                    if (x < layout.size && y < layout.size) cells[y * layout.size + x] = at
                }
            }
        }

        val base = BigInteger.valueOf(kinds.size + 1L)
        var number = BigInteger.ONE
        cells.forEach { number = number.multiply(base).add(BigInteger.valueOf(it + 1L)) }

        val digits = StringBuilder()
        val sixtyThree = BigInteger.valueOf(63)
        while (number > BigInteger.ZERO) {
            digits.append(letter(number.and(sixtyThree).toInt()))
            number = number.shiftRight(6)
        }

        return "3" + letter(layout.size) + letter(kinds.size) + kinds.joinToString("") { letter(it).toString() } + digits.reverse()
    }
}
