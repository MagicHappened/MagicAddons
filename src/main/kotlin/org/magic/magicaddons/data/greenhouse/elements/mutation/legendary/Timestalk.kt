package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.Rotations
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.FIVE_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.StandPose
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Timestalk : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Timestalk",
        effects = setOf(
            CropEffect.ImprovedWaterRetain,
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("TIMESTALK"),
        standPoses = mapOf(
            "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "1b9add43e0e770b7c4ccdcf6708f8b9e875ff13b2a75ab63ff69f2f0e57af9e5" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(7)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 1.28125, 0.0),
                        hashString = "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8",
                        isSmall = false
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 1.09375, 0.0),
                        hashString = "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.875, 0.0),
                        hashString = "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.6875, 0.0),
                        hashString = "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.59375, 0.0),
                        hashString = "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(7)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.125, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.46875, 0.0),
                        hashString = "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(7)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.21875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(7)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.3125, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.25, 0.0),
                        hashString = "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8",
                        isSmall = false
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(7)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063",
                        isSmall = false
                    )
                ),
                12..12
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.3125, 0.0),
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063"
                    )
                ),
                13..13
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.3125, 0.0),
                        hashString = "1b9add43e0e770b7c4ccdcf6708f8b9e875ff13b2a75ab63ff69f2f0e57af9e5"
                    )
                ),
                14..14
            )
        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 14,
        requiredSoil = setOf(Blocks.END_STONE),
        isMutation = true
    )
}