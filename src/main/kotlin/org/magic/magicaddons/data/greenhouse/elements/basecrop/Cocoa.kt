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
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Cocoa : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cocoa",
        skyblockId = SkyBlockItemId.item("INK_SACK-3"),
        stageDefs = listOf(
            CropStage(
                blocks = null,
                armorStands = listOf(
                    CropArmorStand(
                        Vec3(0.0, 0.34375, 0.0),
                        matcher = {
                            it == "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912"
                        }
                    ),
                    CropArmorStand(
                        Vec3(0.0, 1.472, 0.0),
                        matcher = {
                            it == "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912"
                        }
                    ),
                    CropArmorStand(
                        Vec3(0.0, 1.842, 0.0),
                        matcher = {
                            it == "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912"
                        }
                    )
                ),
                stageRange = 1..1,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 2 //dont trust
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.34375, 0.0),
                        matcher = {
                            it == "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912"
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
                        offset = Vec3(0.0, -0.125, 0.0),
                        matcher = {
                            it == "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
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
                                    it.getIntProperty("age") == 7
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        matcher = {
                            it == "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.125, 0.5625, 0.0625),
                        matcher = {
                            it == "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
                        }
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
                                    it.getIntProperty("age") == 7
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        matcher = {
                            it == "44d72eed58354ce14bfc497138a13564070fb4653898aeb3e66c73082ae1f993"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0625, 0.65625, 0.125),
                        matcher = {
                            it == "44d72eed58354ce14bfc497138a13564070fb4653898aeb3e66c73082ae1f993"
                        }
                    )
                ),
                6..6,
                allowRotation = true
            )
        ),
        needsWater = false,
        isBaseCrop = true


    )
}