package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Cheesebite : Mutation() {
    override val name: String = "Cheesebite"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("CHEESBITE")

}