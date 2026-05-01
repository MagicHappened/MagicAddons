package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Helianthus : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Helianthus",
        skyblockId = SkyBlockItemId.item("HELIANTHUS"),
        stageDefs = listOf(),
        needsWater = false,
        isRareCrop = true
    )
}