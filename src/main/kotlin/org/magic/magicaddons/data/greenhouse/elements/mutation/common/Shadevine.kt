package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Shadevine : CropDefinitionProvider {

    override val definition = CropDefinition(
        name = "Shadevine",
        skyblockId = SkyBlockItemId.item("SHADEVINE"),
        stageDefs = listOf(),
        requiredSoil = setOf(Blocks.FARMLAND, Blocks.SAND),
        needsWater = false,
        isMutation = true
    )
}