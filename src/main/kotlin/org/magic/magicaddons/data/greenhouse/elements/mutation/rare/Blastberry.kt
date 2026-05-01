package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Blastberry : Mutation() {
    override val name: String = "Blastberry"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("BLASTBERRY")
}