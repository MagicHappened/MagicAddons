package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Zombud : Mutation() {
    override val name: String = "Zombud"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("ZOMBUD")
}