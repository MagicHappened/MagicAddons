package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Coalroot : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Coalroot",
        skyblockId = SkyBlockItemId.item("COALROOT"),
        stageDefs = listOf(),
        maxStage = 8,
        isMutation = true
    )
}