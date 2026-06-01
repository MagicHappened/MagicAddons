package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Duskbloom : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Duskbloom",
        skyblockId = SkyBlockItemId.item("DUSKBLOOM"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.65625, 0.0),
                            hashMatches = {
                                it == "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02"
                            },
                        )
                    ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashMatches = {
                            it == "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02"
                        },
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.5625, 0.0),
                            hashMatches = {
                                it == "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02"
                            },
                        )
                    ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashMatches = {
                            it == "b9410dd823e984f98c0572e48d3c07641dd89411ba2e4fc66bee4212c6b65f02"
                        },
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashMatches = {
                            it == "7dca7951b36f5f749e883758b379c8008ca55f245987e4ef0c3788cf0c903d5"
                        }
                    )
                ),
                8..8,
                allowRotation = true
            )

        ),
        maxStage = 8,
        isMutation = true
    )
}