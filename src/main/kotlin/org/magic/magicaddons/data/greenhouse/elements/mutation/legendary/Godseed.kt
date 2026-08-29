package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Godseed : CropDefinitionProvider {
    val surroundWheatPositions = listOf(
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
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    surroundWheatPositions,
                    blockState = wheatState(2)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = CropArmorStand.matcherPattern(
                    listOf(
                        Vec3(1.0, 0.5625, 1.0),
                        Vec3(1.0, 0.5625, -1.0),
                        Vec3(-1.0, 0.5625, 1.0),
                        Vec3(-1.0, 0.5625, -1.0)
                    ),
                    listOf(
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f)
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887"
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "ab849bae7ab0927a52836da1a45768527d1c7be5853a9290a283ae9aca0c908b"
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 1),
                        BlockPos(0, 1, 2),
                        BlockPos(1, 1, 0),
                        BlockPos(1, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 1),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(3)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(-1.0, 0.15625, 1.0),
                        Vec3(-1.0, 0.15625, -1.0),
                        Vec3(0.0, 0.4375, 1.0),
                        Vec3(-1.0, 0.4375, 0.0),
                        Vec3(0.0, 0.4375, -1.0),
                        Vec3(1.0, 0.15625, 1.0),
                        Vec3(1.0, 0.15625, -1.0),
                        Vec3(1.0, 0.4375, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f),
                        Rotations(180.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                    ),
                    yRotations = listOf(
                        0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                    ),
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887"
                ) + listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.15625, 0.0),
                        hashString = "ab849bae7ab0927a52836da1a45768527d1c7be5853a9290a283ae9aca0c908b"
                    ),
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(1.6875, 1.0, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 67.5f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    )
                ),
                13..13,
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    surroundWheatPositions,
                    blockState = wheatState(6)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.matcherPattern(
                    listOf(
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
                    listOf(
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
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887"
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.34375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "9bc7d71431dcdcfa432e8ef9fdb6aa4c4683786ac657e7ece038fb94f71e42be"
                    )
                ),
                32..32
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    surroundWheatPositions,
                    blockState = wheatState(6)
                ) + listOf(
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.matcherPattern(
                    listOf(
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
                    listOf(
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
                    xRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
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
                    hashString = "a0cc95bd6b1e5c007cf0d2b8c613a33a7ad3500b27638947c0b6b1db8fcb4887"
                ) + listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.0),
                        hashString = "9bc7d71431dcdcfa432e8ef9fdb6aa4c4683786ac657e7ece038fb94f71e42be"
                    )
                ),
                34..37 // 34 35 and 37 same so assuming for 36
            )

        ),
        // 10 days decay time
        maxStage = 40,
        isMutation = true
    )
}