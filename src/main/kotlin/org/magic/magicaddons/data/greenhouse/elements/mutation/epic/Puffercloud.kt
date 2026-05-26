package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

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
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Puffercloud : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Puffercloud",
        skyblockId = SkyBlockItemId.item("PUFFERCLOUD"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.71875, 0.0),
                        matcher = {
                            it == "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04"
                        }
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.71875, 0.0),
                        matcher = {
                            it == "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04"
                        }
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 3
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.6875, 0.0),
                        matcher = {
                            it == "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04"
                        }
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 3
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, 0.5),
                        matcher = {
                            it == "2f9502a6895a90bbbb25921842fa6a213744ba967bbdfd861a44c92a79530aa0"
                        }
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 3
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.59375, 0.0),
                        matcher = {
                            it == "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b"
                        }
                    )
                ),
                10..10
            )




        ),
        maxStage = 14,
        isMutation = true
    )
}