package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.sugarcaneState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Sugarcane {
    val definition = CropDefinition(
        name = "Sugar Cane",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.17,
        effects = setOf(
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("SUGAR_CANE"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = sugarcaneState()
                    ),
                    StageBlock(
                        offset = BlockPos(0,2,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = null,
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = sugarcaneState()
                    ),
                    StageBlock(
                        offset = BlockPos(0,2,0),
                        blockState = wheatState(3)
                    ),
                ),
                armorStands = null,
                2..2
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    StageBlock(
                        offset = BlockPos(0,3,0),
                        blockState = wheatState(1)
                    ),
                armorStands = null,
                3..3
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    StageBlock(
                        offset = BlockPos(0, 3, 0),
                        blockState = wheatState(3)
                    ),
                armorStands = null,
                4..4
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    StageBlock(
                        offset = BlockPos(0, 4, 0),
                        blockState = wheatState(1)
                    ),
                armorStands = null,
                5..5
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    StageBlock(
                        offset = BlockPos(0, 4, 0),
                        blockState = wheatState(3)
                    ),
                armorStands = null,
                6..6
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0),
                        BlockPos(0,3,0),
                        BlockPos(0,4,0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    StageBlock(
                        offset = BlockPos(0,5,0),
                        blockState = wheatState(1)
                    ),
                armorStands = null,
                7..7
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0),
                        BlockPos(0,3,0),
                        BlockPos(0,4,0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    StageBlock(
                        offset = BlockPos(0,5,0),
                        blockState = wheatState(5)
                    ),
                armorStands = null,
                8..8
            )


        ),
        maxStage = 8,
        requiredSoil = setOf(Blocks.DIRT,Blocks.SAND, Blocks.RED_SAND),
        isBaseCrop = true
    )

}