package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.pumpkinStemState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Cocoa
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Choconut {
    val definition = CropDefinition(
        name = "Choconut",
        tier = CropTier.Common,
        dropMultiplier = 0.25,
        effects = setOf(
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("CHOCONUT"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "2a8d74b77a0e510d058c544c7292a8844e70b9293880caffc562ce5ab5a49ad8" to StandPose.Fixed(Rotations(45.0f, 0.0f, 45.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = pumpkinStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(-0.15625, 0.28125, -0.15625),
                        hashString = "2a8d74b77a0e510d058c544c7292a8844e70b9293880caffc562ce5ab5a49ad8"
                    )
                ),
                1..1
            )
        ),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 30, requiredNeighbourCells = mapOf("Cocoa Beans" to 2))
    )
}