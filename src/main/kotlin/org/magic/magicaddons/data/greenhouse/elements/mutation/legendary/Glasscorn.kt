package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.Footprint
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Glasscorn : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Glasscorn",
        skyblockId = SkyBlockItemId.item("GLASSCORN"),
        stageDefs = listOf(),
        footprint = Footprint(2, 2),
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}