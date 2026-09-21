package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.netherwartState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Netherwart {
    val definition = CropDefinition(
        name = "Nether Wart",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.09,
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.XpLoss
        ),
        skyblockId = SkyBlockItemId.item("NETHER_STALK"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = null,
                1..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(1)
                    )
                ),
                armorStands = null,
                4..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = null,
                6..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(3)
                    )
                ),
                armorStands = null,
                8..8
            )

        ),
        maxStage = 8,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isBaseCrop = true
    )
}