package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic

import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.ChargeRule
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.FIVE_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Noctilume
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Soggybud
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Thunderling {
    val definition = CropDefinition(
        name = "Thunderling",
        tier = CropTier.Epic,
        dropMultiplier = 11.0,
        effects = setOf(
            CropEffect.EffectSpread
        ),
        skyblockId = SkyBlockItemId.item("THUNDERLING"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "63650fc953438755b13b6d0b72e77e43d183cf8d911f8fe12ca4d66168308d46" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8" to StandPose.Fixed(Rotations(22.5f, 22.5f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        isSmall = true,
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "63650fc953438755b13b6d0b72e77e43d183cf8d911f8fe12ca4d66168308d46"
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 1..1
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8",
                        isSmall = true
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 2..2
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                    offset = Vec3(0.0, 0.1875, 0.0),
                    hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8",
                    isSmall = true
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 3..3
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, 0.0625, -0.25),
                        headRotation = Rotations(-22.5f, 0.0f, 22.5f),
                        hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8",
                        isSmall = true
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 4..4
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, 0.15625, -0.25),
                        headRotation = Rotations(-22.5f, 0.0f, 22.5f),
                        hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                        isSmall = true
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 5..5
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, -0.71875, -0.25),
                        headRotation = Rotations(-22.5f, 0.0f, 22.5f),
                        hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                        isSmall = false
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 6..6
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(-0.21875, 0.09375, 0.03125),
                        hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0625, -0.71875, -0.25),
                        headRotation = Rotations(-22.5f, 0.0f, 22.5f),
                        hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                        isSmall = false
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 7..7
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0625, -0.71875, -0.25),
                        headRotation = Rotations(-22.5f, 0.0f, 22.5f),
                        hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08"
                    ),
                    StageStand(
                        offset = Vec3(-0.21875, 0.1875, 0.03125),
                        hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8",
                        isSmall = true
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 8..8
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(-0.21875, -0.625, 0.03125),
                        Vec3(0.0625, -0.71875, -0.25)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 22.5f, 0.0f),
                        Rotations(-22.5f, 0.0f, 22.5f)
                    ),
                    hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                    isSmall = false
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 9..9
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0625, -0.71875, -0.25),
                        Vec3(-0.21875, -0.625, 0.03125)
                    ),
                    rotations = listOf(
                        Rotations(-22.5f, 0.0f, 22.5f),
                        Rotations(22.5f, 22.5f, 0.0f)
                    ),
                    hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        offset = Vec3(0.15625, 0.09375, 0.21875),
                        headRotation = Rotations(22.5f, 22.5f, 22.5f),
                        hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8",
                        isSmall = true
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 10..10
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(-0.21875, -0.625, 0.03125),
                        Vec3(0.0625, -0.71875, -0.25)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 22.5f, 0.0f),
                        Rotations(-22.5f, 0.0f, 22.5f)
                    ),
                    hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        offset = Vec3(0.15625, 0.25, 0.21875),
                        headRotation = Rotations(22.5f, 22.5f, 22.5f),
                        hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8",
                        isSmall = true
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 11..11
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(-0.21875, -0.625, 0.03125),
                        Vec3(0.0625, -0.71875, -0.25),
                        Vec3(0.15625, -0.625, 0.21875)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 22.5f, 0.0f),
                        Rotations(-22.5f, 0.0f, 22.5f),
                        Rotations(22.5f, 22.5f, 22.5f)
                    ),
                    hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                    isSmall = false
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 12..12
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.15625, -0.625, 0.21875),
                        Vec3(0.0625, -0.625, -0.25)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 22.5f, 22.5f),
                        Rotations(-22.5f, 0.0f, 22.5f)
                    ),
                    hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        offset = Vec3(-0.21875, -0.625, 0.03125),
                        headRotation = Rotations(22.5f, 22.5f, 0.0f),
                        hashString = "ec3d7c0e165b00491d3ef787ee14cb0c7d8dd6a29f535002ef6f15d67182bfce",
                        isSmall = false
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 13..13
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(-0.21875, -0.5, 0.03125),
                        Vec3(0.0625, -0.625, -0.25)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 22.5f, 0.0f),
                        Rotations(-22.5f, 0.0f, 22.5f)
                    ),
                    hashString = "ec3d7c0e165b00491d3ef787ee14cb0c7d8dd6a29f535002ef6f15d67182bfce",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        offset = Vec3(0.15625, -0.625, 0.21875),
                        headRotation = Rotations(22.5f, 22.5f, 22.5f),
                        hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08",
                        isSmall = false
                    )
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 14..14
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0625, -0.625, -0.25),
                        Vec3(-0.21875, -0.5, 0.03125),
                        Vec3(0.15625, -0.5, 0.21875)
                    ),
                    rotations = listOf(
                        Rotations(-22.5f, 0.0f, 22.5f),
                        Rotations(22.5f, 22.5f, 0.0f),
                        Rotations(22.5f, 22.5f, 22.5f)
                    ),
                    hashString = "ec3d7c0e165b00491d3ef787ee14cb0c7d8dd6a29f535002ef6f15d67182bfce",
                    isSmall = false
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 15..15
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.15625, -0.5, 0.21875),
                        Vec3(0.0625, -0.625, -0.25),
                        Vec3(-0.21875, -0.5, 0.03125)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 22.5f, 22.5f),
                        Rotations(-22.5f, 0.0f, 22.5f),
                        Rotations(22.5f, 22.5f, 0.0f)
                    ),
                    hashString = "3724327576a20876fc95f41bb37fd0e2f2c79014455f19262f185ce88b155385",
                    isSmall = false
                ),
                readers = listOf(StandReader.nonWaterBar(StandReader.CHARGE)),
                stageRange = 16..16
            )
        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 16,
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Soggybud" to 5, "Noctilume" to 3)),
        chargeRule = ChargeRule(perStage = 2000, limit = 16000)
    )
}