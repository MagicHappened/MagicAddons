package org.magic.magicaddons.data.greenhouse.transfer

import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.plot.GREENHOUSE_SIZE
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout

/** greenhouse.skyshards.com links: `inputs|targets|grid`, raw-deflated in url-safe base64 */
object SkyShardsFormat : LayoutFormat {

    override val displayName: String = "SkyShards"

    private const val SHARE_URL: String = "https://greenhouse.skyshards.com/designer?layout="

    private const val GRID_SIZE: Int = GREENHOUSE_SIZE
    private const val CELL_COUNT: Int = GRID_SIZE * GRID_SIZE

    private const val ALPHABET: String = "abcdefghijklmnopqrstuvwxyz"

    private const val MAX_SINGLE_LETTER_CROP_TYPES: Int = 26

    private val BASE_CROP_IDS: List<String> = listOf(
        "wheat", "potato", "carrot", "pumpkin", "melon", "cocoa_beans", "sugar_cane", "cactus",
        "nether_wart", "red_mushroom", "brown_mushroom", "moonflower", "sunflower", "wild_rose",
        "fire", "dead_plant", "fermento"
    )

    private val MUTATION_IDS: List<String> = listOf(
        "ashwreath", "choconut", "dustgrain", "gloomgourd", "lonelily", "scourroot", "shadevine",
        "veilshroom", "witherbloom", "chocoberry", "cindershade", "coalroot", "creambloom",
        "duskbloom", "thornshade", "blastberry", "cheesebite", "chloronite", "do_not_eat_shroom",
        "fleshtrap", "magic_jellybean", "noctilume", "snoozling", "soggybud", "chorus_fruit",
        "plantboy_advance", "puffercloud", "shellfruit", "startlevine", "stoplight_petal",
        "thunderling", "turtlellini", "zombud", "all_in_aloe", "devourer", "glasscorn", "godseed",
        "jerryflower", "phantomleaf", "timestalk"
    )

    private val siteIdOf: Map<CropDefinition, String> by lazy {
        buildMap {
            (BASE_CROP_IDS + MUTATION_IDS).forEach { id -> CropRegistry.findByLooseName(id)?.let { putIfAbsent(it, id) } }
        }
    }

    private fun siteIdAt(index: Int): String? = when {
        index < 0 -> null
        index < BASE_CROP_IDS.size -> BASE_CROP_IDS[index]
        else -> MUTATION_IDS.getOrNull(index - BASE_CROP_IDS.size)
    }

    private fun siteIndexOf(id: String): Int {
        val base = BASE_CROP_IDS.indexOf(id)
        if (base >= 0) return base

        val mutation = MUTATION_IDS.indexOf(id)
        return if (mutation >= 0) BASE_CROP_IDS.size + mutation else -1
    }

    private fun decode(share: String): String? = RawDeflate.decode(share)?.toString(Charsets.UTF_8)

    private fun encode(text: String): String = RawDeflate.encode(text.toByteArray(Charsets.UTF_8))

    private fun linkCodeOf(text: String): String = text.trim().let {
        when {
            "layout=" in it -> it.substringAfter("layout=").substringBefore('&')
            "/share/" in it -> it.substringAfter("/share/").substringBefore('?')
            else -> it
        }
    }

    override fun canImport(text: String): Boolean {
        val decoded = decode(linkCodeOf(text)) ?: return false
        return decoded.count { it == '|' } == 2
    }

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val decoded = decode(linkCodeOf(text))
            ?: return LayoutTransferResult.Failure("Failed to decode SkyShards layout.")

        val fields = decoded.split('|')
        if (fields.size != 3) {
            return LayoutTransferResult.Failure("SkyShards layout was not inputs, targets and a grid.")
        }

        val (inputField, targetField, grid) = fields

        if (grid.length != CELL_COUNT && grid.length != CELL_COUNT * 2) {
            return LayoutTransferResult.Failure("SkyShards grid covered ${grid.length} cells, not $CELL_COUNT.")
        }

        val lettersPerCell = grid.length / CELL_COUNT
        val notes = mutableListOf<String>()

        fun palette(field: String): List<String?> =
            if (field.isBlank()) emptyList()
            else field.split(',').map { siteIdAt(it.trim().toIntOrNull(36) ?: -1) }

        val inputs = palette(inputField)
        val targets = palette(targetField)

        val layout = PlotLayout(id = layoutId)
        val unknownCropIds = mutableSetOf<String>()

