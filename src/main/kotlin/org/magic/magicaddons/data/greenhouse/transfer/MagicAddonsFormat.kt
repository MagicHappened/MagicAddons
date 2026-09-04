package org.magic.magicaddons.data.greenhouse.transfer

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.MasterLayout

/**
 * This mod's own layout format: plain json, one line per plant, meant to be read and hand-corrected.
 * A plant is written once at the slot it starts from. Version 1 holds one plot under `plants`,
 * version 2 holds several under `plots`, each with its own name and plants.
 */
object MagicAddonsFormat : LayoutFormat {

    override val displayName: String = "MagicAddons"

    /** Raise when the shape below changes in a way an older reader would get wrong. */
    private const val VERSION: Int = 2

    private val GSON = GsonBuilder().setPrettyPrinting().create()

    override fun canImport(text: String): Boolean =
        runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            root.has(PLANTS) || root.has(PLOTS)
        }.getOrDefault(false)

    override fun import(text: String, layoutId: String): LayoutTransferResult {
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
                val id = if (index == 0) layoutId else "${layoutId}_p${index + 1}"
                GreenhouseLayout(id = id, name = plot.get(NAME)?.asString).also { readPlants(plants, it, notes) }
            }.take(MasterLayout.MAX_PLOTS)
            if (plots.isEmpty()) return LayoutTransferResult.Failure("That layout lists no plots.")
            if (plotsJson.size() > MasterLayout.MAX_PLOTS) notes.add("Only the first ${MasterLayout.MAX_PLOTS} plots were taken.")

            return LayoutTransferResult.Imported(plots.first(), notes, plots.drop(1))
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
            val definition = CropRegistry.get(cropName)
                ?: CropRegistry.all.find { it.name.equals(cropName, ignoreCase = true) }

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
                    definition.skyblockId?.id ?: definition.name,
                    anchor ?: return@forEach,
                    cropDef = definition
                )
            )
        }
    }

    private fun plantsOf(layout: GreenhouseLayout): JsonArray {
        val plants = JsonArray()

        layout.elementInstances
            .sortedWith(compareBy({ it.slot.y }, { it.slot.x }))
            .forEach { instance ->
                plants.add(JsonObject().apply {
                    addProperty(CROP, instance.cropDef.name)
                    addProperty(X, instance.slot.x)
                    addProperty(Y, instance.slot.y)
                    instance.slot.slotMark?.let { addProperty(ROLE, it.name) }
                })
            }
        return plants
    }

    override fun export(layout: GreenhouseLayout): LayoutTransferResult = exportOne(layout, layout.name)

    /** One plot written the old way, so older versions of the mod can still read it. */
    private fun exportOne(layout: GreenhouseLayout, name: String?): LayoutTransferResult {
        val root = JsonObject().apply {
            addProperty(VERSION_KEY, 1)
            name?.let { addProperty(NAME, it) }
            addProperty(SIZE, layout.size)
            add(PLANTS, plantsOf(layout))
        }

        return LayoutTransferResult.Exported(GSON.toJson(root))
    }

    override fun exportAll(master: MasterLayout): LayoutTransferResult {
        if (master.plots.size == 1) return exportOne(master.plots.first(), master.name)

        val plots = JsonArray()
        master.plots.forEach { plot ->
            plots.add(JsonObject().apply {
                plot.name?.let { addProperty(NAME, it) }
                add(PLANTS, plantsOf(plot))
            })
        }

        val root = JsonObject().apply {
            addProperty(VERSION_KEY, VERSION)
            master.name?.let { addProperty(NAME, it) }
            addProperty(SIZE, master.plots.first().size)
            add(PLOTS, plots)
        }

        return LayoutTransferResult.Exported(GSON.toJson(root))
    }

    private const val VERSION_KEY: String = "version"
    private const val NAME: String = "name"
    private const val SIZE: String = "size"
    private const val PLANTS: String = "plants"
    private const val PLOTS: String = "plots"
    private const val CROP: String = "crop"
    private const val X: String = "x"
    private const val Y: String = "y"
    private const val ROLE: String = "role"
}
