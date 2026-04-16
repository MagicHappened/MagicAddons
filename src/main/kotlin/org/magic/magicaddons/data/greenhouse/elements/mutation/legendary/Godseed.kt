package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Godseed : Mutation() {
    override val name: String = "Godseed"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("GODSEED")
}