        for (cell in 0 until CELL_COUNT) {
            val row = cell / GRID_SIZE
            val column = cell % GRID_SIZE

            if (layout.plantCovering(column, row) != null) continue

            val cellLetters = grid.substring(cell * lettersPerCell, (cell + 1) * lettersPerCell)
            if (cellLetters.all { it == '.' }) continue

            val index = if (lettersPerCell == 1) {
                ALPHABET.indexOf(cellLetters[0].lowercaseChar())
            } else {
                val high = ALPHABET.indexOf(cellLetters[0].lowercaseChar())
                val low = ALPHABET.indexOf(cellLetters[1].lowercaseChar())
                if (high < 0 || low < 0) -1 else high * ALPHABET.length + low
            }

            val isTarget = cellLetters[0].isUpperCase()
            val id = (if (isTarget) targets else inputs).getOrNull(index)

            if (id == null) {
                notes.add("SkyShards named a crop at ${column},${row} that its own tables do not list")
                continue
            }

            val definition = CropRegistry.findByLooseName(id)
            if (definition == null) {
                unknownCropIds.add(id)
                continue
            }

            val footprint = definition.footprint
            if (row + footprint.height > GRID_SIZE || column + footprint.width > GRID_SIZE) {
                notes.add("$id did not fit where SkyShards put it")
                continue
            }

            val marking = if (isTarget) LayoutSlot.Marking.Target else LayoutSlot.Marking.Ingredient
            layout.placeImportedPlant(definition, column, row, marking)
        }

        if (unknownCropIds.isNotEmpty()) {
            notes.add("No crop described here for ${unknownCropIds.joinToString(", ")}")
        }

        return LayoutTransferResult.Imported(layout, notes)
    }

    override fun export(layout: PlotLayout): LayoutTransferResult {
        val notes = mutableListOf<String>()
        val unknownCropNames = mutableSetOf<String>()

        val inputCellsById = LinkedHashMap<String, MutableList<Int>>()
        val targetCellsById = LinkedHashMap<String, MutableList<Int>>()

        layout.plants.forEach { plant ->
            val definition = plant.cropDef
            val id = siteIdOf[definition]

            if (id == null) {
                unknownCropNames.add(definition.name)
                return@forEach
            }

            val isTarget = plant.slot.mark == LayoutSlot.Marking.Target

            if (isTarget && siteIndexOf(id) < BASE_CROP_IDS.size) {
                notes.add("${definition.name} is marked as a target, which SkyShards keeps for mutations")
            }

            val palette = if (isTarget && siteIndexOf(id) >= BASE_CROP_IDS.size) targetCellsById else inputCellsById
            val cells = palette.getOrPut(id) { mutableListOf() }

            for (offsetY in 0 until definition.footprint.height) {
                for (offsetX in 0 until definition.footprint.width) {
                    val x = plant.slot.x + offsetX
                    val y = plant.slot.y + offsetY

                    if (x in 0 until GRID_SIZE && y in 0 until GRID_SIZE) cells.add(y * GRID_SIZE + x)
                }
            }
        }

        val inputs = inputCellsById.keys.toList()
        val targets = targetCellsById.keys.toList()

        if (inputs.size > ALPHABET.length * ALPHABET.length || targets.size > ALPHABET.length * ALPHABET.length) {
            return LayoutTransferResult.Failure("Too many different crops for a SkyShards link.")
        }

        val isTwoLetter = inputs.size > MAX_SINGLE_LETTER_CROP_TYPES || targets.size > MAX_SINGLE_LETTER_CROP_TYPES
        val emptyCell = if (isTwoLetter) ".." else "."
        val grid = MutableList(CELL_COUNT) { emptyCell }

        fun letters(index: Int): String =
            if (isTwoLetter) "${ALPHABET[index / ALPHABET.length]}${ALPHABET[index % ALPHABET.length]}"
            else "${ALPHABET[index]}"

        inputs.forEachIndexed { index, id ->
            inputCellsById[id]?.forEach { grid[it] = letters(index) }
        }
        targets.forEachIndexed { index, id ->
            targetCellsById[id]?.forEach { grid[it] = letters(index).uppercase() }
        }

        val text = listOf(
            inputs.joinToString(",") { siteIndexOf(it).toString(36) },
            targets.joinToString(",") { siteIndexOf(it).toString(36) },
            grid.joinToString("")
        ).joinToString("|")

        if (unknownCropNames.isNotEmpty()) {
            notes.add("Left out of the link, SkyShards has no ${unknownCropNames.joinToString(", ")}")
        }

        return LayoutTransferResult.Exported(SHARE_URL + encode(text), notes)
    }
}
