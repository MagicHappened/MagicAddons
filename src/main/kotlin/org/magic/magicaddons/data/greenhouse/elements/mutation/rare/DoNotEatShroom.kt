package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class DoNotEatShroom : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Do-not-eat-shroom",
        skyblockId = SkyBlockItemId.item("DO_NOT_EAT_SHROOM"),
        stageDefs = listOf(
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
            CropStagePattern(
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
                2..5,
                baseStageStandOffset = Vec3(0.0, 0.03125, 0.0),
                stageOffsetMultipliers = mapOf(
                    3 to 2,
                    4 to 1,
                    5 to 1,
                    //todo add 6 and see based on 7
                )
            ),
            CropStagePattern(
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
                stageRange = 6..7,
                baseStageStandOffset = Vec3(0.0, 0.03125, 0.0),
                stageOffsetMultipliers = mapOf(
                    7 to 2
                )
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        matcher = {
                            it == "3f22178e2e72d6929a6ef9199795a93cfbad999bbee0aba235b277d0b18e0e94"
                        }
                    )
                ),
                8..8
            )
        ),
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