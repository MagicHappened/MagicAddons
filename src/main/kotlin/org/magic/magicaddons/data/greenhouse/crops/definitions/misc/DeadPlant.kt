package org.magic.magicaddons.data.greenhouse.crops.definitions.misc

import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.deadBushState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object DeadPlant {
    val definition = CropDefinition(
        name = "Dead Plant",
        tier = CropTier.Other,
        skyblockId = SkyBlockItemId.item("DEAD_PLANT"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = deadBushState()
                    )
                ),
                armorStands = null,
                1..1
            )
        ),
        decayTimeMs = NEVER_DECAYS,
        requiredSoil = setOf(Blocks.SAND, Blocks.RED_SAND, Blocks.DIRT, Blocks.FARMLAND, Blocks.MYCELIUM, Blocks.PODZOL, Blocks.SOUL_SAND, Blocks.END_STONE),
        needsWater = false,
        isMutation = false,
        displayItem = Items.DEAD_BUSH
    )
}