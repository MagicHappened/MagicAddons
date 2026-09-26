package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.FIVE_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Noctilume
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Snoozling
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object StoplightPetal {
    val definition = CropDefinition(
        name = "Stoplight Petal",
        tier = CropTier.Epic,
        dropMultiplier = 25.0,
        effects = setOf(
            CropEffect.EffectSpread,
            CropEffect.ImprovedWaterRetain,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("STOPLIGHT_PETAL"),
        standPoses = mapOf(
            "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e" to StandPose.Fixed(Rotations(90.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.25, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = true
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.875, -0.21875),
                        headRotation = Rotations(90.0f, 0.0f, 0.0f),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.75, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.84375, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = true
                    )
                ),
                5..5
            ),
            CropStage(
                blocks =             StageBlock.atPositions(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 1.4375, -0.21875),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    ),
                    StageBlock(
                        offset = BlockPos(0,2,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 1.21875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 1.4375, 0.0),
                        hashString = "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811",
                        isSmall = true
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.71875, 0.0),
                        hashString = "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = melonStemState(5)
                ) + listOf(
                    StageBlock(
                        offset = BlockPos(0, 3, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.71875, 0.0),
                        hashString = "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.03125, 2.125, -0.21875),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = true
                    )
                ),
                11..11
            ),
            CropStage(
                blocks =             StageBlock.atPositions(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "4c2b797e7172a05169e313739908515864d6b372f9a5ecc772f81d9c4e402a54",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.71875, 0.0),
                        hashString = "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.03125, 2.125, -0.21875),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e",
                        isSmall = true
                    )
                ),
                12..12
            )





        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 12,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Snoozling" to 4, "Noctilume" to 4))
    )
}