package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.deadBushState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Ashwreath
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Scourroot
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Coalroot {
    val definition = CropDefinition(
        name = "Coalroot",
        tier = CropTier.Uncommon,
        dropMultiplier = 1.7,
        effects = setOf(
            CropEffect.XpBoost
        ),
        skyblockId = SkyBlockItemId.item("COALROOT"),
        standPoses = mapOf(
            "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = true
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                    offset = Vec3(0.0, -0.75, 0.0),
                    hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                    isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                4..4
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
                    offset = Vec3(0.0, -0.5625, 0.0),
                    hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                    isSmall = false
                    )
                ),
                5..5
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
                        offset = Vec3(0.0, -0.5625, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = deadBushState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        hashString = "f946443fa0039354edd31a70c749c4f963464744dc20b79137bd9910356ee90",
                        isSmall = false
                    )
                ),
                8..8
            )

        ),
        maxStage = 8,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Scourroot" to 3, "Ashwreath" to 5))
    )
}