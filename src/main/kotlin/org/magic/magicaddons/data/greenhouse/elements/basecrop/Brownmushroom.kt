package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.BaseCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Brownmushroom : BaseCrop() {
    override val name: String = "Brown Mushroom"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("BROWN_MUSHROOM")
}