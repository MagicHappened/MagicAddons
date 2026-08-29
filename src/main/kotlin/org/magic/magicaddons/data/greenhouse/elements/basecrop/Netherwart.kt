package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.CropEffect
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.netherwartState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Netherwart : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Nether Wart",
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.XpLoss
        ),
        skyblockId = SkyBlockItemId.item("NETHER_STALK"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(),
                1..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(1)
                    )
                ),
                armorStands = listOf(),
                4..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = listOf(),
                6..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(3)
                    )
                ),
                armorStands = listOf(),
                8..8
            )

        ),
        maxStage = 8,
        decayTimeMs = DECAY_TIME_MS,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isBaseCrop = true
    )
}