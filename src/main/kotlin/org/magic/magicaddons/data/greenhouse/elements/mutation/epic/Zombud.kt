package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.netherwartState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Zombud : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Zombud",
        skyblockId = SkyBlockItemId.item("ZOMBUD"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.21875, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "29e27b7ee26e272ce53f19e169a593ea83cc120bfa3a209e1a8a279fcdf463e7"
                    )
                ),
                4..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.375, 0.0),
                            hashString = "29e27b7ee26e272ce53f19e169a593ea83cc120bfa3a209e1a8a279fcdf463e7",
                        )
                    )
                ,
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.28125, 0.0),
                            hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        )
                    )
                ,
                10..10
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.28125, 0.0),
                            hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        )
                    )
                ,
                11..11
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.28125, 0.0),
                        hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                    )
                ),
                12..12
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 0.40625, 0.0),
                            hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        ),
                        CropArmorStand(
                            offset = Vec3(0.0, -0.09375, 0.0),
                            hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        )
                    )
                ,
                13..13
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 0.0, 0.0),
                            hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        ),
                        CropArmorStand(
                            offset = Vec3(0.0, 0.40625, 0.0),
                            hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        )
                    )
                ,
                14..14
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 0.40625, 0.0),
                            hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d",
                        ),
                        CropArmorStand(
                            offset = Vec3(0.0, 0.125, 0.0),
                            hashString = "7a65b2ac222b9b875d7bd33d0fbe60c60434eb86fe16ce451e9f8c4d81cc6455",
                        )
                    )
                ,
                15..15
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.375, 0.0),
                        hashString = "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.21875, 0.0),
                        hashString = "b2c4994b7a1c45231b623b8245c117382b267c8856c57cffa2d808c241027a51"
                    )
                ),
                16..16,
                allowRotation = true
            )

        ),
        maxStage = 16,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )
}