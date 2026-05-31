package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cindershade : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cindershade",
        skyblockId = SkyBlockItemId.item("CINDERSHADE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:nether_wart") &&
                                    it.getIntProperty("age") == 3
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashMatches = {
                            it == "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        hashMatches = {
                            it == "a0646bc0558155207204711cf5d3d07920e0e98c9b2be0b6107becb409a97427"
                        }
                    )
                ),
                8..8
            )

        ),
        maxStage = 8,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )
}