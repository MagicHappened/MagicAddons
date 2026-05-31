package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Snoozling : CropDefinitionProvider {
    private val wheatPositions = listOf(
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 2),
        BlockPos(2, 1, 0),
        BlockPos(2, 1, 2),
    )

    sealed interface SnoozlingInfo : CropExtraInfo {
        data object Sleeping : SnoozlingInfo
    }

    override val definition = CropDefinition(
        name = "Snoozling",
        skyblockId = SkyBlockItemId.item("SNOOZLING"),
        stageDefs = listOf(  //todo check if this fails and subtract .5 from x and z
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 0
                    }),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.5625, -0.0625, 2.03125),
                            hashMatches = {
                                it == "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.5, -0.46875, 1.53125),
                                    Vec3(2.0, 0.25, 1.3125),
                                    Vec3(1.0, 0.25, 1.28125),
                                    Vec3(1.5, 0.1875, 1.09375)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                4..4,
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 0
                    }),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(1.5625, -0.0625, 2.03125),
                        hashMatches = {
                            it == "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                        }
                    )) +
                        CropArmorStand.matcherPattern(
                            listOf(
                                Vec3(1.5, -0.46875, 1.53125),
                                Vec3(0.96875, 0.25, 1.34375),
                                Vec3(2.0, 0.25, 1.34375),
                                Vec3(1.5, 0.1875, 1.09375)
                            ),
                            hashMatches = {
                                it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                            }
                        ),
                5..5,
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 0
                    }),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.5625, -0.0625, 2.03125),
                            hashMatches = {
                                it == "2c856bec39e5f5fc04fc4c7d90f7d404cee2c628d911c7a756ef5b72f2b876f4"
                            }
                        ),
                        CropArmorStand(
                            offset = Vec3(1.5, 2.042, 1.5),
                            customNameMatches = {
                                it?.isNotBlank() ?: false //zzz stand.
                            }
                        ),
                        CropArmorStand(
                            offset = Vec3(1.5, 1.672, 1.5),
                            customNameMatches = {
                                it == "Right-click to wake up!"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(2.0, 0.25, 1.34375),
                                    Vec3(0.96875, 0.25, 1.34375),
                                    Vec3(1.5, 0.1875, 1.09375),
                                    Vec3(1.5, -0.46875, 1.53125)
                                ),
                                hashMatches = {
                                    it == "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215"
                                }
                            ),
                5..5,
                allowRotation = true,
                extraInfo = SnoozlingInfo.Sleeping
            ),
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 0
                    }),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.5625, -0.0625, 2.03125),
                            hashMatches = {
                                it == "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.03125, 0.25, 1.34375),
                                    Vec3(2.0, 0.25, 1.34375),
                                    Vec3(1.5, -0.46875, 1.53125),
                                    Vec3(1.5, 0.1875, 1.09375)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                6..6,
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 2
                    }),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.5625, -0.0625, 2.03125),
                            hashMatches = {
                                it == "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.03125, 0.25, 1.34375),
                                    Vec3(2.0, 0.25, 1.34375),
                                    Vec3(1.5, -0.46875, 1.53125),
                                    Vec3(1.5, 0.1875, 1.09375)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                7..7, //difference between 6 and 7 only block age....
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 3
                    }),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.5, 0.0, 2.34375),
                            hashMatches = {
                                it == "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.9375, -0.84375, 1.125),
                                    Vec3(1.5, 0.4375, 1.90625),
                                    Vec3(1.5, -0.5625, 1.5),
                                    Vec3(1.5, 0.15625, 1.09375),
                                    Vec3(1.03125, -0.78125, 1.125)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                10..10,
                allowRotation = true //todo check if its different when sleeping.
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,1,2),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(2,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 0
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(2,1,2),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 0
                        }
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.5, 0.0, 2.34375),
                            hashMatches = {
                                it == "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.5, -0.5625, 1.5),
                                    Vec3(1.5, 0.4375, 1.90625),
                                    Vec3(1.9375, -0.84375, 1.125),
                                    Vec3(1.03125, -0.78125, 1.125),
                                    Vec3(1.5, 0.15625, 1.09375)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                11..11,
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 4
                    }),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.5, 0.0, 2.34375),
                            hashMatches = {
                                it == "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.5, -0.65625, 0.875),
                                    Vec3(1.03125, -0.59375, 1.125),
                                    Vec3(1.5, -0.5625, 1.5),
                                    Vec3(2.0, -0.65625, 1.125),
                                    Vec3(1.5, 0.4375, 1.90625)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                14..14,
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 4
                    }),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.5, -0.125, 2.34375),
                            hashMatches = {
                                it == "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.5, 0.4375, 1.90625),
                                    Vec3(2.0, -0.84375, 1.125),
                                    Vec3(1.03125, -0.78125, 1.125),
                                    Vec3(1.5, -0.75, 1.5),
                                    Vec3(1.5, -0.65625, 0.875)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                15..15,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 1, 2),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(2, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(2, 1, 2),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(1.53125, -0.1875, 2.15625),
                            hashMatches = {
                                it == "2c856bec39e5f5fc04fc4c7d90f7d404cee2c628d911c7a756ef5b72f2b876f4"
                            }
                        ),
                        CropArmorStand(
                            offset = Vec3(1.5, 2.042, 1.5),
                            customNameMatches = {
                                it?.isNotBlank() ?: false
                            }
                        ),
                        CropArmorStand(
                            offset = Vec3(1.5, 1.672, 1.5),
                            customNameMatches = {
                                it == "Right-click to wake up!"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.5, -0.75, 1.5),
                                    Vec3(1.03125, -0.78125, 1.125),
                                    Vec3(1.5, -0.65625, 0.875),
                                    Vec3(1.5, 0.4375, 1.90625)
                                ),
                                hashMatches = {
                                    it == "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215"
                                },
                            ),
                15..15,
                allowRotation = true,
                extraInfo = SnoozlingInfo.Sleeping
            ),
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 5
                    }),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.65625, 0.0, 1.5),
                            hashMatches = {
                                it == "24c64afa58bef69ff567b012a2b1638cf475c5bdb050d382308399ffa0b06a8d"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(1.09375, 0.4375, 1.5),
                                    Vec3(1.5, -0.5625, 1.5),
                                    Vec3(1.875, -0.65625, 2.0),
                                    Vec3(1.875, -0.59375, 1.03125),
                                    Vec3(2.125, -0.25, 1.5),
                                    Vec3(2.40625, 0.9375, 1.5)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                20..20,
                allowRotation = true
            )

        ),
        maxStage = 20,
        footprint = Footprint(3, 3),
        isMutation = true
    )
}

/*








sleeping hash^^

 */