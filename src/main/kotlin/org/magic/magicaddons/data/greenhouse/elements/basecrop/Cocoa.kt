package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.BaseCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Cocoa : BaseCrop() {
    override val name: String = "Cocoa Beans"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("INK_SACK-3")
}