package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Devourer : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Devourer",
        skyblockId = SkyBlockItemId.item("DEVOURER"),
        stageDefs = listOf(),
        isMutation = true
    )
}