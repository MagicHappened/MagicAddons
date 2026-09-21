package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Wheat {
    val definition = CropDefinition(
        name = "Wheat",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.18,
        effects = setOf(
            CropEffect.HarvestBoost
        ),
        skyblockId = SkyBlockItemId.item("WHEAT"),
        aliases = listOf(
            SkyBlockItemId.item("SEEDS")
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = null,
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = null,
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = null,
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = null,
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands = null,
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = null,
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(6)
                    )
                ),
                armorStands = null,
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(7)
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