package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Choconut : Mutation() {
    override val name: String = "Choconut"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("CHOCONUT")
}