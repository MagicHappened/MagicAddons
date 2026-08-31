package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.CropEffect
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.StandPose
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Pumpkin : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Pumpkin",
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("PUMPKIN"),
        aliases = listOf(SkyBlockItemId.item("PUMPKIN_SEEDS")),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.125, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f"
                    )
                ),
                1..1
            )
            ,
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.28125, 0.0),
                        hashString = "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f"
                    )
                ),
                2..2,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f"
                    )
                ),
                // a collected stage 3 came out byte-identical to what was recorded as stage 4, so
                // the two cannot be told apart by looking and the stage says so
                3..4,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f",
                    )
                ),
                5..5
            ) ,CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f",
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.1875, 0.1875, 0.21875),
                        hashString = "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f",
                    )
                ),
                7..7,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.21875, 0.1875, 0.1875),
                            hashString = "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f",
                        ),
                        CropArmorStand(
                            offset = Vec3(0.0, -0.53125, 0.0),
                            hashString = "a9d2abe3c6d6400a20b47179bbe9be278ed336c07fcc1e03ab0eb0c470d620c",
                        )
                    ),
                8..8,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.09375, -0.625, 0.09375),
                        headRotation = Rotations(22.5f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "18bd4aa55673e90a3c611117277d94f6ce185b5d13d2a862a3376f50a6139c4f"
                    ),
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(-0.0625, -0.46875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "a9d2abe3c6d6400a20b47179bbe9be278ed336c07fcc1e03ab0eb0c470d620c"
                    )
                ),
                9..9,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.09375, -0.625, 0.09375),
                        Vec3(-0.0625, -0.46875, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 0.0f, 22.5f),
                        Rotations(0.0f, 0.0f, -22.5f)
                    ),
                    hashString = "a9d2abe3c6d6400a20b47179bbe9be278ed336c07fcc1e03ab0eb0c470d620c",
                    isSmall = false
                ),
                10..10,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(-0.0625, -0.46875, 0.0),
                        Vec3(0.09375, -0.625, 0.09375)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, -22.5f),
                        Rotations(22.5f, 0.0f, 22.5f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        0.0f
                    ),
                    hashString = "1839c3565f36c9d6e52d55a1760b11c2060953143ffe4ffe9c8b606ee4e3648f",
                    isSmall = false
                ),
                11..11,
            )




        ),
        maxStage = 11,
        isBaseCrop = true
    )
}