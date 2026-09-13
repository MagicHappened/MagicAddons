package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.data.greenhouse.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Fleshtrap : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Fleshtrap",
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("FLESHTRAP"),
        stageDefs = listOf(
            // the hunger label floats above every stage and is read, not matched
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9",
                        isSmall = false
                    )
                ),
                1..1,
                readers = listOf(CropStandReader.bar(CropStandReader.HUNGER))
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9",
                        isSmall = false
                    )
                ),
                2..2,
                readers = listOf(CropStandReader.bar(CropStandReader.HUNGER))
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.34375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9",
                        isSmall = false
                    )
                ),
                3..3,
                readers = listOf(CropStandReader.bar(CropStandReader.HUNGER))
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.15625, 0.0),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9",
                        isSmall = false
                    )
                ),
                4..4,
                readers = listOf(CropStandReader.bar(CropStandReader.HUNGER))
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
                        offset = Vec3(0.0, 0.25, 0.0),
                        headRotation = Rotations(112.5f, 0.0f, 0.0f),
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.34375, 0.125),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
                    )
                ),
                13..13,
                // hunger and bonus change from moment to moment, so neither can be part of matching.
                // A missing bonus label means never fed, not fed nothing
                readers = listOf(
                    CropStandReader.bar(CropStandReader.HUNGER),
                    CropStandReader.hungerPercentLabel(CropStandReader.BONUS, "Bonus")
                )
            ),
            // the skull changes with hunger, so either look is stage 14
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.25, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9",
                        isSmall = false
                    )
                ),
                14..14,
                readers = listOf(
                    CropStandReader.bar(CropStandReader.HUNGER),
                    CropStandReader.hungerPercentLabel(CropStandReader.BONUS, "Bonus")
                )
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.25, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "2d013e63bc607acdb07173b76fc336a058248fddbfbfabfd72f529f542f0e46b",
                        isSmall = false
                    )
                ),
                14..14,
                readers = listOf(
                    CropStandReader.bar(CropStandReader.HUNGER),
                    CropStandReader.hungerPercentLabel(CropStandReader.BONUS, "Bonus")
                )
            )),
        maxStage = 14,
        decayTimeMs = NEVER_DECAYS,
        isMutation = true
    )
}
