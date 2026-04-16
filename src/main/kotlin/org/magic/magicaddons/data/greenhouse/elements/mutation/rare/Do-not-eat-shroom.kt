package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class `Do-not-eat-shroom` : Mutation() {
    override val name: String = "Do-not-eat-shroom"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("DO_NOT_EAT_SHROOM")
}