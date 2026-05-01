package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Devourer : Mutation() {
    override val name: String = "Devourer"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("DEVOURER")
}