package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Fleshtrap : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Fleshtrap",
        skyblockId = SkyBlockItemId.item("FLESHTRAP"),
        stageDefs = listOf(),
        isMutation = true
    )
}