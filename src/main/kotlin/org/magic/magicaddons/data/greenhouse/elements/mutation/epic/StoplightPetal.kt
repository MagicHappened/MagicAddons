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

object StoplightPetal : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Stoplight Petal",
        skyblockId = SkyBlockItemId.item("STOPLIGHT_PETAL"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 5
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 5
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashMatches = {
                            it == "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.84375, 0.0),
                        hashMatches = {
                            it == "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe"
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
                                    it.getIntProperty("age") == 5
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 5
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashMatches = {
                            it == "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.71875, 0.0),
                        hashMatches = {
                            it == "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashMatches = {
                            it == "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e"
                        }
                    )
                ),
                10..10,
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
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 5
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,3,0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 5
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashMatches = {
                            it == "4c2b797e7172a05169e313739908515864d6b372f9a5ecc772f81d9c4e402a54"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.71875, 0.0),
                        hashMatches = {
                            it == "d6653a481cc301bcf694a70bfb5969485dc42f1e6803288d24d31b7261b61811"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashMatches = {
                            it == "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.03125, 2.125, 0.21875),
                        hashMatches = {
                            it == "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e"
                        }
                    )
                ),
                12..12,
                allowRotation = true
            )




        ),
        maxStage = 12,
        isMutation = true
    )
}