package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Cindershade : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cindershade",
        skyblockId = SkyBlockItemId.item("CINDERSHADE"),
        stageDefs = listOf(),
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )
}