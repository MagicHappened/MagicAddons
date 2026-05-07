package org.magic.magicaddons.data.greenhouse.elements

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class FireElement : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Fire",
        skyblockId = null,
        aliases = listOf(SkyBlockItemId.item("FLINT_AND_STEEL")),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:fire") //todo check if true
                        }
                    )
                ),
                stageRange = 1..1
            ),
        ),
        needsWater = false,
        requiredSoil = setOf(Blocks.SOUL_SAND, Blocks.NETHERRACK)

    )
}