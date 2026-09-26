package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.cactusState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cactus {
    val definition = CropDefinition(
        name = "Cactus",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.22,
        effects = setOf(
            CropEffect.ImprovedWaterRetain,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("CACTUS"),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.78125, 0.0),
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f",
                        isSmall = true
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0, 0.09375, 0.0),
                        Vec3(0.0, -0.5, 0.0)
                    ),
                    hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f",
                    isSmall = false
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = cactusState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.5, 0.0),
                        isSmall = false,
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    )
                ),
                4..4,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = cactusState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.5, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.1875, 1.53125, -0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 67.5f),
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f",
                        isSmall = true
                    )
                ),
                5..5,
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0)
                    ),
                    blockState = cactusState()
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.3125, 1.0, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 67.5f),
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    )
                ),
                6..6,
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0)
                    ),
                    blockState = cactusState()
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(-0.15625, 2.59375, -0.03125),
                        headRotation = Rotations(0.0f, 0.0f, -67.5f),
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f",
                        isSmall = true
                    )
                ) + StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.3125, 1.0, 0.0),
                        Vec3(0.0, 1.5, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 67.5f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f",
                    isSmall = false
                ),
                7..7,
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0)
                    ),
                    blockState = cactusState()
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.3125, 1.0, 0.0),
                        Vec3(-0.28125, 2.0, -0.03125)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 67.5f),
                        Rotations(0.0f, 0.0f, -67.5f)
                    ),
                    hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f",
                    isSmall = false
                ),
                8..8
            )


        ),
        maxStage = 8,
        requiredSoil = setOf(Blocks.SAND, Blocks.RED_SAND),
        needsWater = false,
        isBaseCrop = true
    )





}