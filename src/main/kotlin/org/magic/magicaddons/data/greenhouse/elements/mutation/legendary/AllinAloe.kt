package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class AllinAloe : CropDefinitionProvider {
    val fragmentSkyblockId: SkyBlockId = SkyBlockItemId.item("ALL_IN_ALOE_FRAGMENT")

    override val definition = CropDefinition(
        name = "All-in Aloe",
        skyblockId = SkyBlockItemId.item("ALL_IN_ALOE"),
        stageDefs = listOf(),
        requiredSoil = setOf(Blocks.SAND),
        needsWater = false,
        isMutation = true
    )
}