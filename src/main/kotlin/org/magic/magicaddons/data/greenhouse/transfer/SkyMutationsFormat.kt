package org.magic.magicaddons.data.greenhouse.transfer

import blazing.chain.LZSEncoding
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Melon
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Pumpkin
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Wheat
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.DevourerRoots
import org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops.Cropie
import org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops.Helianthus
import org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops.Squash
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout

/** skymutations.eu links: `layout=` an LZString compressed json array of `[row, column, name, marking]` */
object SkyMutationsFormat : LayoutFormat {

    override val displayName: String = "SkyMutations"

    private const val SHARE_URL: String = "https://skymutations.eu/greenhouse?layout="

    private val SITE_NAME_BY_CROP_NAME: Map<String, String> = mapOf(
        "Wheat" to "Wheat Seeds",
        "Melon" to "Melon Seeds",
        "Pumpkin" to "Pumpkin Seeds",
        "Dead Plant" to "Dead Plants"
    )

    private val CROPS_NOT_ON_SITE: Set<String> = setOf(
        "Cropie",
        "Squash",
        "Helianthus",
        "DevourerRoots"
    )

    override fun canImport(text: String): Boolean = text.contains("layout=")

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val layoutParameter = text.substringAfter("layout=", "").substringBefore("&")

        if (layoutParameter.isBlank()) {
            return LayoutTransferResult.Failure("Invalid skymutations link.")
        }

        val layoutJson = LZSEncoding.decompressFromEncodedURIComponent(layoutParameter)
            ?: return LayoutTransferResult.Failure("Failed to decode SkyMutations layout.")

        val cellEntries = runCatching { JsonParser.parseString(layoutJson).asJsonArray }.getOrNull()
            ?: return LayoutTransferResult.Failure("SkyMutations layout was not a list of plants.")

        val layout = PlotLayout(id = layoutId)
        val notes = mutableListOf<String>()

        cellEntries.forEach { element ->
            val cellEntry = runCatching { element.asJsonArray }.getOrNull() ?: return@forEach
            if (cellEntry.size() < 4) return@forEach

            val row = cellEntry[0].asInt
            val column = cellEntry[1].asInt

            if (row !in 0 until layout.size || column !in 0 until layout.size) return@forEach
            if (layout.plantCovering(column, row) != null) return@forEach

            val siteName = cellEntry[2].asString
            val cropName = SITE_NAME_BY_CROP_NAME.entries.firstOrNull { it.value == siteName }?.key ?: siteName

            val marking = LayoutSlot.Marking.entries.getOrNull(cellEntry[3].asInt)
            if (marking == null) {
                notes.add("Unknown marking on $cropName")
                return@forEach
            }

            val definition = CropRegistry.findByIdOrNameIgnoringCase(cropName)
            if (definition == null) {
                notes.add("Unknown crop: $cropName")
                return@forEach
            }

            val footprint = definition.footprint
            if (row + footprint.height > layout.size || column + footprint.width > layout.size) {
                notes.add("Malformed data for plant $cropName")
                return@forEach
            }

            layout.placeImportedPlant(definition, column, row, marking)
        }

        return LayoutTransferResult.Imported(layout, notes)
    }

    override fun export(layout: PlotLayout): LayoutTransferResult {
        val cellEntries = JsonArray()
        val unknownCropNames = mutableSetOf<String>()

        layout.plants.forEach { plant ->
            val definition = plant.cropDef

            if (definition.name in CROPS_NOT_ON_SITE) {
                unknownCropNames.add(definition.name)
                return@forEach
            }

            val siteName = SITE_NAME_BY_CROP_NAME[definition.name] ?: definition.name
            val slot = plant.slot
            val marking = slot.mark ?: LayoutSlot.Marking.Ingredient

            for (offsetY in 0 until definition.footprint.height) {
                for (offsetX in 0 until definition.footprint.width) {
                    cellEntries.add(JsonArray().apply {
                        add(slot.y + offsetY)
                        add(slot.x + offsetX)
                        add(siteName)
                        add(marking.ordinal)
                    })
                }
            }
        }

        val notes = if (unknownCropNames.isEmpty()) {
            emptyList()
        } else {
            listOf("Left out of the link, skymutations has no ${unknownCropNames.joinToString(", ")}")
        }

        return LayoutTransferResult.Exported(
            SHARE_URL + LZSEncoding.compressToEncodedURIComponent(cellEntries.toString()),
            notes
        )
    }
}
