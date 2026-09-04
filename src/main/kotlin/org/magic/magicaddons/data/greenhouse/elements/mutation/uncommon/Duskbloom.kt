package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Duskbloom : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Duskbloom",
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("DUSKBLOOM"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "7dca7951b36f5f749e883758b379c8008ca55f245987e4ef0c3788cf0c903d5" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            isSmall = false,
                            offset = Vec3(0.0, -0.65625, 0.0),
                            hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
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
                    offset = Vec3(0.0, -0.65625, 0.0),
                    hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02"
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
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
                        isSmall = false,
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
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.5625, 0.0),
                            hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
                            isSmall = false,
                        )
                    ),
                4..4
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
                        isSmall = false
                    )
                ),
                5..5
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
                        isSmall = false
                    )
                ),
                6..6
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02"
                    )
                ),
                7..7
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "7dca7951b36f5f749e883758b379c8008ca55f245987e4ef0c3788cf0c903d5"
                    )
                ),
                8..8
            )


        ),
        maxStage = 8,
        isMutation = true
    )
}