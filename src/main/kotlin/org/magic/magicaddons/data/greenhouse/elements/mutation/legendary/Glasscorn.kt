package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.Footprint
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Glasscorn : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Glasscorn",
        effects = setOf(
            CropEffect.Immunity,
            CropEffect.ImprovedWaterRetain,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("GLASSCORN"),
        stageDefs = listOf(),
        maxStage = 9,
        footprint = Footprint(2, 2),
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}