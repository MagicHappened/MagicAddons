package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Timestalk : Mutation() {
    override val name: String = "Timestalk"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("TIMESTALK")
}