package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.brownMushroomState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Brownmushroom {
    val definition = CropDefinition(
        name = "Brown Mushroom",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.08,
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("BROWN_MUSHROOM"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "578897b83f51fb96b59ba418ff0868cef7bdf661e315ba5dbac51d876d1d15d" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = brownMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = brownMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.69, 0.0),
                        hashString = "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = brownMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.63, 0.0),
                        hashString = "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = brownMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.57, 0.0),
                        hashString = "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = brownMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.51, 0.0),
                        hashString = "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = brownMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.45, 0.0),
                        hashString = "578897b83f51fb96b59ba418ff0868cef7bdf661e315ba5dbac51d876d1d15d"
                    )
                ),
                6..6
            )

        ),
        maxStage = 6,
        requiredSoil = setOf(Blocks.MYCELIUM, Blocks.PODZOL),
        needsWater = false,
        isBaseCrop = true
    )
}