package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Startlevine : Mutation() {
    override val name: String = "Startlevine"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("STARTLEVINE")
}