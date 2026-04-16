package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Snoozling : Mutation() {
    override val name: String = "Snoozling"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("SNOOZLING")
}