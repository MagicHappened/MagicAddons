package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.Coalroot
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.Thornshade
import org.magic.magicaddons.util.compat.McCompat
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Chloronite {
    val definition = CropDefinition(
        name = "Chloronite",
        tier = CropTier.Rare,
        dropMultiplier = 2.7,
        effects = setOf(
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("CHLORONITE"),
        standPoses = mapOf(
            "3d9bcd3946c162aa361e537a455eddae3b55fb4bcf6208e84662b622b3ff6737" to StandPose.Fixed(Rotations(0.0f, 45.0f, 0.0f)),
            "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40" to StandPose.Fixed(Rotations(0.0f, 45.0f, 0.0f))
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
                        offset = Vec3(0.0, -0.84375, 0.0),
                        hashString = "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40",
                        isSmall = false
                    )
                ),
                1..1
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
                    offset = Vec3(0.0, -0.84375, 0.0),
                    hashString = "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40",
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
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40",
                        isSmall = false
                    )
                ),
                3..4
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
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40"
                    )
                ),
                5..6
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
                        offset = Vec3(0.0, -0.46875, 0.0),
                        hashString = "3d9bcd3946c162aa361e537a455eddae3b55fb4bcf6208e84662b622b3ff6737",
                        isSmall = false
                    )
                ),
                7..9
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2),
                        required = false
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.46875, 0.0),
                        hashString = "3d9bcd3946c162aa361e537a455eddae3b55fb4bcf6208e84662b622b3ff6737"
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = McCompat.greenStainedGlass().defaultBlockState(),
                        required = false
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.4, 0.0),
                        hashString = "98056b960ff385c20cffc3d1524500fcd3bf8c31b6dcafd8520f41dfa749dd28"
                    )
                ),
                10..10
            )

        ),
        maxStage = 10,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Coalroot" to 6, "Thornshade" to 2))
    )
}