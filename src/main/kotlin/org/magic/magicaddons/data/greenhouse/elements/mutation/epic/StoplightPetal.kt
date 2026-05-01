package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class StoplightPetal : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Stoplight Petal",
        skyblockId = SkyBlockItemId.item("STOPLIGHT_PETAL"),
        stageDefs = listOf(),
        isMutation = true
    )
}