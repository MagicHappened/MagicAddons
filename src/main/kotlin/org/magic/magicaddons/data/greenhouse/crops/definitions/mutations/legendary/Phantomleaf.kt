package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
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
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.Shellfruit
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Phantomleaf {
    val definition = CropDefinition(
        name = "Phantomleaf",
        tier = CropTier.Legendary,
        dropMultiplier = 5.0,
        effects = setOf(
            CropEffect.XpBoost,
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("PHANTOMLEAF"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3" to StandPose.Fixed(Rotations(20.0f, 0.0f, 0.0f)),
            "988eaca2c41056ed3fb34669548843c62bae0b406441ea9d224fd7bd2f73f86e" to StandPose.Fixed(Rotations(45.0f, 0.0f, 0.0f)),
            "66e4ba32a6e955f6f7787fbd6e7fe69d9466413fab75950c152785641002ca2f" to StandPose.Fixed(Rotations(67.5f, 0.0f, 0.0f))
        ),
        stages = listOf(
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
                        offset = Vec3(0.0, -0.59375, -0.28125),
                        hashString = "66e4ba32a6e955f6f7787fbd6e7fe69d9466413fab75950c152785641002ca2f"
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
                        isSmall = false,
                        offset = Vec3(0.0, -0.5, -0.28125),
                        hashString = "66e4ba32a6e955f6f7787fbd6e7fe69d9466413fab75950c152785641002ca2f"
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
                        offset = Vec3(0.0, -0.5, -0.28125),
                        headRotation = Rotations(67.5f, 0.0f, 0.0f),
                        hashString = "988eaca2c41056ed3fb34669548843c62bae0b406441ea9d224fd7bd2f73f86e"
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
                        offset = Vec3(0.0, -0.5, -0.28125),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        hashString = "988eaca2c41056ed3fb34669548843c62bae0b406441ea9d224fd7bd2f73f86e"
                    )
                ),
                4..4
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
                        offset = Vec3(0.0, -0.375, -0.28125),
                        hashString = "988eaca2c41056ed3fb34669548843c62bae0b406441ea9d224fd7bd2f73f86e"
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
                        isSmall = false,
                        offset = Vec3(0.0, -0.4375, -0.15625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "988eaca2c41056ed3fb34669548843c62bae0b406441ea9d224fd7bd2f73f86e"
                    )
                ),
                6..6
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
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.28125),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        hashString = "bf3d71c3fc8bfcfd3003b7d7fde62748c1701aaa8185268cc9ce963feda2f631"
                    )
                ),
                7..8
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
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.28125),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        hashString = "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3"
                    )
                ),
                9..9
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
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.125),
                        hashString = "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3"
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.125),
                        hashString = "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3"
                    )
                ),
                11..13
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.125),
                        hashString = "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3"
                    )
                ),
                14..14
            )

        ,
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.375, -0.125),
                        headRotation = Rotations(20.0f, 0.0f, 0.0f),
                        hashString = "e1c206ab05cbac4a22f54f8778c3bfbd870f19c380176f288614c77d19b5d122",
                        isSmall = false
                    )
                ),
                15..15
            )),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 15,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 20, requiredNeighbourCells = mapOf("Chorus Fruit" to 4, "Shellfruit" to 4))
    )
}