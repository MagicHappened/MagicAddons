package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class AllinAloe : Mutation() {
    override val name: String = "All-in Aloe"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("ALL_IN_ALOE")
    val fragmentSkyblockId: SkyBlockId = SkyBlockItemId.item("ALL_IN_ALOE_FRAGMENT")
}