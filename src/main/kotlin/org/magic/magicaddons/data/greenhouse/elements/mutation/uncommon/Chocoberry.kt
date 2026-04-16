package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Chocoberry : Mutation() {
    override val name: String = "Chocoberry"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("CHOCOBERRY")
}