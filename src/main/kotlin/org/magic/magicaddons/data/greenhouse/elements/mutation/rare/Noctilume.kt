package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropExtraInfo
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Noctilume : CropDefinitionProvider {
    sealed interface NoctilumeInfo : CropExtraInfo {
        data object Day : NoctilumeInfo
        data object Night : NoctilumeInfo
    }
    private val wheatPositions = listOf(
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(1, 1, 0),
        BlockPos(1, 1, 1),
    )


    override val definition = CropDefinition(
        name = "Noctilume",
        skyblockId = SkyBlockItemId.item("NOCTILUME"),
        stageDefs = listOf(),
        maxStage = 4,
        footprint = Footprint(2,2),
        isMutation = true
    )
}