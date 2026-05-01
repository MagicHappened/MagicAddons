package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Witherbloom : Mutation() {
    override val name: String = "Witherbloom"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("WITHERBLOOM")
}