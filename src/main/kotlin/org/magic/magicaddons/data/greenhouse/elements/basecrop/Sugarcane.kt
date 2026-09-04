package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.CropEffect
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.sugarcaneState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Sugarcane : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Sugar Cane",
        effects = setOf(
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("SUGAR_CANE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = sugarcaneState()
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = null,
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = sugarcaneState()
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        blockState = wheatState(3)
                    ),
                ),
                armorStands = listOf(),
                2..2
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    CropBlockState(
                        offset = BlockPos(0,3,0),
                        blockState = wheatState(1)
                    ),
                armorStands = listOf(
                ),
                3..3
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    CropBlockState(
                        offset = BlockPos(0, 3, 0),
                        blockState = wheatState(3)
                    ),
                armorStands = listOf(),
                4..4
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0)
                    ),
                    blockState = sugarcaneState()
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(0, 4, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = null,
                6..6
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0),
                        BlockPos(0,3,0),
                        BlockPos(0,4,0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    CropBlockState(
                        offset = BlockPos(0,5,0),
                        blockState = wheatState(1)
                    ),
                armorStands = null,
                7..7
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0),
                        BlockPos(0,3,0),
                        BlockPos(0,4,0)
                    ),
                    blockState = sugarcaneState()
                ) +
                    CropBlockState(
                        offset = BlockPos(0,5,0),
                        blockState = wheatState(5)
                    ),
                armorStands = null,
                8..8
            )


        ),
        maxStage = 8,
        requiredSoil = setOf(Blocks.DIRT,Blocks.SAND),
        isBaseCrop = true
    )

}