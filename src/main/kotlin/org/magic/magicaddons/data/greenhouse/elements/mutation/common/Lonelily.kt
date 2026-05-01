package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Lonelily : CropDefinitionProvider {

    override val definition = CropDefinition(
        name = "Lonelily",
        skyblockId = SkyBlockItemId.item("LONELILY"),
        stageDefs = listOf(),
        needsWater = false,
        isMutation = true
    )
}