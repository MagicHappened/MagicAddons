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

class Wildrose : BaseCrop() {
    override val name: String = "Wild Rose"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("WILD_ROSE")

    override val stageDefs: List<CropStage> = listOf(
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0, 1, 0),
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 0
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, 0.0625, 0.0),
                    matcher = {
                        it == "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece"
                    }
                )
            ),
            1..1
        )

    )
}