package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.BaseCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Carrot : BaseCrop() {
    override val name: String = "Carrot"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("CARROT_ITEM")
}