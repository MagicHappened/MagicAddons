package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.BaseCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Fermento : BaseCrop() {
    override val name: String = "Fermento"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("FERMENTO")
}