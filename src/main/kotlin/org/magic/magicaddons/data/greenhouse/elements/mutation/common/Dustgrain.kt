package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Dustgrain : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Dustgrain",
        skyblockId = SkyBlockItemId.item("DUSTGRAIN"),
        stageDefs = listOf(),
        needsWater = false,
        isMutation = true
    )
}