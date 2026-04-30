package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Potato : BaseCrop() {
    override val name: String = "Potato"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("POTATO_ITEM")

    override val stageDefs: List<CropStage> = listOf(
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,0,0),
                    matcher = {
                        it.isBlock("minecraft:potatoes") &&
                                it.getIntProperty("age") == 0
                    }
                )
            ),
            armorStands = null,
            1..1
        )
    )
}