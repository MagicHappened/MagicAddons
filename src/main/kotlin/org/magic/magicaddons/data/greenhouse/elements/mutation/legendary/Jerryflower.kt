package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Jerryflower : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Jerryflower",
        skyblockId = SkyBlockItemId.item("JERRYFLOWER"),
        stageDefs = listOf(),
        isMutation = true
    )
}