package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.Rotations
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
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
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "7cf25e2106b1f8ac856c2d13e8850cdb6b4f96ae9df243a605d6a6d2e1fdacf8" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
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
                    hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063"
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
                        hashString = "d2306f4c5946990204517a73bbfa8281fd7d9a294f908b0286e708c51f79a063"
                    )
                ),
                11..11
            )

        ,
            // as placed
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.3125, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "1b9add43e0e770b7c4ccdcf6708f8b9e875ff13b2a75ab63ff69f2f0e57af9e5"
                    )
                ),
                14..14,
                placed = true
            )),
        // five days decay timer
        maxStage = 14,
        requiredSoil = setOf(Blocks.END_STONE),
        isMutation = true
    )
}