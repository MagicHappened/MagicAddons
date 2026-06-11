package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.CropStates.brownMushroomState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Brownmushroom : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Brown Mushroom",
        skyblockId = SkyBlockItemId.item("BROWN_MUSHROOM"),
        stageDefs = listOf(
            CropStagePattern(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = brownMushroomState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    )
                ),
                stageRange = 1..5,
                baseStageStandOffset = Vec3(0.0, 0.06, 0.0)
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = brownMushroomState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.45000000000000284, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "578897b83f51fb96b59ba418ff0868cef7bdf661e315ba5dbac51d876d1d15d"
                    )
                ),
                6..6
            )

        ),
        maxStage = 6,
        decayTimeMs = -1,
        requiredSoil = setOf(Blocks.MYCELIUM),
        needsWater = false,
        isBaseCrop = true
    )
}