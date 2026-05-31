package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object PlantBoyAdvance : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "PlantBoy Advance",
        skyblockId = SkyBlockItemId.item("PLANTBOY_ADVANCE"),
        stageDefs = listOf(
        ),
        maxStage = 12,
        footprint = Footprint(2,2),
        needsWater = false,
        isMutation = true
    )
}