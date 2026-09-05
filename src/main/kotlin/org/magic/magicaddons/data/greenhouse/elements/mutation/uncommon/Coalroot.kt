package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import org.magic.magicaddons.data.greenhouse.StandPose
import net.minecraft.core.Rotations
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.deadBushState
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Coalroot : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Coalroot",
        effects = setOf(
            CropEffect.XpBoost
        ),
        skyblockId = SkyBlockItemId.item("COALROOT"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                2..2
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
                    offset = Vec3(0.0, -0.75, 0.0),
                    hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                    isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                4..4
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
                    offset = Vec3(0.0, -0.5625, 0.0),
                    hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                    isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                6..6
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
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = deadBushState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                8..8
            )

        ,
            // as bought: the page says stage eight, the stem is still the seventh
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                8..8,
                placed = true
            )),
        maxStage = 8,
        isMutation = true
    )
}