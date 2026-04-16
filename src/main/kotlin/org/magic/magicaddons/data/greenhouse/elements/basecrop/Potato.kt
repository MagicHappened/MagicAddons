package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.BaseCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Potato : BaseCrop() {
    override val name: String = "Potato"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("POTATO_ITEM")
}