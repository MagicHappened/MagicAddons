package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

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
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Noctilume : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Noctilume",
        skyblockId = SkyBlockItemId.item("NOCTILUME"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.09375, -0.03125, -0.375),
                        matcher = {
                            it == "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8"
                        }
                    )
                ),
                2..2,
                allowRotation = true
            ),
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
                        offset = Vec3(0.71875, -0.0625, 0.34375),
                        matcher = {
                            it == "5cdd8c3d5d76a1dc07cdbedc5fd0bb230852df9c1864896f8893f5bfdf3d4c96"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.21875, 0.03125, 0.375),
                        matcher = {
                            it == "5cdd8c3d5d76a1dc07cdbedc5fd0bb230852df9c1864896f8893f5bfdf3d4c96"
                        }
                    )
                ),
                4..4,
                allowRotation = true
            )


        ),
        maxStage = 4,
        footprint = Footprint(2,2),
        isMutation = true
    )
}