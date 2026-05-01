package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class PlantBoyAdvance : Mutation() {
    override val name: String = "PlantBoy Advance"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("PLANTBOY_ADVANCE")
}