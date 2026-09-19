package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import org.magic.magicaddons.data.greenhouse.SpawnRule
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.TEN_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.data.greenhouse.StandPose
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Godseed : CropDefinitionProvider {
    private val surroundWheatPositions = listOf(
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(0, 1, 2),
        BlockPos(1, 1, 0),
        BlockPos(1, 1, 2),
        BlockPos(2, 1, 0),
        BlockPos(2, 1, 1),
        BlockPos(2, 1, 2)
    )
    override val definition = CropDefinition(
        name = "Godseed",
        dropMultiplier = 8.0,
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.ImprovedWaterRetain,
            CropEffect.ImprovedXpBoost,
            CropEffect.Immunity,
            CropEffect.BonusDrops,
            CropEffect.EffectSpread
        ),
        skyblockId = SkyBlockItemId.item("GODSEED"),
        footprint = Footprint(3, 3),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "9bc7d71431dcdcfa432e8ef9fdb6aa4c4683786ac657e7ece038fb94f71e42be" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "ab849bae7ab0927a52836da1a45768527d1c7be5853a9290a283ae9aca0c908b" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(0)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "ab849bae7ab0927a52836da1a45768527d1c7be5853a9290a283ae9aca0c908b",
                        isSmall = true
                    )
                ),
                1..4
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(2)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(-1.0, 0.5625, 1.0),
                        Vec3(-1.0, 0.5625, -1.0),
                        Vec3(1.0, 0.5625, 1.0),
                        Vec3(1.0, 0.5625, -1.0)
                    ),
                    rotations = List(4) { Rotations(180.0f, 0.0f, 0.0f) },
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = true
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        hashString = "ab849bae7ab0927a52836da1a45768527d1c7be5853a9290a283ae9aca0c908b",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(2)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(1.0, 0.5625, 1.0),
                        Vec3(1.0, 0.5625, -1.0),
                        Vec3(-1.0, 0.5625, 1.0),
                        Vec3(-1.0, 0.5625, -1.0)
                    ),
                    rotations = listOf(
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f)
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = true
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        hashString = "ab849bae7ab0927a52836da1a45768527d1c7be5853a9290a283ae9aca0c908b",
                        isSmall = true
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(2)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(-1.0, 0.0625, 1.0),
                        Vec3(-1.0, 0.0625, -1.0),
                        Vec3(1.0, 0.0625, 1.0),
                        Vec3(1.0, 0.0625, -1.0)
                    ),
                    rotations = listOf(
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f)
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = false
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.15625, 0.0),
                        hashString = "ab849bae7ab0927a52836da1a45768527d1c7be5853a9290a283ae9aca0c908b",
                        isSmall = false
                    )
                ),
                9..11
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(3)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(-1.0, 0.15625, 1.0),
                        Vec3(-1.0, 0.15625, -1.0),
                        Vec3(1.0, 0.15625, 1.0),
                        Vec3(1.0, 0.15625, -1.0)
                    ),
                    rotations = List(4) { Rotations(180.0f, 0.0f, 0.0f) },
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = false
                ) + CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0, 0.4375, 1.0),
                        Vec3(-1.0, 0.4375, 0.0),
                        Vec3(0.0, 0.4375, -1.0),
                        Vec3(1.0, 0.4375, 0.0)
                    ),
                    rotations = List(4) { Rotations(180.0f, 0.0f, 0.0f) },
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = true
                ) + listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.15625, 0.0),
                        hashString = "ab849bae7ab0927a52836da1a45768527d1c7be5853a9290a283ae9aca0c908b"
                    )
                ),
                13..15
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(4)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(-1.0, -0.75, -1.0),
                        Vec3(-1.0, 0.4375, -1.0),
                        Vec3(-1.0, -0.75, 1.0),
                        Vec3(-1.0, 0.4375, 1.0),
                        Vec3(1.0, -0.75, -1.0),
                        Vec3(1.0, 0.4375, -1.0),
                        Vec3(1.0, -0.75, 1.0),
                        Vec3(1.0, 0.4375, 1.0)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f)
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = false
                ) + CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0, 0.4375, -1.0),
                        Vec3(0.0, 0.4375, 1.0),
                        Vec3(-1.0, 0.4375, 0.0),
                        Vec3(1.0, 0.4375, 0.0)
                    ),
                    rotations = List(4) { Rotations(180.0f, 0.0f, 0.0f) },
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = true
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "a9d3e8e4001232e1c6ba43b5f39c6cb0a1ff2f4dfd5aceb14e4b287b91ba7c9a",
                        isSmall = false
                    )
                ),
                18..19
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(5)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(-1.0, -0.65625, -1.0),
                        Vec3(-1.0, 0.4375, -1.0),
                        Vec3(-1.0, -0.65625, 1.0),
                        Vec3(-1.0, 0.4375, 1.0),
                        Vec3(1.0, -0.65625, -1.0),
                        Vec3(1.0, 0.4375, -1.0),
                        Vec3(1.0, -0.65625, 1.0),
                        Vec3(1.0, 0.4375, 1.0)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f)
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = false
                ) + CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0, 0.5625, -1.0),
                        Vec3(0.0, 0.5625, 1.0),
                        Vec3(-1.0, 0.5625, 0.0),
                        Vec3(1.0, 0.5625, 0.0)
                    ),
                    rotations = List(4) { Rotations(180.0f, 0.0f, 0.0f) },
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = true
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.25, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "a9d3e8e4001232e1c6ba43b5f39c6cb0a1ff2f4dfd5aceb14e4b287b91ba7c9a",
                        isSmall = false
                    )
                ),
                22..24
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(6)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(-1.0, -0.5625, -1.0),
                        Vec3(-1.0, 0.5625, -1.0),
                        Vec3(-1.0, -0.5625, 1.0),
                        Vec3(-1.0, 0.5625, 1.0),
                        Vec3(1.0, -0.5625, -1.0),
                        Vec3(1.0, 0.5625, -1.0),
                        Vec3(1.0, -0.5625, 1.0),
                        // this one corner really does sit 0.03125 below its three mirrors, read twice
                        Vec3(1.0, 0.53125, 1.0)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f)
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = false
                ) + CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.0, -0.15625, -1.0),
                        Vec3(0.0, -0.15625, 1.0),
                        Vec3(-1.0, -0.15625, 0.0),
                        Vec3(1.0, -0.15625, 0.0)
                    ),
                    rotations = List(4) { Rotations(180.0f, 0.0f, 0.0f) },
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = false
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.34375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "9bc7d71431dcdcfa432e8ef9fdb6aa4c4683786ac657e7ece038fb94f71e42be",
                        isSmall = false
                    )
                ),
                30..30
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(6)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(1.0, 0.5625, -1.0),
                        Vec3(1.0, -0.5625, -1.0),
                        Vec3(0.0, -0.15625, -1.0),
                        Vec3(-1.0, 0.53125, -1.0),
                        Vec3(-1.0, -0.5625, -1.0),
                        Vec3(1.0, -0.5625, 1.0),
                        Vec3(1.0, -0.15625, 0.0),
                        Vec3(1.0, 0.5625, 1.0),
                        Vec3(0.0, -0.15625, 1.0),
                        Vec3(-1.0, 0.5625, 1.0),
                        Vec3(-1.0, -0.5625, 1.0),
                        Vec3(-1.0, -0.15625, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f)
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = true
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.34375, 0.0),
                        hashString = "9bc7d71431dcdcfa432e8ef9fdb6aa4c4683786ac657e7ece038fb94f71e42be",
                        isSmall = true
                    )
                ),
                32..32
            ),
            CropStage(
                blocks = CropBlockState.atPositions(
                    positions = surroundWheatPositions,
                    blockState = wheatState(6)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.atOffsets(
                    offsets = listOf(
                        Vec3(-1.0, -0.0625, 0.0),
                        Vec3(-1.0, -0.4375, -1.0),
                        Vec3(-1.0, 0.65625, -1.0),
                        Vec3(-1.0, 0.65625, 1.0),
                        Vec3(0.0, -0.0625, -1.0),
                        Vec3(0.0, -0.0625, 1.0),
                        Vec3(-1.0, -0.4375, 1.0),
                        Vec3(1.0, -0.4375, -1.0),
                        Vec3(1.0, -0.0625, 0.0),
                        Vec3(1.0, 0.65625, -1.0),
                        Vec3(1.0, -0.4375, 1.0),
                        Vec3(1.0, 0.65625, 1.0)
                    ),
                    rotations = listOf(
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 90.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f,
                        -180.0f
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887",
                    isSmall = true
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.0),
                        hashString = "9bc7d71431dcdcfa432e8ef9fdb6aa4c4683786ac657e7ece038fb94f71e42be",
                        isSmall = true
                    )
                ),
                34..37 // 34 35 and 37 same so assuming for 36
            )

        ),
        decayTimeMs = TEN_DAY_DECAY_TIME_MS,
        maxStage = 40,
        isMutation = true,
        spawnRule = SpawnRule(weight = 5, needsAllPositiveEffects = true)
    )
}