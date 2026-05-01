package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Squash : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Squash",
        skyblockId = SkyBlockItemId.item("SQUASH"),
        stageDefs = listOf(),
        needsWater = false,
        isRareCrop = true
    )
}