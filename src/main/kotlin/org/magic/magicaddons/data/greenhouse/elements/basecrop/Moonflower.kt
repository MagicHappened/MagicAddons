package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Moonflower : BaseCrop() {
    override val name: String = "Moonflower"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("MOONFLOWER")

    override val stageDefs: List<CropStage> = listOf(
        CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(-0.1875, 0.25, 0.0),
                    matcher = {
                        it == "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                    }
                )
            ),
            1..1,
            allowRotation = true
        )

    )
}