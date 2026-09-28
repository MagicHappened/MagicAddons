package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Moonflower
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Sunflower
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Dustgrain
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Shadevine
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Duskbloom {
    val definition = CropDefinition(
        name = "Duskbloom",
        tier = CropTier.Uncommon,
        dropMultiplier = 2.7,
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("DUSKBLOOM"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    listOf(
                        StageStand(
                            isSmall = false,
                            offset = Vec3(0.0, -0.65625, 0.0),
                            hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02"
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
                    hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
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
                        hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
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
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.5625, 0.0),
                            hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
                            isSmall = false
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
                        isSmall = false
                    )
                ),
                5..5
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
                        hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02",
                        isSmall = false
                    )
                ),
                6..6
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02"
                    )
                ),
                7..7
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "7dca7951b36f5f749e883758b379c8008ca55f245987e4ef0c3788cf0c903d5"
                    )
                ),
                8..8
            )
        ),
        maxStage = 8,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Moonflower" to 2, "Shadevine" to 2, "Sunflower" to 2, "Dustgrain" to 2))
    )
}