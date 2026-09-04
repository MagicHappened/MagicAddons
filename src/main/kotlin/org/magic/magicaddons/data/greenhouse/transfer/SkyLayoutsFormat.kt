package org.magic.magicaddons.data.greenhouse.transfer

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import java.math.BigInteger

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

    private const val MAX_PLOTS: Int = 3

    private fun key(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    private val byKey: Map<String, CropDefinition> by lazy {
        buildMap {
            CropRegistry.all.forEach { def ->
                putIfAbsent(key(def.name), def)
                def.skyblockId?.id?.substringAfter(':')?.let { putIfAbsent(key(it), def) }
            }
        }
    }

    private val kindOf: Map<CropDefinition, Int> by lazy {
        buildMap { KINDS.forEachIndexed { index, id -> byKey[key(id)]?.let { putIfAbsent(it, index) } } }
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

        val layouts = boards.take(MAX_PLOTS).mapIndexed { number, board ->
            val id = if (number == 0) layoutId else "${layoutId}_part${number + 1}"
            readBoard(board, id, notes) ?: return LayoutTransferResult.Failure("Could not read plot ${number + 1} of the SkyLayouts link.")
        }
        if (boards.size > MAX_PLOTS) notes.add("Only the first $MAX_PLOTS plots were taken.")

        val layout = layouts.first()
        layout.parts.addAll(layouts.drop(1))
        if (layouts.size > 1) notes.add("Imported ${layouts.size} plots as one master layout.")

        return LayoutTransferResult.Imported(layout, notes)
    }

    /** One plot's cells into a layout; a plant covering several cells is one plant here. */
    private fun readBoard(board: String, id: String, notes: MutableList<String>): GreenhouseLayout? {
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

        val layout = GreenhouseLayout(id = id)
        val taken = Array(layout.size) { BooleanArray(layout.size) }
        val unknown = mutableSetOf<String>()

        for (y in 0 until minOf(size, layout.size)) {
            for (x in 0 until minOf(size, layout.size)) {
                val kind = cells[y * size + x]
                if (kind < 0 || kind >= kinds.size || taken[x][y]) continue

                val def = byKey[key(kinds[kind])]
                if (def == null) {
                    unknown.add(kinds[kind])
                    continue
                }
                val slot = layout.getSlot(x, y) ?: continue

                layout.elementInstances.add(GreenhouseElementInstance(def.skyblockId?.id ?: def.name, slot, null, null, cropDef = def))
                val soil = def.requiredSoil.firstOrNull()?.defaultBlockState()
                for (dx in 0 until def.footprint.width) {
                    for (dy in 0 until def.footprint.height) {
                        if (x + dx >= layout.size || y + dy >= layout.size) continue
                        taken[x + dx][y + dy] = true
                        soil?.let { layout.getSlot(x + dx, y + dy)?.placedBlock = it }
                    }
                }
            }
        }
        unknown.forEach { notes.add("Unknown crop: $it") }
        return layout
    }

    override fun export(layout: GreenhouseLayout): LayoutTransferResult {
        val notes = mutableListOf<String>()

        // the site files a layout under the mutation it grows: the plant marked as the target
        val target = layout.elementInstances
            .firstOrNull { it.slot.slotMark == LayoutSlot.Marking.Target && it.cropDef.isMutation }
            ?.let { kindOf[it.cropDef] }
        val head = "1" + letter(target?.plus(1) ?: 0) + letter(VISIT_INTERVAL)

        val boards = (listOf(layout) + layout.parts).take(MAX_PLOTS).map { writeBoard(it, notes) }
        if (layout.parts.size + 1 > MAX_PLOTS) notes.add("Only the first $MAX_PLOTS plots were written.")

        return LayoutTransferResult.Exported(URL + head + "~p" + boards.joinToString("~"), notes)
    }

    private fun writeBoard(layout: GreenhouseLayout, notes: MutableList<String>): String {
        val kinds = mutableListOf<Int>()
        val cells = IntArray(layout.size * layout.size) { -1 }

        layout.elementInstances.forEach { instance ->
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
