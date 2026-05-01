package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class ChorusFruit : Mutation() {
    override val name: String = "Chorus Fruit"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("CHORUS_FRUIT")
}