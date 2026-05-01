package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Ashwreath : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Ashwreath",
        skyblockId = SkyBlockItemId.item("ASHWREATH"),
        stageDefs = listOf(),
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )

}