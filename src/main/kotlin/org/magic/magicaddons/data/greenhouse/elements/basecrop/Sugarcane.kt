package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
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