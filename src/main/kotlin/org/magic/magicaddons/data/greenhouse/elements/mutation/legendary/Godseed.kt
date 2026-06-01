package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Godseed : CropDefinitionProvider {
    val surroundWheatPositions = listOf(
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(0, 1, 2),
        BlockPos(1, 1, 0),
        BlockPos(1, 1, 2),
        BlockPos(2, 1, 0),
        BlockPos(2, 1, 1),
        BlockPos(2, 1, 2)
    )
    override val definition = CropDefinition(
        name = "Godseed",
        skyblockId = SkyBlockItemId.item("GODSEED"),
        footprint = Footprint(3, 3),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 7
                        }
                    )
                )
                        + CropBlockState.matcherPattern(
                    positions = surroundWheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 6
                    }
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 0.34375, 0.0),
                            hashMatches = {
                                it == "9bc7d71431dcdcfa432e8ef9fdb6aa4c4683786ac657e7ece038fb94f71e42be"
                            },
                        )
                    )
                            +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(-1.0, -0.5625, -1.0),
                                    Vec3(0.0, -0.15625, -1.0),
                                    Vec3(-1.0, -0.15625, 0.0),
                                    Vec3(0.0, -0.15625, 1.0),
                                    Vec3(-1.0, 0.5625, -1.0),
                                    Vec3(-1.0, 0.5625, 1.0),
                                    Vec3(-1.0, -0.5625, 1.0),
                                    Vec3(1.0, 0.53125, 1.0),
                                    Vec3(1.0, 0.5625, -1.0),
                                    Vec3(1.0, -0.5625, -1.0),
                                    Vec3(1.0, -0.15625, 0.0),
                                    Vec3(1.0, -0.5625, 1.0)
                                ),
                                hashMatches = {
                                    it == "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887"
                                }
                            )
                ,
                32..32
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 7
                        }
                    )
                )
                        + CropBlockState.matcherPattern(
                    positions = surroundWheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 6
                    }
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 0.4375, 0.0),
                            hashMatches = {
                                it == "9bc7d71431dcdcfa432e8ef9fdb6aa4c4683786ac657e7ece038fb94f71e42be"
                            },
                        )
                    )
                            +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(-1.0, -0.0625, 0.0),
                                    Vec3(-1.0, 0.65625, 1.0),
                                    Vec3(0.0, -0.0625, 1.0),
                                    Vec3(-1.0, -0.4375, 1.0),
                                    Vec3(1.0, -0.0625, 0.0),
                                    Vec3(1.0, -0.4375, 1.0),
                                    Vec3(1.0, 0.65625, 1.0),
                                    Vec3(-1.0, -0.4375, -1.0),
                                    Vec3(-1.0, 0.65625, -1.0),
                                    Vec3(0.0, -0.0625, -1.0),
                                    Vec3(1.0, -0.4375, -1.0),
                                    Vec3(1.0, 0.65625, -1.0)
                                ),
                                hashMatches = {
                                    it == "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887"
                                }
                            ),
                37..37
            )

        ),
        maxStage = 40,
        isMutation = true
    )
}