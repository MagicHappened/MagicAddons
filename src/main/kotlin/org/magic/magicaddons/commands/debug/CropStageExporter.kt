package org.magic.magicaddons.commands.debug

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft
import net.minecraft.commands.arguments.blocks.BlockStateParser
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.WorldRotation
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.PlayerUtils

object CropStageExporter {

    private const val INLINE_JSON_WIDTH: Int = 100

    fun partsJson(basePos: BlockPos, foundDefinition: CropDefinition?, knownStands: List<Entity>): JsonObject? {
        val world = Minecraft.getInstance().level ?: return null

        val width = foundDefinition?.footprint?.width ?: 1
        val height = foundDefinition?.footprint?.height ?: 1

        val blocks = JsonArray()
        for (dx in 0 until width) {
            for (dz in 0 until height) {
                var y = basePos.y + 1
                while (true) {
                    val state = world.getBlockState(BlockPos(basePos.x + dx, y, basePos.z + dz))
                    if (state.isAir) break

                    blocks.add(JsonObject().apply {
                        add("offset", jsonArrayOf(dx, y - basePos.y, dz))
                        addProperty("state", BlockStateParser.serialize(state))
                    })
                    y++
                }
            }
        }

        val box = AABB(
            basePos.x.toDouble(),
            basePos.y.toDouble() - 2,
            basePos.z.toDouble(),
            basePos.x + width.toDouble(),
            basePos.y.toDouble() + 14,
            basePos.z + height.toDouble()
        )
        val standsNearby = knownStands.ifEmpty { world.getEntities(null, box) }

        val worldStep = if (foundDefinition?.rotatesWithPlot == false) 0 else WorldRotation.quarterTurnsAt(basePos.x, basePos.z)
        val unturn = Math.floorMod(-worldStep, 4)
        val footprintCentre = Vec3(basePos.x + width / 2.0, basePos.y.toDouble(), basePos.z + height / 2.0)

        val stands = JsonArray()
        standsNearby.filterIsInstance<ArmorStand>().forEach { stand ->
            val hash = PlayerUtils.getSkullHash(stand)
            val name = if (stand.hasCustomName()) stand.name.string else null
            val held = if (hash == null) EntityUtils.heldItem(stand) else null
            if (hash == null && name == null && held == null) return@forEach

            val offset = WorldRotation.turned(stand.position().subtract(footprintCentre), unturn)
            val headPose = stand.headPose
            val yRot = Mth.wrapDegrees(stand.yRot - 90f * worldStep) + 0f

            stands.add(JsonObject().apply {
                add("offset", jsonArrayOf(offset.x + 0.0, offset.y + 0.0, offset.z + 0.0))
                addProperty("small", stand.isSmall)
                add("headPose", jsonArrayOf(headPose.x + 0f, headPose.y + 0f, headPose.z + 0f))
                if (stand.xRot != 0f) addProperty("xRot", stand.xRot)
                if (yRot != 0f) addProperty("yRot", yRot)
                hash?.let { addProperty("skullHash", it) }
                name?.let { addProperty("name", it) }
                held?.let { (slot, itemId) ->
                    add("heldItem", JsonObject().apply {
                        addProperty("id", itemId)
                        addProperty("slot", slot.name)
                    })
                }
            })
        }

        return JsonObject().apply {
            addProperty("worldStep", worldStep)
            add("blocks", blocks)
            add("stands", stands)
        }
    }

    fun jsonArrayOf(vararg values: Number): JsonArray = JsonArray().apply { values.forEach { add(it) } }

    fun prettyJson(element: JsonElement, indent: String = ""): String {
        val compact = element.toString()
        if (indent.length + compact.length <= INLINE_JSON_WIDTH || !(element.isJsonObject || element.isJsonArray)) return compact

        val inner = "$indent  "
        return if (element.isJsonArray) {
            element.asJsonArray.joinToString(",\n", "[\n", "\n$indent]") { inner + prettyJson(it, inner) }
        } else {
            element.asJsonObject.entrySet().joinToString(",\n", "{\n", "\n$indent}") { (key, value) ->
                "$inner${JsonPrimitive(key)}: ${prettyJson(value, inner)}"
            }
        }
    }
}
