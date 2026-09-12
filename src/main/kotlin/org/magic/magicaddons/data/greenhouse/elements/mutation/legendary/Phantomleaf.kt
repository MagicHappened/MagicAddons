package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.StandPose
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Phantomleaf : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Phantomleaf",
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
        stageDefs = listOf(
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
                        offset = Vec3(0.0, -0.59375, -0.28125),
                        hashString = "66e4ba32a6e955f6f7787fbd6e7fe69d9466413fab75950c152785641002ca2f"
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
                        offset = Vec3(0.0, -0.5, -0.28125),
                        hashString = "66e4ba32a6e955f6f7787fbd6e7fe69d9466413fab75950c152785641002ca2f"
                    )
                ),
                2..2
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
                        offset = Vec3(0.0, -0.375, -0.28125),
                        hashString = "988eaca2c41056ed3fb34669548843c62bae0b406441ea9d224fd7bd2f73f86e"
                    )
                ),
                5..5,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.125),
                        hashString = "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3"
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.125),
                        hashString = "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3"
                    )
                ),
                11..12
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.125),
                        hashString = "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3"
                    )
                ),
                13..13
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
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
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.375, -0.125),
                        headRotation = Rotations(20.0f, 0.0f, 0.0f),
                        hashString = "e1c206ab05cbac4a22f54f8778c3bfbd870f19c380176f288614c77d19b5d122",
                        isSmall = false
                    )
                ),
                15..15
            )),
        //five days decay time
        maxStage = 15,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true,
        // a placed one is the grown look of the stage it arrives at, so the stage above serves both
        placedSameAsGrown = true
    )
}