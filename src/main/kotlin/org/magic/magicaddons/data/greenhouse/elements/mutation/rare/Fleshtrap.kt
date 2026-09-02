package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Fleshtrap : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Fleshtrap",
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("FLESHTRAP"),
        stageDefs = listOf(
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
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
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
                        isSmall = false,
                        offset = Vec3(0.0, 0.25, 0.0),
                        headRotation = Rotations(112.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
                    )
                ),
                8..8,
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
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
                    )
                ),
                13..13,
                // hunger and bonus change from moment to moment, so neither can be part of matching.
                // A missing bonus label means never fed, not fed nothing
                readers = listOf(
                    CropStandReader.bar(CropStandReader.HUNGER),
                    CropStandReader.percentLabel(CropStandReader.BONUS, "Bonus")
                )
            )


        ),
        maxStage = 14,
        decayTimeMs = NEVER_DECAYS,
        isMutation = true
    )
}

/*


// BEWARE SUBTRACT 0.5 FROM X AND Z


CropStage(
    blocks = listOf(
        CropBlockState(
            offset = BlockPos(0,1,0),
            matcher = {
it.isBlock("minecraft:wheat") &&
        it.getIntProperty("age") == 3
            }
        )
    ),
    armorStands = listOf(
        CropArmorStand(
    offset = Vec3(0.5, 2.6119999999999948, 0.5),
    matcher = {
        true
    }
),
        CropArmorStand(
    offset = Vec3(0.5, 2.2419999999999902, 0.5),
    matcher = {
        true
    }
),
        CropArmorStand(
    offset = Vec3(0.5, 1.8719999999999857, 0.5),
    matcher = {
        true
    }
),
        CropArmorStand(
    offset = Vec3(0.5, -0.34375, 0.5),
    matcher = {
        it == "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
    }
)
    ),
    3..3
)
CropStage(
    blocks = listOf(
        CropBlockState(
            offset = BlockPos(0,1,0),
            matcher = {
it.isBlock("minecraft:melon_stem") &&
        it.getIntProperty("age") == 5
            }
        )
    ),
    armorStands = listOf(
        CropArmorStand(
    offset = Vec3(0.5, 2.6119999999999948, 0.5),
    matcher = {
        true
    }
),
        CropArmorStand(
    offset = Vec3(0.5, 2.2419999999999902, 0.5),
    matcher = {
        true
    }
),
        CropArmorStand(
    offset = Vec3(0.5, 1.8719999999999857, 0.5),
    matcher = {
        true
    }
),
        CropArmorStand(
    offset = Vec3(0.5, 0.0625, 0.5),
    matcher = {
        it == "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
    }
)
    ),
    6..6
)
CropStage(
    blocks = listOf(
        CropBlockState(
            offset = BlockPos(0,1,0),
            matcher = {
it.isBlock("minecraft:melon_stem") &&
        it.getIntProperty("age") == 5
            }
        )
    ),
    armorStands = listOf(
        CropArmorStand(
    offset = Vec3(0.5, 0.0625, 0.5),
    matcher = {
        it == "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
    }
),
        CropArmorStand(
    offset = Vec3(0.5, 2.6119999999999948, 0.5),
    matcher = {
        it.name == "+100% Bonus"
    }
),
        CropArmorStand(
    offset = Vec3(0.5, 2.2419999999999902, 0.5),
    matcher = {
        it.name == "Hunger"
    }
),
        CropArmorStand(
    offset = Vec3(0.5, 1.8719999999999857, 0.5),
    matcher = {
        it.name == "||||||||||||||||||||"
    }
)
    ),
    7..7
)



with armor stands that correlate to bonus hunger and hunger level ^^

CropStage(
    blocks = listOf(
        CropBlockState(
            offset = BlockPos(0,1,0),
            matcher = {
it.isBlock("minecraft:melon_stem") &&
        it.getIntProperty("age") == 7
            }
        )
    ),
    armorStands = listOf(
        CropArmorStand(
    offset = Vec3(0.5, 0.25, 0.5),
    matcher = {
        it == "c7f45f6cb2e4bbf45c5537c4dc3055a323021d62db7d91cc60beb02956401fb9"
    }
)
    ),
    14..14
)
this one placed so no other armor stands, i think other armor stands where bogus


 */