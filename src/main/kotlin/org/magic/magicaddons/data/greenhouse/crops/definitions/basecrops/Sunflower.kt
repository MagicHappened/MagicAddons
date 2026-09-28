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

object Sunflower {
    val definition = CropDefinition(
        name = "Sunflower",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.29,
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("DOUBLE_PLANT"),
        standPoses = mapOf(
            "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71" to StandPose.Fixed(Rotations(15.0f, 0.0f, 0.0f)),
            "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4" to StandPose.Fixed(Rotations(-45.0f, 0.0f, 0.0f)),
            "f2c4a75b5b6478087b6565edf7643c2b868a5e3eccec1250cdfaa371adfc0754" to StandPose.Fixed(Rotations(-22.5f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.25, 0.1875),
                        hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4",
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
                        offset = Vec3(0.0, -0.5625, 0.1875),
                        hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4",
                        isSmall = false
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
                        offset = Vec3(0.0, -0.25, 0.1875),
                        hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4",
                        isSmall = false
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
                            hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4",
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
                        hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4",
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
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71",
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
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.1875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71",
                        isSmall = false
                    )
                ),
                7..8,
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
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
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
                        offset = Vec3(0.0, 0.25, 0.1875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71",
                        isSmall = false
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.4375, 0.1875),
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
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
                        hashString = "f942c5e8426609a132549b7df5300739fade9122dad08a1d0265347795cf51ad",
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
                        hashString = "f942c5e8426609a132549b7df5300739fade9122dad08a1d0265347795cf51ad",
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
                        hashString = "f2c4a75b5b6478087b6565edf7643c2b868a5e3eccec1250cdfaa371adfc0754"
                    )
                ),
                15..15,
            )
        ),
        maxStage = 15,
        isBaseCrop = true

    )
}