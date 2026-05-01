package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Dustgrain : Mutation() {
    override val name: String = "Dustgrain"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("DUSTGRAIN")
}