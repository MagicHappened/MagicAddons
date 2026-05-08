package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cactus : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cactus",
        skyblockId = SkyBlockItemId.item("CACTUS"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    )
                ),
                1..1,
                allowRotation = true
            ),

            CropStagePattern(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.78125, 0.0),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    )
                ),
                stageRange = 2..3,
                allowRotation = true,
                baseStageStandOffset = Vec3(0.0, 0.59375, 0.0)
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:cactus") &&
                                    it.getIntProperty("age") == 0
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.5, 0.0),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    )
                ),
                4..4,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:cactus") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        matcher = {
                            it.isBlock("minecraft:cactus") &&
                                    it.getIntProperty("age") == 0
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.3125, 1.0, 0.0),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    )
                ),
                6..6,
                allowRotation = true,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:cactus") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        matcher = {
                            it.isBlock("minecraft:cactus") &&
                                    it.getIntProperty("age") == 0
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.03125, 2.59375, -0.15625),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 1.0, 0.3125),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 1.5, 0.0),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    )
                ),
                7..7,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:cactus") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        matcher = {
                            it.isBlock("minecraft:cactus") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,3,0),
                        matcher = {
                            it.isBlock("minecraft:cactus") &&
                                    it.getIntProperty("age") == 0
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 1.0, 0.3125),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.03125, 2.0, -0.28125),
                        matcher = {
                            it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                        }
                    )
                ),
                8..8,
                allowRotation = true
            )
        ),
        requiredSoil = setOf(Blocks.SAND),
        needsWater = false,
        isBaseCrop = true
    )





}