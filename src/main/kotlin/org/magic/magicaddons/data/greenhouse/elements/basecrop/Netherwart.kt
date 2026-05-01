package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Netherwart : BaseCrop() {
    override val name: String = "Nether Wart"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("NETHER_STALK")

    override val stageDefs: List<CropStage> = listOf(
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:nether_wart") &&
                                it.getIntProperty("age") == 0
                    }
                )
            ),
            armorStands = null,
            1..2
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:nether_wart") &&
                                it.getIntProperty("age") == 1
                    }
                )
            ),
            armorStands = listOf(
            ),
            4..4
        ),
                CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:nether_wart") &&
                                it.getIntProperty("age") == 3
                    }
                )
            ),
            armorStands = null,
            8..8
        )
    )
}