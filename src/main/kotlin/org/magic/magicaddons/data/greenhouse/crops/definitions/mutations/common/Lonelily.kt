package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Lonelily {

    val definition = CropDefinition(
        name = "Lonelily",
        tier = CropTier.Common,
        dropMultiplier = 1.75,
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("LONELILY"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "f4672d5fb128c63197da27f40d7143818d318665b3ab2335121e9ae8f9ecc2e3" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.125, 0.0),
                        hashString = "f4672d5fb128c63197da27f40d7143818d318665b3ab2335121e9ae8f9ecc2e3"
                    )
                ),
                1..1
            )
        ),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 6, needsNoNeighbours = true)
    )
}