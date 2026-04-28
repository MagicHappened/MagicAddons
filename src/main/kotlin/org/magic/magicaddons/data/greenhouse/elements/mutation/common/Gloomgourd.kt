package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.Mutation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Gloomgourd : Mutation() {
    override val name: String = "Gloomgourd"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("GLOOMGOURD")
    override val standHashes: MutableList<String> = mutableListOf(
        "7f693e42ba3b763292e7de26fd2b0a08fcee3bec2e017075dc66dfc4a932aa64"
    )
}