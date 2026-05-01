package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Duskbloom : Mutation() {
    override val name: String = "Duskbloom"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("DUSKBLOOM")
}