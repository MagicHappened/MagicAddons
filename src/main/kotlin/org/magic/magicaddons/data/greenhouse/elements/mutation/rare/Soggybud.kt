package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Soggybud : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Soggybud",
        skyblockId = SkyBlockItemId.item("SOGGYBUD"),
        stageDefs = listOf(),
        isMutation = true
    )
}