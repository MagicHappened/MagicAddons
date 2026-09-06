package org.magic.magicaddons.data.greenhouse.transfer

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.MasterLayout

/**
 * Reads the json this mod shared layouts as before the share code: one line per plant, written
 * once at the slot it starts from. Version 1 holds one plot under `plants`, version 2 several
 * under `plots`, each with its own name and plants. Nothing writes this shape any more.
 */
object MagicAddonsFormat {

    val displayName: String = "MagicAddons (json)"

    /** The newest shape this reader understands. */
    private const val VERSION: Int = 2

    fun canImport(text: String): Boolean =
        runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            root.has(PLANTS) || root.has(PLOTS)
        }.getOrDefault(false)

    fun import(text: String, layoutId: String): LayoutTransferResult {
        val root = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            ?: return LayoutTransferResult.Failure("That is not a MagicAddons layout.")

        val version = root.get(VERSION_KEY)?.asInt ?: 1
        if (version > VERSION) {
            return LayoutTransferResult.Failure(
                "That layout was written by a newer version of the mod."
            )
        }

        val notes = mutableListOf<String>()
        val presetName = root.get(NAME)?.asString

        val plotsJson = runCatching { root.getAsJsonArray(PLOTS) }.getOrNull()
        if (plotsJson != null) {
            val plots = plotsJson.mapIndexedNotNull { index, element ->
                val plot = runCatching { element.asJsonObject }.getOrNull() ?: return@mapIndexedNotNull null
                val plants = runCatching { plot.getAsJsonArray(PLANTS) }.getOrNull() ?: JsonArray()
                val id = MasterLayout.plotId(layoutId, index)
                GreenhouseLayout(id = id, name = plot.get(NAME)?.asString).also { readPlants(plants, it, notes) }
            }.take(MasterLayout.MAX_PLOTS)
            if (plots.isEmpty()) return LayoutTransferResult.Failure("That layout lists no plots.")
            if (plotsJson.size() > MasterLayout.MAX_PLOTS) notes.add("Only the first ${MasterLayout.MAX_PLOTS} plots were taken.")

            return LayoutTransferResult.Imported(plots.first(), notes, plots.drop(1), presetName)
        }

        val plants = runCatching { root.getAsJsonArray(PLANTS) }.getOrNull()
            ?: return LayoutTransferResult.Failure("That layout lists no plants.")

        val layout = GreenhouseLayout(id = layoutId, name = presetName)
        readPlants(plants, layout, notes)
        return LayoutTransferResult.Imported(layout, notes)
    }

    /** Puts the plants of one json list onto [layout], noting whatever could not be placed. */
    private fun readPlants(plants: JsonArray, layout: GreenhouseLayout, notes: MutableList<String>) {
        plants.forEach { element ->
            val plant = runCatching { element.asJsonObject }.getOrNull() ?: return@forEach

            val cropName = plant.get(CROP)?.asString ?: return@forEach
            val definition = CropRegistry.findByName(cropName)

            if (definition == null) {
                notes.add("Unknown crop: $cropName")
                return@forEach
            }

            val x = plant.get(X)?.asInt ?: return@forEach
            val y = plant.get(Y)?.asInt ?: return@forEach

            val footprint = definition.footprint
            if (x + footprint.width > layout.size || y + footprint.height > layout.size) {
                notes.add("$cropName at $x,$y does not fit the grid")
                return@forEach
            }

            val marking = plant.get(ROLE)?.asString?.let { role ->
                LayoutSlot.Marking.entries.find { it.name.equals(role, ignoreCase = true) }
                    ?: run {
                        notes.add("Unknown role on $cropName: $role")
                        null
                    }
            }

            var anchor: LayoutSlot? = null

            for (offsetX in 0 until footprint.width) {
                for (offsetY in 0 until footprint.height) {
                    val slot = layout.getSlot(x + offsetX, y + offsetY)
                    slot?.placedBlock = definition.requiredSoil.firstOrNull()?.defaultBlockState()
                    slot?.slotMark = marking

                    if (offsetX == 0 && offsetY == 0) anchor = slot
                }
            }

            layout.elementInstances.add(
                GreenhouseElementInstance(
                    definition.elementId,
                    anchor ?: return@forEach,
                    cropDef = definition
                )
            )
        }
    }

    private const val VERSION_KEY: String = "version"
    private const val NAME: String = "name"
    private const val PLANTS: String = "plants"
    private const val PLOTS: String = "plots"
    private const val CROP: String = "crop"
    private const val X: String = "x"
    private const val Y: String = "y"
    private const val ROLE: String = "role"
}
