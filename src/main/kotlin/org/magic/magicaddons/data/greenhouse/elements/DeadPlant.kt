package org.magic.magicaddons.data.greenhouse.elements

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class DeadPlant : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Dead Plant",
        skyblockId = SkyBlockItemId.item("DEAD_PLANT"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:dead_bush")
                        }
                    )
                ),
                armorStands = null,
                1..1
            )

        ),
        requiredSoil = setOf(Blocks.SAND, Blocks.DIRT, Blocks.FARMLAND, Blocks.MYCELIUM, Blocks.SOUL_SAND, Blocks.END_STONE),
        needsWater = false
    )
}