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
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Wheat
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Dustgrain {
    val definition = CropDefinition(
        name = "Dustgrain",
        tier = CropTier.Common,
        dropMultiplier = 0.25,
        effects = setOf(
            CropEffect.HarvestBoost
        ),
        skyblockId = SkyBlockItemId.item("DUSTGRAIN"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "8698331f183a586ae7258d6b3c83ccd3620bb2411d803123bd6706444c1efdf3" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.1875, 0.0),
                        hashString = "8698331f183a586ae7258d6b3c83ccd3620bb2411d803123bd6706444c1efdf3"
                    )
                ),
                1..1
            )
        ),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 30, requiredNeighbourCells = mapOf("Wheat" to 2))
    )
}