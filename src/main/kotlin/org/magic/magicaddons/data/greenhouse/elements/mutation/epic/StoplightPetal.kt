package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object StoplightPetal : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Stoplight Petal",
        effects = setOf(
            CropEffect.EffectSpread,
            CropEffect.ImprovedWaterRetain,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("STOPLIGHT_PETAL"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e" to StandPose.Fixed(Rotations(90.0f, 0.0f, 0.0f)),
            "4c2b797e7172a05169e313739908515864d6b372f9a5ecc772f81d9c4e402a54" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.25, 0.0),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.875, -0.21875),
                        headRotation = Rotations(90.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
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
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.75, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0),
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.84375, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks =             CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
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
                    blockState = melonStemState(5)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.0, 1.4375, -0.21875),
                        Vec3(0.0, -0.4375, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(90.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        0.0f
                    ),
                    hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                    isSmall = false
                ) +
                listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 1.21875, 0.0),
                        hashString = "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    )
                ),
                8..8,
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0),
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.71875, 0.0),
                        hashString = "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e"
                    )
                ),
                10..10
            ),
            CropStage(
                blocks =             CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "4c2b797e7172a05169e313739908515864d6b372f9a5ecc772f81d9c4e402a54",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.71875, 0.0),
                        hashString = "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.03125, 2.125, -0.21875),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e"
                    )
                ),
                12..12
            )





        ),
        // five days decay timer
        maxStage = 12,
        isMutation = true
    )
}