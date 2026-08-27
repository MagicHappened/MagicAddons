package org.magic.magicaddons.data.greenhouse.transfer

import blazing.chain.LZSEncoding
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.LayoutSlot

/**
 * Layouts as skymutations.eu shares them: a link whose `layout` parameter is an LZString compressed
 * json array, one entry of `[row, column, name, marking]` per covered cell.
 *
 * A crop taking more than one slot is stored as its name repeated over every cell it covers, which
 * is how the site itself decides to draw a crop as one merged block, so both directions here have
 * to walk the footprint rather than the anchor alone.
 */
object SkyMutationsFormat : LayoutFormat {

    override val displayName: String = "SkyMutations"

    /** Where a shared layout lives, the encoded layout is appended to it. */
    private const val URL: String = "https://skymutations.eu/greenhouse?layout="

    /**
     * Crops this mod names differently than skymutations does, keyed by the name used here.
     * Skymutations names the three vanilla crops after the seed they are planted from.
     */
    private val NAMES: Map<String, String> = mapOf(
        "Wheat" to "Wheat Seeds",
        "Melon" to "Melon Seeds",
        "Pumpkin" to "Pumpkin Seeds",
        "Dead Plant" to "Dead Plants"
    )

    /**
     * Crops this mod knows that skymutations has no entry for at all. Helianthus is planted in the
     * greenhouse but the site only lists the condensed version, which is a different item.
     */
    private val NOT_ON_SITE: Set<String> = setOf(
        "Cropie",
        "Squash",
        "Helianthus",
        "DevourerRoots"
    )

    override fun canImport(text: String): Boolean = text.contains("layout=")

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val encoded = text.substringAfter("layout=", "").substringBefore("&")

        if (encoded.isBlank()) {
            return LayoutTransferResult.Failure("Invalid skymutations link.")
        }

        val decoded = LZSEncoding.decompressFromEncodedURIComponent(encoded)
            ?: return LayoutTransferResult.Failure("Failed to decode SkyMutations layout.")

        val entries = runCatching { JsonParser.parseString(decoded).asJsonArray }.getOrNull()
            ?: return LayoutTransferResult.Failure("SkyMutations layout was not a list of plants.")

        val layout = GreenhouseLayout(id = layoutId)
        val occupied = Array(layout.size) { BooleanArray(layout.size) }
        val notes = mutableListOf<String>()

        entries.forEach { element ->
            val entry = runCatching { element.asJsonArray }.getOrNull() ?: return@forEach
            if (entry.size() < 4) return@forEach

            val row = entry[0].asInt
            val column = entry[1].asInt

            if (row !in 0 until layout.size || column !in 0 until layout.size) return@forEach
            if (occupied[row][column]) return@forEach

            // a SEED is not a CROP skymutations smh
            val siteName = entry[2].asString
            val cropName = NAMES.entries.firstOrNull { it.value == siteName }?.key ?: siteName

            val marking = LayoutSlot.Marking.entries.getOrNull(entry[3].asInt)
            if (marking == null) {
                notes.add("Unknown marking on $cropName")
                return@forEach
            }

            val definition = CropRegistry.all.find { it.name.equals(cropName, ignoreCase = true) }
            if (definition == null) {
                notes.add("Unknown crop: $cropName")
                return@forEach
            }

            val footprint = definition.footprint

            // a crop hanging off the edge is one broken entry, not one broken entry per cell, so the
            // whole crop is dropped on the first cell that would miss
            if (row + footprint.height > layout.size || column + footprint.width > layout.size) {
                notes.add("Malformed data for plant $cropName")
                return@forEach
            }

            var topLeftSlot: LayoutSlot? = null

            for (offsetX in 0 until footprint.width) {
                for (offsetY in 0 until footprint.height) {
                    occupied[row + offsetY][column + offsetX] = true

                    val slot = layout.getSlot(column + offsetX, row + offsetY)
                    slot?.placedBlock = definition.requiredSoil.firstOrNull()?.defaultBlockState()
                    slot?.slotMark = marking

                    if (offsetX == 0 && offsetY == 0) topLeftSlot = slot
                }
            }

            val anchor = topLeftSlot ?: return@forEach

            layout.elementInstances.add(
                GreenhouseElementInstance(
                    definition.skyblockId?.id ?: definition.name,
                    anchor,
                    null,
                    null,
                    cropDef = definition
                )
            )
        }

        return LayoutTransferResult.Imported(layout, notes)
    }

    override fun export(layout: GreenhouseLayout): LayoutTransferResult {
        val entries = JsonArray()
        val unsupported = mutableSetOf<String>()

        layout.elementInstances.forEach { instance ->
            val definition = instance.cropDef

            // the site drops names it does not know, so a crop it never lists is left out of the
            // link instead of turning into a silently missing plant
            if (definition.name in NOT_ON_SITE) {
                unsupported.add(definition.name)
                return@forEach
            }

            val exportName = NAMES[definition.name] ?: definition.name
            val slot = instance.slot
            val marking = slot.slotMark ?: LayoutSlot.Marking.Ingredient

            for (offsetY in 0 until definition.footprint.height) {
                for (offsetX in 0 until definition.footprint.width) {
                    entries.add(JsonArray().apply {
                        add(slot.y + offsetY)
                        add(slot.x + offsetX)
                        add(exportName)
                        add(marking.ordinal)
                    })
                }
            }
        }

        val notes = if (unsupported.isEmpty()) {
            emptyList()
        } else {
            listOf("Left out of the link, skymutations has no ${unsupported.joinToString(", ")}")
        }

        return LayoutTransferResult.Exported(
            URL + LZSEncoding.compressToEncodedURIComponent(entries.toString()),
            notes
        )
    }
}
