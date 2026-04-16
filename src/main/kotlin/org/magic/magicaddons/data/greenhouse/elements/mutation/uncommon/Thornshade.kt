package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Thornshade : Mutation() {
    override val name: String = "Thornshade"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("THORNSHADE")
}