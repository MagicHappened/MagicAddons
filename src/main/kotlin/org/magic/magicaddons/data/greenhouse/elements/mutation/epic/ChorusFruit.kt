package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class ChorusFruit : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Chorus Fruit",
        skyblockId = SkyBlockItemId.item("CHORUS_FRUIT"),
        stageDefs = listOf(),
        requiredSoil = setOf(Blocks.END_STONE),
        needsWater = false,
        isMutation = true
    )
}