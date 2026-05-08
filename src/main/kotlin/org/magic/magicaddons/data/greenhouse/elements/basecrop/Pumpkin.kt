package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Pumpkin : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Pumpkin",
        skyblockId = SkyBlockItemId.item("PUMPKIN"),
        aliases = listOf(SkyBlockItemId.item("PUMPKIN_SEEDS")),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 3
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.125, 0.0),
                        matcher = {
                            it == "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f"
                        }
                    )
                ),
                1..1,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 5
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.28125, 0.0),
                        matcher = {
                            it == "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f"
                        }
                    )
                ),
                2..2,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 7
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.0625, -0.46875, 0.0),
                        matcher = {
                            it == "1839c3565f36c9d6e52d55a1760b11c2060953143ffe4ffe9c8b606ee4e3648f"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.09375, -0.625, 0.09375),
                        matcher = {
                            it == "1839c3565f36c9d6e52d55a1760b11c2060953143ffe4ffe9c8b606ee4e3648f"
                        }
                    )
                ),
                11..11,
                allowRotation = true
            )


        ),
        isBaseCrop = true
    )
}