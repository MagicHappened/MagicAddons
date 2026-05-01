package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Coalroot : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Coalroot",
        skyblockId = SkyBlockItemId.item("COALROOT"),
        stageDefs = listOf(),
        isMutation = true
    )
}