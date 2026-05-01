package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Melon : BaseCrop() {
    override val name: String = "Melon"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("MELON")

    override val stageDefs: List<CropStage> = listOf(
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0, 1, 0),
                    matcher = {
                        it.isBlock("minecraft:melon_stem") &&
                                it.getIntProperty("age") == 3
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, 0.125, 0.0),
                    matcher = {
                        it == "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4"
                    }
                )
            ),
            1..1
        )

    )
}