package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.StandPose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object DoNotEatShroom : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Do-not-eat-shroom",
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("DO_NOT_EAT_SHROOM"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "6a7ae95a8bb1fcdbf71385fe663c5647e0a5c32004db8c0313c2d45c94e3d1ad" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "77a99b274b5e21a3537469b2ae717bb4cedcacca76df7525092a99bc08ea8c9" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "3f22178e2e72d6929a6ef9199795a93cfbad999bbee0aba235b277d0b18e0e94" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "1772fa43e9f41925f681690167db25d5488a7fa4b428ec4e95a4b683f15dbb4" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.125, 0.0),
                        hashString = "77a99b274b5e21a3537469b2ae717bb4cedcacca76df7525092a99bc08ea8c9"
                    )
                ),
                1..1
            ),
            CropStagePattern(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "77a99b274b5e21a3537469b2ae717bb4cedcacca76df7525092a99bc08ea8c9"
                    )
                ),
                stageRange = 2..3,
                baseStageStandOffset = Vec3(0.0, 0.03125, 0.0),
                stageOffsetMultipliers = mapOf(
                    2 to 0,
                    3 to 2
                )
            ),
            CropStagePattern(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.59375, 0.0),
                        hashString = "1772fa43e9f41925f681690167db25d5488a7fa4b428ec4e95a4b683f15dbb4"
                    )
                ),
                stageRange = 4..5,
                baseStageStandOffset = Vec3(0.0, 0.03125, 0.0),
                stageOffsetMultipliers = mapOf(
                    4 to 0,
                    5 to 1
                )
            ),
            CropStagePattern(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "6a7ae95a8bb1fcdbf71385fe663c5647e0a5c32004db8c0313c2d45c94e3d1ad"
                    )
                ),
                stageRange = 6..7,
                baseStageStandOffset = Vec3(0.0, 0.03125, 0.0),
                stageOffsetMultipliers = mapOf(
                    6 to 0,
                    7 to 2,
                )
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "3f22178e2e72d6929a6ef9199795a93cfbad999bbee0aba235b277d0b18e0e94"
                    )
                ),
                8..8
            )
        ),
        maxStage = 8,
        isMutation = true
    )

    /*
    CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, 0.125, 0.0),
                    matcher = {
                        it == "77a99b274b5e21a3537469b2ae717bb4cedcacca76df7525092a99bc08ea8c9"
                    }
                )
            ),
            1..1
        ),
        CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.6875, 0.0),
                    matcher = {
                        it == "77a99b274b5e21a3537469b2ae717bb4cedcacca76df7525092a99bc08ea8c9"
                    }
                )
            ),
            2..2
        ),
        CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.625, 0.0),
                    matcher = {
                        it == "77a99b274b5e21a3537469b2ae717bb4cedcacca76df7525092a99bc08ea8c9"
                    }
                )
            ),
            3..3
        ),
        CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.59375, 0.0),
                    matcher = {
                        it == "1772fa43e9f41925f681690167db25d5488a7fa4b428ec4e95a4b683f15dbb4"
                    }
                )
            ),
            4..4
        ),
        CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.5625, 0.0),
                    matcher = {
                        it == "1772fa43e9f41925f681690167db25d5488a7fa4b428ec4e95a4b683f15dbb4"
                    }
                )
            ),
            5..5
        ),
         CropStage(
    blocks = listOf(
    ),
    armorStands = listOf(
        CropArmorStand(
    offset = Vec3(0.0, -0.5, 0.0),
    matcher = {
        it == "6a7ae95a8bb1fcdbf71385fe663c5647e0a5c32004db8c0313c2d45c94e3d1ad"
    }
)
    ),
    6..6
),
        CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.4375, 0.0),
                    matcher = {
                        it == "6a7ae95a8bb1fcdbf71385fe663c5647e0a5c32004db8c0313c2d45c94e3d1ad"
                    }
                )
            ),
            7..7
        ),
     */


}