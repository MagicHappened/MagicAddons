package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.stateOf
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Ashwreath
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.Chocoberry
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Blastberry {
    val definition = CropDefinition(
        name = "Blastberry",
        tier = CropTier.Rare,
        dropMultiplier = 2.0,
        effects = setOf(
            CropEffect.Immunity,
            CropEffect.ImprovedHarvestBoost,
            CropEffect.XpLoss
        ),
        skyblockId = SkyBlockItemId.item("BLASTBERRY"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "bacae0e87ffeadb750260c2e6531004d69d14473376cb22577fafe70569349f3" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.71875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "a3b6c45e028b8b70befb6feb9d320dbda86c6f925961e23babbc7ff250918d0f",
                        isSmall = false
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "a3b6c45e028b8b70befb6feb9d320dbda86c6f925961e23babbc7ff250918d0f",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "ba199c78d912ac538f9c2b7994fe7a2918cfae95bcb82d6c4313ee0f2f7ed54d",
                        isSmall = false
                    )
                ),
                3..3
            ),
            // the run could not name the block at stage four, so the stand alone says the stage
            CropStage(
                blocks = null,
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "ba199c78d912ac538f9c2b7994fe7a2918cfae95bcb82d6c4313ee0f2f7ed54d",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = stateOf("minecraft:redstone_torch[lit=false]")
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "b09bffcb94d3b2bf641758f386b3fefa672afa0e342aa6f0c4eb1b7d6ec5b5f6",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = Blocks.REDSTONE_TORCH.defaultBlockState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "bacae0e87ffeadb750260c2e6531004d69d14473376cb22577fafe70569349f3",
                        isSmall = false
                    )
                ),
                6..6
            )

        ),
        maxStage = 6,
        requiredSoil = setOf(Blocks.SAND, Blocks.RED_SAND),
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Chocoberry" to 5, "Ashwreath" to 3))
    )
}