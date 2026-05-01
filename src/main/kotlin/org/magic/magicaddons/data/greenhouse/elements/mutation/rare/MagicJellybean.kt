package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class MagicJellybean : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Magic Jellybean",
        skyblockId = SkyBlockItemId.item("MAGIC_JELLYBEAN"),
        stageDefs = listOf(),
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}