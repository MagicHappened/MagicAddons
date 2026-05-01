package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Fermento : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Fermento",
        skyblockId = SkyBlockItemId.item("FERMENTO"),
        stageDefs = listOf(),
        needsWater = false,
        isRareCrop = true
    )
}