package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Gloomgourd : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Ashwreath",
        skyblockId = SkyBlockItemId.item("ASHWREATH"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 6
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.78125, 0.0),
                        matcher = {
                            it == "7f693e42ba3b763292e7de26fd2b0a08fcee3bec2e017075dc66dfc4a932aa64"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        matcher = {
                            it == "7f693e42ba3b763292e7de26fd2b0a08fcee3bec2e017075dc66dfc4a932aa64"
                        }
                    )
                ),
                1..1
            )

        ),
        needsWater = false,
        isMutation = true
    )

    /*
    override val standHashes: MutableList<String> = mutableListOf(
        "7f693e42ba3b763292e7de26fd2b0a08fcee3bec2e017075dc66dfc4a932aa64"
    )
    */
}