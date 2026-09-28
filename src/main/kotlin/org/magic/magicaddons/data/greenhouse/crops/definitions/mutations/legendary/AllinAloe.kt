package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.stateOf
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object AllinAloe {
    private val fragmentSkyblockId: SkyBlockId = SkyBlockItemId.item("ALL_IN_ALOE_FRAGMENT")

    val definition = CropDefinition(
        name = "All-in Aloe",
        tier = CropTier.Legendary,
        dropMultiplier = 0.7,
        effects = setOf(
            CropEffect.HarvestBoost
        ),
        skyblockId = SkyBlockItemId.item("ALL_IN_ALOE"),
        standPoses = mapOf(
            "dde18b1db0f938380dd8bed0c9189c3e62ea3acf900a19b2e95f52708c3ae3f2" to StandPose.Fixed(Rotations(22.5f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.25, -0.125),
                        hashString = "dde18b1db0f938380dd8bed0c9189c3e62ea3acf900a19b2e95f52708c3ae3f2"
                    )
                ),
                1..1,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
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
                        offset = Vec3(0.0, -0.15625, -0.125),
                        hashString = "dde18b1db0f938380dd8bed0c9189c3e62ea3acf900a19b2e95f52708c3ae3f2",
                        isSmall = false
                    )
                ),
                2..2,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.0625, -0.125),
                        hashString = "dde18b1db0f938380dd8bed0c9189c3e62ea3acf900a19b2e95f52708c3ae3f2",
                        isSmall = false
                    )
                ),
                3..3,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.0625, 0.09375),
                        hashString = "955a5ebfc03404c361753d267f7d1664692a2da3ebc63fc3b74925015ab7171b"
                    )
                ),
                4..4,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.09375),
                        hashString = "955a5ebfc03404c361753d267f7d1664692a2da3ebc63fc3b74925015ab7171b",
                        isSmall = false
                    )
                ),
                5..5,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.15625, 0.09375),
                        hashString = "955a5ebfc03404c361753d267f7d1664692a2da3ebc63fc3b74925015ab7171b"
                    )
                ),
                6..6,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.09375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "241163612258d30dc6ef63b21f61ba89c622e5dcebd99fd36a3b507e80cdc725",
                        isSmall = false
                    )
                ),
                7..7,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.25, 0.09375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "241163612258d30dc6ef63b21f61ba89c622e5dcebd99fd36a3b507e80cdc725",
                        isSmall = false
                    )
                ),
                8..8,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.34375, 0.09375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "241163612258d30dc6ef63b21f61ba89c622e5dcebd99fd36a3b507e80cdc725",
                        isSmall = false
                    )
                ),
                9..9,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = stateOf("minecraft:spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]")
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.65625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "eef0a5b20bf0ba3eb017955cc5209cf0635f15598b5ccca83a82a09c66914a1c"
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = stateOf("minecraft:spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]")
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.84375, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "eef0a5b20bf0ba3eb017955cc5209cf0635f15598b5ccca83a82a09c66914a1c"
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = stateOf("minecraft:spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]")
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 1.0625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "eef0a5b20bf0ba3eb017955cc5209cf0635f15598b5ccca83a82a09c66914a1c"
                    )
                ),
                12..12
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = stateOf("minecraft:spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]")
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 1.25, 0.09375),
                        headRotation = Rotations(-45.0f, 0.0f, 0.0f),
                        hashString = "d846f1f7ea8b021e1feedb00811baa8d3eb4de88800a7ebd8f852e806e60af90",
                        isSmall = false
                    )
                ),
                13..14,
                readers = listOf(
                    StandReader.stageNumberLabel(),
                    StandReader.percentLabel(StandReader.REWARDS_RESET, "reset"),
                    StandReader.multiplierLabel(StandReader.REWARDS_MULTIPLIER, "reward")
                )
            )
        ),
        decayTimeMs = NEVER_DECAYS,
        maxStage = 27,
        requiredSoil = setOf(Blocks.SAND, Blocks.RED_SAND),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Magic Jellybean" to 6, "PlantBoy Advance" to 2))
    )
}