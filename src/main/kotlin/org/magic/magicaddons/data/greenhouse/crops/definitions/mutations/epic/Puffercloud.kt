package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
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
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Snoozling
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Puffercloud {
    val definition = CropDefinition(
        name = "Puffercloud",
        tier = CropTier.Epic,
        dropMultiplier = 6.0,
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("PUFFERCLOUD"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.75, 0.0),
                            hashString = "a9ceff4063b495dbe5c42885f5f81b022d5b37322255b71e30963c489f936985",
                            isSmall = false
                        )
                    )
                ,
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "a9ceff4063b495dbe5c42885f5f81b022d5b37322255b71e30963c489f936985",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "a9ceff4063b495dbe5c42885f5f81b022d5b37322255b71e30963c489f936985",
                        isSmall = false
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
                        offset = Vec3(0.0, -0.71875, 0.0),
                        hashString = "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04",
                        isSmall = false
                    )
                ),
                4..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04",
                        isSmall = false
                    )
                ),
                6..6
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
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "4c5d2d992b12548a4112cd533627ef76e193fec3f4452d367d654a4bb60f0a04",
                        isSmall = false
                    )
                ),
                7..7
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
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "2f9502a6895a90bbbb25921842fa6a213744ba967bbdfd861a44c92a79530aa0",
                        isSmall = false
                    )
                ),
                8..8
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
                        offset = Vec3(0.0, -0.59375, 0.0),
                        hashString = "2f9502a6895a90bbbb25921842fa6a213744ba967bbdfd861a44c92a79530aa0",
                        isSmall = false
                    )
                ),
                9..9
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
                        offset = Vec3(0.0, -0.59375, 0.0),
                        hashString = "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b",
                        isSmall = false
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.59375, 0.0),
                            hashString = "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b",
                            isSmall = false
                        )
                    )
                ,
                11..11
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.53125, 0.0),
                            hashString = "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b",
                            isSmall = false
                        )
                    )
                ,
                12..12
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
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "fb1e911b28a5bc539bba0e159256c415c6f74833cbee9c32fbcac4ca7b98a77b",
                        isSmall = false
                    )
                ),
                13..13
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "9a488340c3d9131b6e97bae6279aac852037367cb93a6b50c60a7d328aec173e",
                        isSmall = false
                    )
                ),
                14..14
            )
        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 14,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Snoozling" to 2, "Do-not-eat-shroom" to 6))
    )
}