package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.FIVE_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.Puffercloud
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.Zombud
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Devourer {
    val definition = CropDefinition(
        name = "Devourer",
        tier = CropTier.Legendary,
        dropMultiplier = 19.0,
        effects = setOf(
            CropEffect.BonusDrops,
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("DEVOURER"),
        standPoses = mapOf(
            "d5dcd6e26e5ab3c3a60ccc824c05b0fd195f526961019d3249776e8d57399d27" to StandPose.Fixed(Rotations(0.0f, 45.0f, 0.0f), yRotation = 90.0f)
        ),
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
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "d5dcd6e26e5ab3c3a60ccc824c05b0fd195f526961019d3249776e8d57399d27",
                        isSmall = false
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
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "d5dcd6e26e5ab3c3a60ccc824c05b0fd195f526961019d3249776e8d57399d27",
                        isSmall = false
                    )
                ),
                2..2
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
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "d5dcd6e26e5ab3c3a60ccc824c05b0fd195f526961019d3249776e8d57399d27",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "d5dcd6e26e5ab3c3a60ccc824c05b0fd195f526961019d3249776e8d57399d27",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "d5dcd6e26e5ab3c3a60ccc824c05b0fd195f526961019d3249776e8d57399d27",
                        isSmall = false
                    )
                ),
                5..5
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.34375, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0",
                        isSmall = false
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.15625, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0",
                        isSmall = false
                    )
                ),
                9..9
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
                        offset = Vec3(0.0, -0.0625, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0",
                        isSmall = false
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.03125, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0",
                        isSmall = false
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.03125, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "4100d3b81c8dd0af22af3b42c97045bd844438d2f0297b0f267a46bd35ffb33f",
                        isSmall = false
                    )
                ),
                12..12
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
                        offset = Vec3(0.0, 0.15625, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "4100d3b81c8dd0af22af3b42c97045bd844438d2f0297b0f267a46bd35ffb33f",
                        isSmall = false
                    )
                ),
                13..13
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
                        offset = Vec3(0.0, 0.15625, 0.0),
                        headRotation = Rotations(0.0f, 67.5f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "4100d3b81c8dd0af22af3b42c97045bd844438d2f0297b0f267a46bd35ffb33f",
                        isSmall = false
                    )
                ),
                14..14
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.34375, 0.0),
                        headRotation = Rotations(-22.5f, 90.0f, 0.0f),
                        yRotation = 90.0f,
                        hashString = "4100d3b81c8dd0af22af3b42c97045bd844438d2f0297b0f267a46bd35ffb33f",
                        isSmall = false
                    )
                ),
                15..15
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.5625, 0.0),
                        headRotation = Rotations(112.5f, 0.0f, 0.0f),
                        hashString = "4100d3b81c8dd0af22af3b42c97045bd844438d2f0297b0f267a46bd35ffb33f",
                        isSmall = false
                    )
                ),
                16..16
            )

        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 16,
        isMutation = true,
        spawnRule = SpawnRule(weight = 20, requiredNeighbourCells = mapOf("Puffercloud" to 4, "Zombud" to 4))
    )
}