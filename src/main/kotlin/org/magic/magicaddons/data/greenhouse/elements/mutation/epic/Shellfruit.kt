package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Shellfruit : Mutation() {
    override val name: String = "Shellfruit"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("SHELLFRUIT")
}