package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Thunderling : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Thunderling",
        skyblockId = SkyBlockItemId.item("THUNDERLING"),
        stageDefs = listOf(),
        needsWater = false,
        isMutation = true
    )
}