package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import org.magic.magicaddons.data.greenhouse.CropStandReader
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.NEVER_DECAYS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import net.minecraft.core.Rotations
import org.magic.magicaddons.data.greenhouse.StandPose

object AllinAloe : CropDefinitionProvider {
    val fragmentSkyblockId: SkyBlockId = SkyBlockItemId.item("ALL_IN_ALOE_FRAGMENT")

    override val definition = CropDefinition(
        name = "All-in Aloe",
        effects = setOf(
            CropEffect.HarvestBoost
        ),
        skyblockId = SkyBlockItemId.item("ALL_IN_ALOE"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "dde18b1db0f938380dd8bed0c9189c3e62ea3acf900a19b2e95f52708c3ae3f2" to StandPose.Fixed(Rotations(22.5f, 0.0f, 0.0f)),
            "955a5ebfc03404c361753d267f7d1664692a2da3ebc63fc3b74925015ab7171b" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.25, -0.125),
                        hashString = "dde18b1db0f938380dd8bed0c9189c3e62ea3acf900a19b2e95f52708c3ae3f2"
                    )
                ),
                1..1
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
                        offset = Vec3(0.0, -0.15625, -0.125),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "dde18b1db0f938380dd8bed0c9189c3e62ea3acf900a19b2e95f52708c3ae3f2",
                        isSmall = false
                    )
                ),
                2..2,
                readers = listOf(
                    CropStandReader.stageLabel(),
                    CropStandReader.percentLabel(CropStandReader.REWARDS_RESET, "reset"),
                    CropStandReader.multiplierLabel(CropStandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.0625, -0.125),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "dde18b1db0f938380dd8bed0c9189c3e62ea3acf900a19b2e95f52708c3ae3f2",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.0625, 0.09375),
                        hashString = "955a5ebfc03404c361753d267f7d1664692a2da3ebc63fc3b74925015ab7171b"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.09375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "955a5ebfc03404c361753d267f7d1664692a2da3ebc63fc3b74925015ab7171b",
                        isSmall = false
                    )
                ),
                5..5,
                readers = listOf(
                    CropStandReader.stageLabel(),
                    CropStandReader.percentLabel(CropStandReader.REWARDS_RESET, "reset"),
                    CropStandReader.multiplierLabel(CropStandReader.REWARDS_MULTIPLIER, "reward")
                )
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.15625, 0.09375),
                        hashString = "955a5ebfc03404c361753d267f7d1664692a2da3ebc63fc3b74925015ab7171b"
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.09375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "241163612258d30dc6ef63b21f61ba89c622e5dcebd99fd36a3b507e80cdc725",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.25, 0.09375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "241163612258d30dc6ef63b21f61ba89c622e5dcebd99fd36a3b507e80cdc725",
                        isSmall = false
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.34375, 0.09375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "241163612258d30dc6ef63b21f61ba89c622e5dcebd99fd36a3b507e80cdc725",
                        isSmall = false
                    )
                ),
                9..9
            )),
        decayTimeMs = NEVER_DECAYS,
        maxStage = 27,
        requiredSoil = setOf(Blocks.SAND),
        needsWater = false,
        isMutation = true
    )
}