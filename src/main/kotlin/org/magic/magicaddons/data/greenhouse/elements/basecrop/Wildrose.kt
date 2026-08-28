package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.roseBushState
import org.magic.magicaddons.data.greenhouse.CropStates.shortGrassState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Wildrose : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Wild Rose",
        skyblockId = SkyBlockItemId.item("WILD_ROSE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.71875, 0.0),
                        hashString = "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "816176a32c70d53e5aaade1f16e7d4ab6f5750e37d55b3e9e99977cbd5fa9f19",
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "816176a32c70d53e5aaade1f16e7d4ab6f5750e37d55b3e9e99977cbd5fa9f19"
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.34375, 0.0),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = shortGrassState()
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                        isSmall = false,
                            offset = Vec3(0.0, -0.125, 0.0),
                            hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074",
                        )
                    )
                ,
                8..8
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = shortGrassState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.09375, 0.0),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = roseBushState(DoubleBlockHalf.LOWER)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.5, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                12..13
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = roseBushState(DoubleBlockHalf.LOWER)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = roseBushState(DoubleBlockHalf.UPPER)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.78125, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                14..14
            )
            ,
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = roseBushState(DoubleBlockHalf.LOWER)
                    ),
                    CropBlockState(
                        offset = BlockPos(0,2,0),
                        blockState = roseBushState(DoubleBlockHalf.UPPER)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                        isSmall = false,
                            offset = Vec3(0.0, 1.09375, 0.0),
                            headRotation = Rotations(0.0f, 0.0f, 0.0f),
                            hashString = "61a37adb8bcad712663771235feeb136ebe0f5c4d593070a5410ccb6f6706aa0",
                        )
                    )
                ,
                15..15,
                allowRotation = true
            )



        ),
        maxStage = 15,
        decayTimeMs = DECAY_TIME_MS,
        isBaseCrop = true
    )
}