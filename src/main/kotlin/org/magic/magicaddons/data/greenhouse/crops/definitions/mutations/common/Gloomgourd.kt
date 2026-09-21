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
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Melon
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Pumpkin
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Gloomgourd {
    val definition = CropDefinition(
        name = "Gloomgourd",
        tier = CropTier.Common,
        dropMultiplier = 0.2,
        effects = setOf(
            CropEffect.WaterRetain,
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("GLOOMGOURD"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(6)
                    )
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0, 0.78125, 0.0),
                        Vec3(0.0, -0.40625, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, -180.0f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    hashString = "7f693e42ba3b763292e7de26fd2b0a08fcee3bec2e017075dc66dfc4a932aa64",
                    isSmall = false
                ),
                1..1
            )
        ),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 30, requiredNeighbourCells = mapOf("Pumpkin" to 1, "Melon" to 1))
    )
}