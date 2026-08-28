package org.magic.magicaddons.data.greenhouse.transfer

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Layouts as greenhouse.skyshards.com shares them.
 *
 * The share string is two things stacked: a plain text description of the design, raw-deflated and
 * then written in url-safe base64 without padding. Decompressed it reads as three fields separated
 * by pipes, `inputs|targets|grid`.
 *
 * The two field lists are palettes, each a comma separated list of base36 numbers indexing one
 * table of crops: the base crops first, then the mutations offset past them, which is why a
 * mutation's number is always at least seventeen. Inputs may be either kind; targets are always
 * mutations, since a target is the thing being grown towards.
 *
 * The grid is the ten by ten read row by row, a letter per cell, lower case indexing the inputs
 * palette and upper case the targets, with a dot for an empty cell. A palette of more than
 * twenty six crops switches the whole grid to two letters a cell, `aa` through `zz`, so a cell is
 * measured rather than assumed. A crop wider than one cell is written into every cell it covers,
 * the same repetition skymutations uses, so both directions here walk the footprint.
 */
object SkyShardsFormat : LayoutFormat {

    override val displayName: String = "SkyShards"

    private const val URL: String = "https://greenhouse.skyshards.com/designer?layout="

    private const val GRID: Int = 10
    private const val CELLS: Int = GRID * GRID

    private const val ALPHABET: String = "abcdefghijklmnopqrstuvwxyz"

    /** Past this many crops in a palette the site writes two letters a cell instead of one. */
    private const val SINGLE_LETTER_MAX: Int = 26

    /** The base crops, indexed from zero. */
    private val BASE: List<String> = listOf(
        "wheat", "potato", "carrot", "pumpkin", "melon", "cocoa_beans", "sugar_cane", "cactus",
        "nether_wart", "red_mushroom", "brown_mushroom", "moonflower", "sunflower", "wild_rose",
        "fire", "dead_plant", "fermento"
    )

    /** The mutations, indexed from where the base crops leave off. */
    private val MUTATIONS: List<String> = listOf(
        "ashwreath", "choconut", "dustgrain", "gloomgourd", "lonelily", "scourroot", "shadevine",
        "veilshroom", "witherbloom", "chocoberry", "cindershade", "coalroot", "creambloom",
        "duskbloom", "thornshade", "blastberry", "cheesebite", "chloronite", "do_not_eat_shroom",
        "fleshtrap", "magic_jellybean", "noctilume", "snoozling", "soggybud", "chorus_fruit",
        "plantboy_advance", "puffercloud", "shellfruit", "startlevine", "stoplight_petal",
        "thunderling", "turtlellini", "zombud", "all_in_aloe", "devourer", "glasscorn", "godseed",
        "jerryflower", "phantomleaf", "timestalk"
    )

