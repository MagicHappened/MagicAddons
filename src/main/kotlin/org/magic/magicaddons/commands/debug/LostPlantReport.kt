package org.magic.magicaddons.commands.debug

import kotlin.math.abs
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.Footprint
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.WorldRotation
import org.magic.magicaddons.data.greenhouse.plot.CROP_HEIGHT
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils

/** why a scan stopped matching a plant the records already had */
object LostPlantReport {

    fun sendReport(previous: Plant, origin: BlockPos, remainingStands: List<ArmorStand>) {
        val crop = previous.cropDef
        val level = Minecraft.getInstance().level ?: return
        val footprint = crop.footprint
        val standsInWorld = level.getEntitiesOfClass(ArmorStand::class.java, footprint.spaceAbove(origin.below(), CROP_HEIGHT + 1))
            .filterNot { it.isMarker }
        val standsInPool = standsInWorld.filter { it in remainingStands }

        ChatUtils.sendWithPrefix(
            "${crop.name} at ${previous.slot.x},${previous.slot.y} stage ${previous.lowestStage ?: "?"} was not matched"
        )
        ChatUtils.send(
            "  soil ${previous.slot.soil?.descriptionId ?: "none"}," +
                    " stands around it ${standsInWorld.size} (${standsInPool.size} still free)"
        )

        val stages = crop.stages.filter { stage -> previous.lowestStage?.let { it in stage.stageRange } ?: true }
        (stages.ifEmpty { crop.stages }).take(3).forEach { stage ->
            ChatUtils.send("  stage ${stage.stageRange.first}: ${failureOf(stage, origin, crop.rotatesWithPlot, footprint, standsInPool, standsInWorld)}")
        }
    }

    private fun failureOf(
        stage: CropStage,
        origin: BlockPos,
        rotatesWithPlot: Boolean,
        footprint: Footprint,
        standsInPool: List<ArmorStand>,
        standsInWorld: List<ArmorStand>
    ): String {
        val level = Minecraft.getInstance().level ?: return "no world"

        stage.blocks?.filter { it.required }?.forEach { recordedBlock ->
            val worldBlock = level.getBlockState(origin.offset(recordedBlock.offset))
            if (worldBlock != recordedBlock.blockState) {
                return "block at ${recordedBlock.offset.x},${recordedBlock.offset.y},${recordedBlock.offset.z}" +
                        " is ${worldBlock.block.descriptionId}, recorded ${recordedBlock.blockState.block.descriptionId}"
            }
        }

        val footprintCenter = Vec3(origin.x + footprint.width / 2.0, origin.y.toDouble(), origin.z + footprint.height / 2.0)
        val quarterTurns = if (rotatesWithPlot) WorldRotation.quarterTurnsAt(origin.x, origin.z) else 0

        stage.armorStands.orEmpty().forEach { recordedStand ->
            val expectedOffset = WorldRotation.turned(recordedStand.offset, quarterTurns)
            if (standsInPool.any { matches(it, recordedStand, footprintCenter, expectedOffset) }) return@forEach

            val takenElsewhere = standsInWorld.any { matches(it, recordedStand, footprintCenter, expectedOffset) }
            if (takenElsewhere) return "stand ${shortHash(recordedStand.hashString)} matches, but another plant took it"

            val nearest = standsInWorld
                .filter { recordedStand.hashString == null || PlayerUtils.getSkullHash(it) == recordedStand.hashString }
                .minByOrNull { furthestAxis(it.position().subtract(footprintCenter), expectedOffset) }
                ?: return "no stand with skull ${shortHash(recordedStand.hashString)} anywhere around it"

            val actualOffset = nearest.position().subtract(footprintCenter)
            return "stand ${shortHash(recordedStand.hashString)} off by ${"%.4f".format(furthestAxis(actualOffset, expectedOffset))}" +
                    " (tolerance $OFFSET_TOLERANCE): recorded ${format(expectedOffset)}, standing at ${format(actualOffset)}"
        }

        return "matches now"
    }

    private fun matches(stand: ArmorStand, recordedStand: StageStand, footprintCenter: Vec3, expectedOffset: Vec3): Boolean =
        furthestAxis(stand.position().subtract(footprintCenter), expectedOffset) < OFFSET_TOLERANCE &&
                (recordedStand.hashString?.let { it == PlayerUtils.getSkullHash(stand) } ?: true)

    private fun furthestAxis(actual: Vec3, expected: Vec3): Double =
        maxOf(abs(actual.x - expected.x), abs(actual.y - expected.y), abs(actual.z - expected.z))

    private const val OFFSET_TOLERANCE: Double = 0.015

    private fun format(offset: Vec3): String = "(%.4f, %.4f, %.4f)".format(offset.x, offset.y, offset.z)

    private fun shortHash(hash: String?): String = hash?.take(8) ?: "any"
}
