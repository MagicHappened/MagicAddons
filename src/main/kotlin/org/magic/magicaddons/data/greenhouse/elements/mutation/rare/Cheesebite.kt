package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Cheesebite : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cheesebite",
        skyblockId = SkyBlockItemId.item("CHEESBITE"),
        stageDefs = listOf(),
        isMutation = true
    )
}