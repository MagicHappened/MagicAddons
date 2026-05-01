package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Startlevine : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Startlevine",
        skyblockId = SkyBlockItemId.item("STARTLEVINE"),
        stageDefs = listOf(),
        isMutation = true
    )
}