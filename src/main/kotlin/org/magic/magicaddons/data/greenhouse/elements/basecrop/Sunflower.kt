package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Sunflower : BaseCrop() {
    override val name: String = "Sunflower"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("DOUBLE_PLANT")

    override val stageDefs: List<CropStage> = listOf(
        CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(-0.1875, 0.25, 0.0),
                    matcher = {
                        it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
                    }
                )
            ),
            1..1,
            allowRotation = true
        )


    )
}