    /**
     * Both sides written the same way, so a name can be compared without a table of exceptions.
     * The site says `do_not_eat_shroom` where this mod says `Do-not-eat-shroom`, and once the
     * punctuation and case are gone they are the same word.
     */
    private fun key(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    private val byKey: Map<String, CropDefinition> by lazy {
        buildMap {
            CropRegistry.all.forEach { def ->
                putIfAbsent(key(def.name), def)
                def.skyblockId?.id?.substringAfter(':')?.let { putIfAbsent(key(it), def) }
            }
        }
    }

    private val idOf: Map<CropDefinition, String> by lazy {
        buildMap {
            (BASE + MUTATIONS).forEach { id -> byKey[key(id)]?.let { putIfAbsent(it, id) } }
        }
    }

    private fun definitionFor(id: String): CropDefinition? = byKey[key(id)]

    private fun cropAt(index: Int): String? = when {
        index < 0 -> null
        index < BASE.size -> BASE[index]
        else -> MUTATIONS.getOrNull(index - BASE.size)
    }

    private fun indexOf(id: String): Int {
        val base = BASE.indexOf(id)
        if (base >= 0) return base

        val mutation = MUTATIONS.indexOf(id)
        return if (mutation >= 0) BASE.size + mutation else -1
    }

    // ------------------------------------------------------------------ the wrapping

    private fun decode(share: String): String? = runCatching {
        val bytes = Base64.getUrlDecoder().decode(share.trim().padded())

        val inflater = Inflater(true)
        inflater.setInput(bytes)

        val out = ByteArrayOutputStream(bytes.size * 4)
        val buffer = ByteArray(4096)

        while (!inflater.finished()) {
            val read = inflater.inflate(buffer)
            if (read == 0 && inflater.needsInput()) break
            out.write(buffer, 0, read)
        }
        inflater.end()

        out.toString(Charsets.UTF_8)
    }.getOrNull()

    /** The site strips base64 padding; the decoder wants it back. */
    private fun String.padded(): String = this + "=".repeat((4 - length % 4) % 4)

    private fun encode(text: String): String {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(text.toByteArray(Charsets.UTF_8))
        deflater.finish()

        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)

        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()

        return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    /** The share string out of whatever the player copied: either link shape, or the string bare. */
    private fun payloadOf(text: String): String = text.trim().let {
        when {
            "layout=" in it -> it.substringAfter("layout=").substringBefore('&')
            "/share/" in it -> it.substringAfter("/share/").substringBefore('?')
            else -> it
        }
    }

    // ------------------------------------------------------------------ reading

    override fun canImport(text: String): Boolean {
        val decoded = decode(payloadOf(text)) ?: return false
        return decoded.count { it == '|' } == 2
    }

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val decoded = decode(payloadOf(text))
            ?: return LayoutTransferResult.Failure("Failed to decode SkyShards layout.")

        val fields = decoded.split('|')
        if (fields.size != 3) {
            return LayoutTransferResult.Failure("SkyShards layout was not inputs, targets and a grid.")
        }

        val (inputField, targetField, grid) = fields

        if (grid.length != CELLS && grid.length != CELLS * 2) {
            return LayoutTransferResult.Failure("SkyShards grid covered ${grid.length} cells, not $CELLS.")
        }

        val width = grid.length / CELLS
        val notes = mutableListOf<String>()

        fun palette(field: String): List<String?> =
            if (field.isBlank()) emptyList()
            else field.split(',').map { cropAt(it.trim().toIntOrNull(36) ?: -1) }

        val inputs = palette(inputField)
        val targets = palette(targetField)

        val layout = GreenhouseLayout(id = layoutId)
        val claimed = Array(GRID) { BooleanArray(GRID) }
        val unknown = mutableSetOf<String>()

        for (cell in 0 until CELLS) {
            val row = cell / GRID
            val column = cell % GRID

            if (claimed[row][column]) continue

            val token = grid.substring(cell * width, (cell + 1) * width)
            if (token.all { it == '.' }) continue

            val index = if (width == 1) {
                ALPHABET.indexOf(token[0].lowercaseChar())
            } else {
                val high = ALPHABET.indexOf(token[0].lowercaseChar())
                val low = ALPHABET.indexOf(token[1].lowercaseChar())
                if (high < 0 || low < 0) -1 else high * ALPHABET.length + low
            }

            val isTarget = token[0].isUpperCase()
            val id = (if (isTarget) targets else inputs).getOrNull(index)

            if (id == null) {
                notes.add("SkyShards named a crop at ${column},${row} that its own tables do not list")
                continue
            }

            val definition = definitionFor(id)
            if (definition == null) {
                unknown.add(id)
                continue
            }

            val footprint = definition.footprint

            // a crop hanging off the edge is one broken plant rather than one per cell it covers
            if (row + footprint.height > GRID || column + footprint.width > GRID) {
                notes.add("$id did not fit where SkyShards put it")
                continue
            }

            val marking = if (isTarget) LayoutSlot.Marking.Target else LayoutSlot.Marking.Ingredient
            var anchor: LayoutSlot? = null

            for (offsetX in 0 until footprint.width) {
                for (offsetY in 0 until footprint.height) {
                    claimed[row + offsetY][column + offsetX] = true

                    val slot = layout.getSlot(column + offsetX, row + offsetY)
                    slot?.placedBlock = definition.requiredSoil.firstOrNull()?.defaultBlockState()
                    slot?.slotMark = marking

                    if (offsetX == 0 && offsetY == 0) anchor = slot
                }
            }

            layout.elementInstances.add(
                GreenhouseElementInstance(
                    definition.skyblockId?.id ?: definition.name,
                    anchor ?: continue,
                    null,
                    null,
                    cropDef = definition
                )
            )
        }

        if (unknown.isNotEmpty()) {
            notes.add("No crop described here for ${unknown.joinToString(", ")}")
        }

        return LayoutTransferResult.Imported(layout, notes)
    }

