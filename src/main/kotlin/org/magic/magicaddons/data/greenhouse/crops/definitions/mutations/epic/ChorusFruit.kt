package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic

import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.FIVE_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Chloronite
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object ChorusFruit {
    val definition = CropDefinition(
        name = "Chorus Fruit",
        tier = CropTier.Epic,
        dropMultiplier = 3.5,
        effects = setOf(
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("CHORUS_FRUIT"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.21875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "e044238503102f64f30f4d58f0843b215d8dd54fc9f1f70cc17d8dd891bb3e5e",
                        isSmall = true
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.4375, 0.0),
                            hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                            isSmall = false
                        )
                    ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.21875, 0.0),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.125, 0.0),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    StageStand.atOffsets(
                        offsets = listOf(
                            Vec3(0.0, 0.0, 0.0),
                            Vec3(0.0, -0.5625, 0.0)
                        ),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    StageStand.atOffsets(
                        offsets = listOf(
                            Vec3(0.0, 0.09375, 0.0),
                            Vec3(0.0, -0.5, 0.0)
                        ),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    StageStand.atOffsets(
                        offsets = listOf(
                            Vec3(0.0, 0.1875, 0.0),
                            Vec3(0.0, -0.40625, 0.0)
                        ),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.1875, 1.3125, 0.21875),
                            headRotation = Rotations(45.0f, 0.0f, 45.0f),
                            hashString = "e044238503102f64f30f4d58f0843b215d8dd54fc9f1f70cc17d8dd891bb3e5e",
                            isSmall = true
                        )
                    )
                            +
                            StageStand.atOffsets(
                                offsets = listOf(
                                    Vec3(0.0, 0.125, 0.0),
                                    Vec3(0.0, -0.46875, 0.0)
                                ),
                                hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                                isSmall = false
                            ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    StageStand.atOffsets(
                        offsets = listOf(
                            Vec3(0.0, 0.125, 0.0),
                            Vec3(0.0, -0.46875, 0.0)
                        ),
                        hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                        isSmall = false
                    )
                            +
                            StageStand.atOffsets(
                                offsets = listOf(
                                    Vec3(0.1875, 1.3125, 0.21875),
                                    Vec3(-0.1875, 1.25, -0.1875)
                                ),
                                rotations = listOf(
                                    Rotations(45.0f, 0.0f, 45.0f),
                                    Rotations(-45.0f, 0.0f, -45.0f)
                                ),
                                hashString = "e044238503102f64f30f4d58f0843b215d8dd54fc9f1f70cc17d8dd891bb3e5e",
                                isSmall = true
                            ),
                11..11
            ),
            CropStage(
                blocks = listOf(),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0, -0.46875, 0.0),
                        Vec3(0.0, 0.125, 0.0)
                    ),

                    hashString = "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d",
                    isSmall = false
                ) + StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.1875, 0.53125, 0.21875),
                        Vec3(-0.09375, 0.25, -0.21875),
                        Vec3(0.0, 0.6875, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 0.0f, 45.0f),
                        Rotations(-45.0f, 0.0f, -45.0f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    hashString = "e044238503102f64f30f4d58f0843b215d8dd54fc9f1f70cc17d8dd891bb3e5e",
                    isSmall = false
                ),
                12..12
            )
        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 12,
        requiredSoil = setOf(Blocks.END_STONE),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Chloronite" to 5, "Magic Jellybean" to 3))
    )
}