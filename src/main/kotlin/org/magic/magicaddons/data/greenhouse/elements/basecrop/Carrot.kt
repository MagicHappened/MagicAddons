package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Carrot : BaseCrop() {
    override val name: String = "Carrot"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("CARROT_ITEM")

    override val stageDefs: List<CropStage> = mutableListOf(
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:carrots") &&
                                it.getIntProperty("age") == 0
                    }
                )
            ),
            armorStands = null,
            1..1
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,0,0),
                    matcher = {
                        it.isBlock("minecraft:carrots") &&
                                it.getIntProperty("age") == 1
                    }
                )
            ),
            armorStands = null,
            2..2
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:carrots") &&
                                it.getIntProperty("age") == 2
                    }
                )
            ),
            armorStands = listOf(
            ),
            3..3
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:carrots") &&
                                it.getIntProperty("age") == 3
                    }
                )
            ),
            armorStands = listOf(
            ),
            4..4
        )

    )

}