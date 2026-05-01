package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Turtlellini : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Turtlellini",
        skyblockId = SkyBlockItemId.item("TURTLELLINI"),
        stageDefs = listOf(),
        needsWater = false,
        isMutation = true
    )
}