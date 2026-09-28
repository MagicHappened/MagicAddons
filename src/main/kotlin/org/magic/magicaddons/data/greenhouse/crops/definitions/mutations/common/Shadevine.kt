package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common

import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Cactus
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Shadevine {

    val definition = CropDefinition(
        name = "Shadevine",
        tier = CropTier.Common,
        dropMultiplier = 0.26,
        effects = setOf(
            CropEffect.ImprovedWaterRetain,
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("SHADEVINE"),
        standPoses = mapOf(
            "c3c6d9dcb8fbd73de6171a2c2155314d097a9c99d09c9fce9cba068d7e5aedf7" to StandPose.Fixed(Rotations(-45.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.75, 0.1875),
                        hashString = "c3c6d9dcb8fbd73de6171a2c2155314d097a9c99d09c9fce9cba068d7e5aedf7"
                    )
                ),
                1..1
            )
        ),
        requiredSoil = setOf(Blocks.FARMLAND, Blocks.SAND, Blocks.RED_SAND),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 30, requiredNeighbourCells = mapOf("Cactus" to 1, "Sugar Cane" to 1))
    )
}