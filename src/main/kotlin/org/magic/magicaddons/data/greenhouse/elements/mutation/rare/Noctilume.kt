package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Noctilume : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Noctilume",
        skyblockId = SkyBlockItemId.item("NOCTILUME"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,1,1),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(1,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(1,1,1),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(1.375, 0.84375, 0.6875),
                        matcher = {
                            it == "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(1.28125, 0.78125, 1.125),
                        matcher = {
                            it == "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.78125, 0.6875, 1.15625),
                        matcher = {
                            it == "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8"
                        }
                    )
                ),
                2..2,
                allowRotation = true //DAYTIME ONE
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 5
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(0,1,1),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 5
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(1,1,0),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 5
                        }
                    ),
                    CropBlockState(
                        offset = BlockPos(1,1,1),
                        matcher = {
                            it.isBlock("minecraft:wheat") &&
                                    it.getIntProperty("age") == 5
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.875, 0.78125, 1.28125),
                        matcher = {
                            it == "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.84375, -0.0625, 0.78125),
                        matcher = {
                            it == "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(1.40625, -0.03125, 0.875),
                        matcher = {
                            it == "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(1.3125, 0.09375, 1.375),
                        matcher = {
                            it == "329aa65e77ecc216dbadc774121dec2f3d7267289462eb5d11d3bafa6f5996c8"
                        }
                    )
                ),
                3..3, //this is DAYTIME noctilume stage 3
                // fuck need to somehow convert day time hash to NeedsDaytime and NeedsNighttime
                // could make matcher match any and then based on the first one?
                // will have to think about it
                allowRotation = true //todo check the others
            )


        ),
        maxStage = 4,
        footprint = Footprint(2,2),
        isMutation = true
    )
}

/*
CropStage(
    blocks = listOf(
        CropBlockState(
            offset = BlockPos(0,1,0),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 3
            }
        ),
        CropBlockState(
            offset = BlockPos(0,1,1),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 3
            }
        ),
        CropBlockState(
            offset = BlockPos(1,1,0),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 3
            }
        ),
        CropBlockState(
            offset = BlockPos(1,1,1),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 3
            }
        )
    ),
    armorStands = listOf(
        CropArmorStand(
    offset = Vec3(1.21875, 0.6875, 0.84375),
    matcher = {
        it == "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09"
    }
),
        CropArmorStand(
    offset = Vec3(0.625, 0.84375, 1.3125),
    matcher = {
        it == "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09"
    }
),
        CropArmorStand(
    offset = Vec3(1.125, 0.71875, 1.40625),
    matcher = {
        it == "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09"
    }
),
        CropArmorStand(
    offset = Vec3(0.71875, 0.78125, 0.875),
    matcher = {
        it == "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09"
    }
)
    ),
    1..1
)






CropStage(
    blocks = listOf(
        CropBlockState(
            offset = BlockPos(0,1,0),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 5
            }
        ),
        CropBlockState(
            offset = BlockPos(0,1,1),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 5
            }
        ),
        CropBlockState(
            offset = BlockPos(1,1,0),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 5
            }
        ),
        CropBlockState(
            offset = BlockPos(1,1,1),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 5
            }
        )
    ),
    armorStands = listOf(
        CropArmorStand(
    offset = Vec3(0.71875, 0.78125, 0.875),
    matcher = {
        it == "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09"
    }
),
        CropArmorStand(
    offset = Vec3(1.125, -0.03125, 1.40625),
    matcher = {
        it == "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09"
    }
),
        CropArmorStand(
    offset = Vec3(1.21875, -0.0625, 0.84375),
    matcher = {
        it == "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09"
    }
),
        CropArmorStand(
    offset = Vec3(0.625, 0.09375, 1.3125),
    matcher = {
        it == "281e8164cf7af240cc235d4826996013bd045de20d40abd262145dc24c790a09"
    }
)
    ),
    3..3 // NIGHT TIME
)

// TODO allow rotation on all after sorting this mess


 */