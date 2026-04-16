package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Puffercloud : Mutation() {
    override val name: String = "Puffercloud"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("PUFFERCLOUD")
}