package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.RareCrop
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Cropie : RareCrop() {
    override val name: String = "Cropie"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("CROPIE")
}