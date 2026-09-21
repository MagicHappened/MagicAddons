package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.deadBushState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Witherbloom {

    val definition = CropDefinition(
        name = "Witherbloom",
        tier = CropTier.Common,
        dropMultiplier = 0.23,
        effects = setOf(
            CropEffect.EffectSpread
        ),
        skyblockId = SkyBlockItemId.item("WITHERBLOOM"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "ef831051cf18a4c3ea7a2a83311f218f43b032712799cc81910ab67ee7397b32" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = deadBushState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.40625, 0.0),
                        hashString = "ef831051cf18a4c3ea7a2a83311f218f43b032712799cc81910ab67ee7397b32"
                    )
                ),
                1..1
            )
        ),
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 30, requiredNeighbourCells = mapOf("Dead Plant" to 4))
    )
}