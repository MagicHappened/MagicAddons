package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Thornshade : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Thornshade",
        skyblockId = SkyBlockItemId.item("THORNSHADE"),
        stageDefs = listOf(),
        isMutation = true
    )
}