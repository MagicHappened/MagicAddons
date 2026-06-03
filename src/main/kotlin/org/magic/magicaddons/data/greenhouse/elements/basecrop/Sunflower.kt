package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Sunflower : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Sunflower",
        skyblockId = SkyBlockItemId.item("DOUBLE_PLANT"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, 0.25, 0.0),
                        hashMatches = {
                            it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
                        }
                    )
                ),
                1..1,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, -0.5625, 0.0),
                        hashMatches = {
                            it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
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
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, -0.25, 0.0),
                        hashMatches = {
                            it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
                        }
                    )
                ),
                3..3,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 3
                        }
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.15625, 0.1875),
                            hashMatches = {
                                it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
                            },
                        )
                    )
                ,
                4..4,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, 0.03125, 0.0),
                        hashMatches = {
                            it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
                        },
                    )
                ),
                5..5,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 6
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.1875),
                        hashMatches = {
                            it == "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
                        }
                    )
                ),
                7..8,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 6
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, 0.15625, 0.0),
                        hashMatches = {
                            it == "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
                        }
                    )
                ),
                9..9,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:sunflower")
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, 0.4375, 0.0),
                        hashMatches = {
                            it == "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
                        }
                    )
                ),
                12..12,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:sunflower")
                        }
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 0.5625, 0.1875),
                            headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                            hashMatches = {
                                it == "f2c4a75b5b6478087b6565edf7643c2b868a5e3eccec1250cdfaa371adfc0754"
                            },
                        )
                    ),
                15..15
            )






        ),
        maxStage = 15,
        isBaseCrop = true

    )
}