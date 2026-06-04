package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
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
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    wheatPositions,
                    blockState = wheatState(6)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(-0.21875, -0.0625, 0.15625),
                        Vec3(0.375, 0.09375, -0.3125),
                        Vec3(0.28125, 0.03125, 0.125),
                        Vec3(-0.125, -0.03125, -0.40625)
                    ),
                    hashString = "b1b18493d50ff8972f7ef359893d9063fdc54cb822c679002957c294fc8b0005"
                ),
                4..4,
                allowRotation = true,
                NoctilumeInfo.Night
            )

        ),
        maxStage = 4,
        footprint = Footprint(2,2),
        isMutation = true
    )
}