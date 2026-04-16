package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.BaseCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Squash : BaseCrop() {
    override val name: String = "Squash"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("SQUASH")
}