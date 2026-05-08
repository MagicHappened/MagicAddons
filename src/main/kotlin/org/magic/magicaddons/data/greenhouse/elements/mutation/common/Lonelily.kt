package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Lonelily : CropDefinitionProvider {

    override val definition = CropDefinition(
        name = "Lonelily",
        skyblockId = SkyBlockItemId.item("LONELILY"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 3
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.125, 0.0),
                        matcher = {
                            it == "f4672d5fb128c63197da27f40d7143818d318665b3ab2335121e9ae8f9ecc2e3"
                        }
                    )
                ),
                1..1,
                allowRotation = true
            )

        ),
        needsWater = false,
        isMutation = true
    )
}