package org.magic.magicaddons.data.greenhouse.transfer

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.LayoutSlot

/**
 * This mod's own layout format: plain json, one line per plant, meant to be read.
 *
 * Nothing carrying a layout of ours has a length limit worth compressing for, and the things worth
 * having from a format of our own are exactly what compressing takes away: a layout somebody pastes
 * in broken can be read, corrected and pasted back by hand.
 *
 * A plant is written once at the slot it starts from rather than once per slot it covers, since the
 * crop already knows how much room it takes. [VERSION] is carried so a later shape can be told
 * apart from this one.
 */
object MagicAddonsFormat : LayoutFormat {

    override val displayName: String = "MagicAddons"

    /** Raise when the shape below changes in a way an older reader would get wrong. */
    private const val VERSION: Int = 1

    private val GSON = GsonBuilder().setPrettyPrinting().create()

    override fun canImport(text: String): Boolean =
        runCatching {
            JsonParser.parseString(text).asJsonObject.has(PLANTS)
        }.getOrDefault(false)

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val root = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            ?: return LayoutTransferResult.Failure("That is not a MagicAddons layout.")

        val version = root.get(VERSION_KEY)?.asInt ?: VERSION
        if (version > VERSION) {
            return LayoutTransferResult.Failure(
                "That layout was written by a newer version of the mod."
            )
        }

        val plants = runCatching { root.getAsJsonArray(PLANTS) }.getOrNull()
            ?: return LayoutTransferResult.Failure("That layout lists no plants.")

        val layout = GreenhouseLayout(
            id = layoutId,
            name = root.get(NAME)?.asString
        )

        val notes = mutableListOf<String>()

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

        return LayoutTransferResult.Imported(layout, notes)
    }

    override fun export(layout: GreenhouseLayout): LayoutTransferResult {
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

        val root = JsonObject().apply {
            addProperty(VERSION_KEY, VERSION)
            layout.name?.let { addProperty(NAME, it) }
            addProperty(SIZE, layout.size)
            add(PLANTS, plants)
        }

        return LayoutTransferResult.Exported(GSON.toJson(root))
    }

    private const val VERSION_KEY: String = "version"
    private const val NAME: String = "name"
    private const val SIZE: String = "size"
    private const val PLANTS: String = "plants"
    private const val CROP: String = "crop"
    private const val X: String = "x"
    private const val Y: String = "y"
    private const val ROLE: String = "role"
}
