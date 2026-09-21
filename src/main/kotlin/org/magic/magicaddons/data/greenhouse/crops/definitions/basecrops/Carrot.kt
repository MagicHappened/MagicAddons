package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.carrotState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Carrot {
    val definition = CropDefinition(
        name = "Carrot",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.125,
        effects = setOf(
            CropEffect.XpBoost
        ),
        skyblockId = SkyBlockItemId.item("CARROT_ITEM"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = carrotState(0)
                    )
                ),
                armorStands = null,
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = carrotState(1)
                    )
                ),
                armorStands = null,
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = carrotState(2)
                    )
                ),
                armorStands = null,
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = carrotState(3)
                    )
                ),
                armorStands = null,
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = carrotState(4)
                    )
                ),
                armorStands = null,
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = carrotState(5)
                    )
                ),
                armorStands = null,
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = carrotState(6)
                    )
                ),
                armorStands = null,
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = carrotState(7)
                    )
                ),
                armorStands = null,
                8..8
            )

        ),
        maxStage = 8,
        isBaseCrop = true

    )
}