package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Melon {
    val definition = CropDefinition(
        name = "Melon",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.2,
        effects = setOf(
            CropEffect.WaterRetain
        ),
        skyblockId = SkyBlockItemId.item("MELON"),
        aliases = listOf(SkyBlockItemId.item("MELON_SEEDS")),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.125, 0.0),
                        hashString = "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4",
                        isSmall = true
                    )
                ),
                1..1,
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
                        offset = Vec3(0.0, 0.28125, 0.0),
                        hashString = "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4",
                        isSmall = true
                    )
                ),
                2..2,
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
                        isSmall = false,
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4"
                    )
                ),
                3..3,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.53125, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9"
                    )
                ),
                4..4,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                        isSmall = false
                    )
                ),
                5..5,
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
                        offset = Vec3(0.0, -0.53125, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.21875, 0.1875, 0.1875),
                        headRotation = Rotations(22.5f, 0.0f, 22.5f),
                        hashString = "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4",
                        isSmall = true
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.21875, 0.1875, 0.1875),
                        headRotation = Rotations(22.5f, 0.0f, 22.5f),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                        isSmall = false
                    )
                ),
                7..7,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "192600cad8dbec5b6a6ec4dcf9bb4e9cd76190cad80aeee8b047de719cf5e36d",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.21875, 0.1875, 0.1875),
                        headRotation = Rotations(22.5f, 0.0f, 22.5f),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                        isSmall = true,
                    )
                ),
                8..8,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(-0.0625, -0.46875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "192600cad8dbec5b6a6ec4dcf9bb4e9cd76190cad80aeee8b047de719cf5e36d",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.09375, -0.625, 0.09375),
                        headRotation = Rotations(22.5f, 0.0f, 22.5f),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                        isSmall = false
                    )
                ),
                9..9,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(-0.0625, -0.46875, 0.0),
                        Vec3(0.09375, -0.625, 0.09375)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, -22.5f),
                        Rotations(22.5f, 0.0f, 22.5f)
                    ),
                    hashString = "192600cad8dbec5b6a6ec4dcf9bb4e9cd76190cad80aeee8b047de719cf5e36d",
                    isSmall = false
                ),
                10..10,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(-0.0625, -0.46875, 0.0),
                        Vec3(0.09375, -0.625, 0.09375)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, -22.5f),
                        Rotations(22.5f, 0.0f, 22.5f)
                    ),
                    hashString = "fdfae4b11048bc1ce96ed150134e79f16e2bcaf12d43fa0ff0e27fb2e0852130",
                    isSmall = false
                ),
                11..11,
            )
        ),
        maxStage = 11,
        isBaseCrop = true

    )
}