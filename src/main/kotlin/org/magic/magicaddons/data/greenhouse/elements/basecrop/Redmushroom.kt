package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.BaseCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Redmushroom : BaseCrop() {
    override val name: String = "Red Mushroom"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("RED_MUSHROOM")
}