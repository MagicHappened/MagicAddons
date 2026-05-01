package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Timestalk : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Timestalk",
        skyblockId = SkyBlockItemId.item("TIMESTALK"),
        stageDefs = listOf(),
        requiredSoil = setOf(Blocks.END_STONE),
        isMutation = true
    )
}