package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Startlevine : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Startlevine",
        skyblockId = SkyBlockItemId.item("STARTLEVINE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashMatches = {
                            it == "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a"
                        }
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 0
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashMatches = {
                            it == "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a"
                        }
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 1
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashMatches = {
                            it == "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a"
                        }
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 1
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashMatches = {
                            it == "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a"
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
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashMatches = {
                            it == "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a"
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
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashMatches = {
                            it == "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a"
                        },
                    )
                ),
                6..6
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
                        hashMatches = {
                            it == "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a"
                        },
                    )
                ),
                7..7
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
                            offset = Vec3(0.0, -0.4375, 0.0),
                            hashMatches = {
                                it == "98bef15a64354093d26b8f002e476b8012ed3ad9b061796953b6b1dad447d7"
                            },
                        )
                    )
                ,
                12..12
            )




        ),
        maxStage = 12,
        isMutation = true
    )
}