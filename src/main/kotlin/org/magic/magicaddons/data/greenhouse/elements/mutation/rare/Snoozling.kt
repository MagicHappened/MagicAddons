package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.Footprint
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Snoozling : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Snoozling",
        skyblockId = SkyBlockItemId.item("SNOOZLING"),
        stageDefs = listOf(),
        footprint = Footprint(3,3),
        isMutation = true
    )
}