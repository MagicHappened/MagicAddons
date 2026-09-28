package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.roseBushState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.shortGrassState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Wildrose {
    val definition = CropDefinition(
        name = "Wild Rose",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.25,
        effects = setOf(
            CropEffect.EffectSpread
        ),
        skyblockId = SkyBlockItemId.item("WILD_ROSE"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece",
                        isSmall = true
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece",
                        isSmall = true
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.71875, 0.0),
                        hashString = "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "f341905af17c74a1c6181a56c88d8f91853f2cff0a9a33aaa16c0d835fdceece"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "816176a32c70d53e5aaade1f16e7d4ab6f5750e37d55b3e9e99977cbd5fa9f19",
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "816176a32c70d53e5aaade1f16e7d4ab6f5750e37d55b3e9e99977cbd5fa9f19"
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.34375, 0.0),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = shortGrassState()
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
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
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = shortGrassState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.09375, 0.0),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = roseBushState(DoubleBlockHalf.LOWER)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.1875, 0.0),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                10..10,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = roseBushState(DoubleBlockHalf.LOWER)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.375, 0.0),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = roseBushState(DoubleBlockHalf.LOWER)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.5, 0.0),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                12..13
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = roseBushState(DoubleBlockHalf.LOWER)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = roseBushState(DoubleBlockHalf.UPPER)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.78125, 0.0),
                        hashString = "4a99a01317a01f65f7a7610122bea792c22a771e7a48ce1a5b352bccc8335074"
                    )
                ),
                14..14
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = roseBushState(DoubleBlockHalf.LOWER)
                    ),
                    StageBlock(
                        offset = BlockPos(0,2,0),
                        blockState = roseBushState(DoubleBlockHalf.UPPER)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                        isSmall = false,
                            offset = Vec3(0.0, 1.09375, 0.0),
                            hashString = "61a37adb8bcad712663771235feeb136ebe0f5c4d593070a5410ccb6f6706aa0",
                        )
                    )
                ,
                15..15,
            )



        ),
        maxStage = 15,
        isBaseCrop = true
    )
}