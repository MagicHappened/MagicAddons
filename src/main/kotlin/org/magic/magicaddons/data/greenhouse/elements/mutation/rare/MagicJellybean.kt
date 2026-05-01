package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class MagicJellybean : Mutation() {
    override val name: String = "Magic Jellybean"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("MAGIC_JELLYBEAN")
}