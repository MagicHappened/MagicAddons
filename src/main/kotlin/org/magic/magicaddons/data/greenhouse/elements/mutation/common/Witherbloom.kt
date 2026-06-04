package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.deadBushState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Witherbloom : CropDefinitionProvider {

    override val definition = CropDefinition(
        name = "Witherbloom",
        skyblockId = SkyBlockItemId.item("WITHERBLOOM"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = deadBushState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.40625, 0.0),
                        hashString = "ef831051cf18a4c3ea7a2a83311f218f43b032712799cc81910ab67ee7397b32"
                    )
                ),
                1..1
            )

        ),
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )
}