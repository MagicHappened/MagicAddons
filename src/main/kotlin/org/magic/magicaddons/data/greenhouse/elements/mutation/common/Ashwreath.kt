package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.StandPose
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Ashwreath : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Ashwreath",
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.XpLoss
        ),
        skyblockId = SkyBlockItemId.item("ASHWREATH"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "5890f50780fdecedaa85aa40bf3399e9439ee68594c6d022688165608171681d" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.375, 0.0),
                        hashString = "5890f50780fdecedaa85aa40bf3399e9439ee68594c6d022688165608171681d",
                        isSmall = false
                    )
                ),
                1..1
            )

        ),
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )

}