package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Cropie : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cropie",
        skyblockId = SkyBlockItemId.item("CROPIE"),
        stageDefs = listOf(),
        needsWater = false,
        isRareCrop = true
    )
}