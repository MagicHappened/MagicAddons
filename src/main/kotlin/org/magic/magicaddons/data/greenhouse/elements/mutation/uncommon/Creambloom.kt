package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Creambloom : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Creambloom",
        skyblockId = SkyBlockItemId.item("CREAMBLOOM"),
        stageDefs = listOf(),
        isMutation = true
    )
}