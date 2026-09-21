package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.netherwartState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Ashwreath
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Witherbloom
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cindershade {
    val definition = CropDefinition(
        name = "Cindershade",
        tier = CropTier.Uncommon,
        dropMultiplier = 1.7,
        effects = setOf(
            CropEffect.EffectSpread,
            CropEffect.ImprovedHarvestBoost,
            CropEffect.XpLoss
        ),
        skyblockId = SkyBlockItemId.item("CINDERSHADE"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "a0646bc0558155207204711cf5d3d07920e0e98c9b2be0b6107becb409a97427" to StandPose.Fixed(Rotations(0.0f, 45.0f, 0.0f)),
            "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3" to StandPose.Fixed(Rotations(0.0f, 45.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3",
                        isSmall = true
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.71875, 0.0),
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.71875, 0.0),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c"
                    ),
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.03125, -0.15625, 0.03125),
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c"
                    ),
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.03125, -0.03125, 0.03125),
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        hashString = "a0646bc0558155207204711cf5d3d07920e0e98c9b2be0b6107becb409a97427",
                        isSmall = false
                    )
                ),
                8..8
            ),
            // the same last stage wearing younger wart: four plants read this way, each in a plot
            // turned its own way
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        hashString = "a0646bc0558155207204711cf5d3d07920e0e98c9b2be0b6107becb409a97427",
                        isSmall = false
                    )
                ),
                8..8
            )
        ),
        maxStage = 8,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Ashwreath" to 4, "Witherbloom" to 4))
    )
}