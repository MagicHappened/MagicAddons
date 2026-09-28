package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.netherwartState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.FIVE_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Fleshtrap
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.Cindershade
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Zombud {
    val definition = CropDefinition(
        name = "Zombud",
        tier = CropTier.Epic,
        dropMultiplier = 4.5,
        effects = setOf(
            CropEffect.EffectSpread,
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("ZOMBUD"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.21875, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        isSmall = true
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "29e27b7ee26e272ce53f19e169a593ea83cc120bfa3a209e1a8a279fcdf463e7",
                        isSmall = false
                    )
                ),
                4..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        hashString = "29e27b7ee26e272ce53f19e169a593ea83cc120bfa3a209e1a8a279fcdf463e7",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.375, 0.0),
                        hashString = "29e27b7ee26e272ce53f19e169a593ea83cc120bfa3a209e1a8a279fcdf463e7",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.34375, 0.0),
                        hashString = "29e27b7ee26e272ce53f19e169a593ea83cc120bfa3a209e1a8a279fcdf463e7",
                        isSmall = false
                    )
                ),
                8..9
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.28125, 0.0),
                        hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        isSmall = false
                    )
                ),
                10..11
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.28125, 0.0),
                        hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        isSmall = false
                    )
                ),
                12..12
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.09375, 0.0),
                        hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.40625, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        isSmall = true
                    )
                ),
                13..13
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.40625, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        isSmall = true
                    )
                ),
                14..14
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.125, 0.0),
                        hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.40625, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        isSmall = true
                    )
                ),
                15..15
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.375, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.21875, 0.0),
                        hashString = "b2c4994b7a1c45231b623b8245c117382b267c8856c57cffa2d808c241027a51",
                        isSmall = false
                    )
                ),
                16..16
            )
        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 16,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Dead Plant" to 4, "Cindershade" to 2, "Fleshtrap" to 2))
    )
}