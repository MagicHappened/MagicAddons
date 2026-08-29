package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.StandPose
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Shadevine : CropDefinitionProvider {

    override val definition = CropDefinition(
        name = "Shadevine",
        effects = setOf(
            CropEffect.ImprovedWaterRetain,
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("SHADEVINE"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "c3c6d9dcb8fbd73de6171a2c2155314d097a9c99d09c9fce9cba068d7e5aedf7" to StandPose.Fixed(Rotations(-45.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.75, 0.1875),
                        hashString = "c3c6d9dcb8fbd73de6171a2c2155314d097a9c99d09c9fce9cba068d7e5aedf7"
                    )
                ),
                1..1,
            )

        ),
        requiredSoil = setOf(Blocks.FARMLAND, Blocks.SAND),
        needsWater = false,
        isMutation = true
    )
}