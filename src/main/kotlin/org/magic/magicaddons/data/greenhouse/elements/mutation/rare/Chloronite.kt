package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Chloronite : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Chloronite",
        skyblockId = SkyBlockItemId.item("CHLORONITE"),
        stageDefs = listOf(),
        isMutation = true
    )
}