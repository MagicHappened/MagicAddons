package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.RareCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Helianthus : RareCrop() {
    override val name: String = "Helianthus"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("HELIANTHUS")
}