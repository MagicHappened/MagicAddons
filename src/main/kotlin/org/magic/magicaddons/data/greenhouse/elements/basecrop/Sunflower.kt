package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.StandPose
import org.magic.magicaddons.data.greenhouse.CropEffect
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.sunflowerState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Sunflower : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Sunflower",
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("DOUBLE_PLANT"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71" to StandPose.Fixed(Rotations(15.0f, 0.0f, 0.0f)),
            "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4" to StandPose.Fixed(Rotations(-45.0f, 0.0f, 0.0f)),
            "f2c4a75b5b6478087b6565edf7643c2b868a5e3eccec1250cdfaa371adfc0754" to StandPose.Fixed(Rotations(-22.5f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.25, 0.1875),
                        hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
                    )
                ),
                1..1,
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        // re-recorded normalized; the old offset was this one turned a step
                        offset = Vec3(0.0, -0.5625, 0.1875),
                        hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4",
                        isSmall = false
                    )
                ),
                2..2,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, 0.1875),
                        hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
                    )
                ),
                3..3,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.15625, 0.1875),
                            hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4",
                        )
                    )
                ,
                4..4,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.03125, 0.1875),
                        headRotation = Rotations(-10.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.1875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.1875),
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
                    )
                ),
                7..8,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.15625, 0.1875),
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
                    )
                ),
                9..9,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.1875),
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
                    )
                ),
                10..10,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.4375, 0.1875),
                        hashString = "8082ca3aa210204d1daa8a3b737f594e102daf3c87b776530d49ba79b9b22e71"
                    )
                ),
                12..12,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = sunflowerState(DoubleBlockHalf.LOWER)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.1875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "f942c5e8426609a132549b7df5300739fade9122dad08a1d0265347795cf51ad",
                        isSmall = false
                    )
                ),
                13..13
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
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