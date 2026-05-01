package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Scourroot : Mutation() {
    override val name: String = "Scourroot"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("SCOURROOT")
}