    // ------------------------------------------------------------------ writing

    override fun export(layout: GreenhouseLayout): LayoutTransferResult {
        val notes = mutableListOf<String>()
        val unsupported = mutableSetOf<String>()

        // the palettes in the order they are first met, since a cell names a crop by where it sits
        // in its own palette rather than by the crop's number
        val inputOrder = LinkedHashMap<String, MutableList<Int>>()
        val targetOrder = LinkedHashMap<String, MutableList<Int>>()

        layout.elementInstances.forEach { instance ->
            val definition = instance.cropDef
            val id = idOf[definition]

            if (id == null) {
                unsupported.add(definition.name)
                return@forEach
            }

            val isTarget = instance.slot.slotMark == LayoutSlot.Marking.Target

            // a target is the plant being grown towards, which is always a mutation. A base crop
            // marked as one has nowhere to go in the share, so it travels as an input instead
            if (isTarget && indexOf(id) < BASE.size) {
                notes.add("${definition.name} is marked as a target, which SkyShards keeps for mutations")
            }

            val into = if (isTarget && indexOf(id) >= BASE.size) targetOrder else inputOrder
            val cells = into.getOrPut(id) { mutableListOf() }

            for (offsetY in 0 until definition.footprint.height) {
                for (offsetX in 0 until definition.footprint.width) {
                    val x = instance.slot.x + offsetX
                    val y = instance.slot.y + offsetY

                    if (x in 0 until GRID && y in 0 until GRID) cells.add(y * GRID + x)
                }
            }
        }

        val inputs = inputOrder.keys.toList()
        val targets = targetOrder.keys.toList()

        if (inputs.size > ALPHABET.length * ALPHABET.length || targets.size > ALPHABET.length * ALPHABET.length) {
            return LayoutTransferResult.Failure("Too many different crops for a SkyShards link.")
        }

        val twoLetter = inputs.size > SINGLE_LETTER_MAX || targets.size > SINGLE_LETTER_MAX
        val empty = if (twoLetter) ".." else "."
        val grid = MutableList(CELLS) { empty }

        fun letters(index: Int): String =
            if (twoLetter) "${ALPHABET[index / ALPHABET.length]}${ALPHABET[index % ALPHABET.length]}"
            else "${ALPHABET[index]}"

        inputs.forEachIndexed { index, id ->
            inputOrder[id]?.forEach { grid[it] = letters(index) }
        }
        targets.forEachIndexed { index, id ->
            targetOrder[id]?.forEach { grid[it] = letters(index).uppercase() }
        }

        val text = listOf(
            inputs.joinToString(",") { indexOf(it).toString(36) },
            targets.joinToString(",") { indexOf(it).toString(36) },
            grid.joinToString("")
        ).joinToString("|")

        if (unsupported.isNotEmpty()) {
            notes.add("Left out of the link, SkyShards has no ${unsupported.joinToString(", ")}")
        }

        return LayoutTransferResult.Exported(URL + encode(text), notes)
    }
}
