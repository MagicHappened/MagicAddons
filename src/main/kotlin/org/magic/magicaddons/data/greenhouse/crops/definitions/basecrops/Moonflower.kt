package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.sunflowerState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Moonflower {
    val definition = CropDefinition(
        name = "Moonflower",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.29,
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("MOONFLOWER"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9" to StandPose.Fixed(Rotations(-45.0f, 0.0f, 0.0f)),
            "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb" to StandPose.Fixed(Rotations(15.0f, 0.0f, 0.0f)),
            "bd1001840c85349f87c6e20478317f4026b588514d8b1e78241a849d93f9cd94" to StandPose.Fixed(Rotations(-22.5f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.25, 0.1875),
                        hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9",
                        isSmall = true
                    )
                ),
                1..1,
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5625, 0.1875),
                        hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                    )
                ),
                2..2,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.25, 0.1875),
                        hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                    )
                ),
                3..3,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.15625, 0.1875),
                            headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                            hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9",
                            isSmall = false
                        )
                    )
                ,
                4..4,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.03125, 0.1875),
                        headRotation = Rotations(-10.0f, 0.0f, 0.0f),
                        hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.1875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            isSmall = false,
                            offset = Vec3(0.0, 0.0625, 0.1875),
                            headRotation = Rotations(0.0f, 0.0f, 0.0f),
                            hashString = "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb",
                        )
                    )
                ,
                7..7,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.1875),
                        headRotation = Rotations(15.0f, 0.0f, 0.0f),
                        hashString = "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb",
                        isSmall = false
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.15625, 0.1875),
                        hashString = "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb"
                    )
                ),
                9..10,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.25, 0.1875),
                        hashString = "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb"
                    )
                ),
                11..11,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.4375, 0.1875),
                        hashString = "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb"
                    )
                ),
                12..12,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = sunflowerState(DoubleBlockHalf.LOWER)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.1875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "24f985b13feff57c62440a1b88fc093da03966878a48a513bce57ee9d5004980",
                        isSmall = false
                    )
                ),
                13..13
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = sunflowerState(DoubleBlockHalf.LOWER)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.5625, 0.1875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "24f985b13feff57c62440a1b88fc093da03966878a48a513bce57ee9d5004980",
                        isSmall = false
                    )
                ),
                14..14
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.5625, 0.1875),
                        hashString = "bd1001840c85349f87c6e20478317f4026b588514d8b1e78241a849d93f9cd94"
                    )
                ),
                15..15,
            )
        ),
        maxStage = 15,
        isBaseCrop = true


    )
}