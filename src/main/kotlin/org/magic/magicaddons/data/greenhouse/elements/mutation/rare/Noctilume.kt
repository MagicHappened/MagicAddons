package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.Footprint
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Noctilume : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Noctilume",
        skyblockId = SkyBlockItemId.item("NOCTILUME"),
        stageDefs = listOf(),
        footprint = Footprint(2,2),
        isMutation = true
    )
}