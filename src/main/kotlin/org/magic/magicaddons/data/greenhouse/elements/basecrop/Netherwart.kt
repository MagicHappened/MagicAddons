package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.BaseCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Netherwart : BaseCrop() {
    override val name: String = "Nether Wart"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("NETHER_STALK")
}