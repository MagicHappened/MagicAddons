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

object Cocoa : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cocoa Beans",
        skyblockId = SkyBlockItemId.item("INK_SACK-3"),
        stageDefs = listOf(
            CropStage(
                blocks = null,
                armorStands = listOf(
                    CropArmorStand(
                        Vec3(0.0, 0.34375, 0.0),
                        hashMatches = {
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
                        hashMatches = {
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
                        hashMatches = {
                            it == "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.125, 0.5625, 0.0625),
                        hashMatches = {
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
                armorStands =
                    CropArmorStand.matcherPattern(
                        listOf(
                            Vec3(0.0, 0.09375, 0.0),
                            Vec3(-0.0625, 0.65625, -0.125)
                        ),
                        listOf(
                            Rotations(0.0f, 22.5f, 22.5f),
                            Rotations(0.0f, -22.5f, -22.5f)
                        ),
                        hashMatches = {
                            it == "44d72eed58354ce14bfc497138a13564070fb4653898aeb3e66c73082ae1f993"
                        }
                    )
                ,
                6..6,
                allowRotation = true
            )

        ),
        maxStage = 6,
        needsWater = false,
        isBaseCrop = true


    )
}