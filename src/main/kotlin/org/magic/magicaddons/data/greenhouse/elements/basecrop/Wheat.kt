package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.CropEffect
import net.minecraft.core.BlockPos
import org.magic.magicaddons.data.greenhouse.DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Wheat : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Wheat",
        effects = setOf(
            CropEffect.HarvestBoost
        ),
        skyblockId = SkyBlockItemId.item("WHEAT"),
        aliases = listOf(
            SkyBlockItemId.item("SEEDS")
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = null,
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = null,
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = null,
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = null,
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands = null,
                5..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(6)
                    )
                ),
                armorStands = null,
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(7)
                    )
                ),
                armorStands = null,
                8..8
            )

        ),
        maxStage = 8,
        decayTimeMs = DECAY_TIME_MS,
        isBaseCrop = true
    )
}