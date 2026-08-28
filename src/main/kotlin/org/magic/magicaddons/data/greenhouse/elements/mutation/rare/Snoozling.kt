package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Snoozling : CropDefinitionProvider {
    private val wheatPositions = listOf(
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 2),
        BlockPos(2, 1, 0),
        BlockPos(2, 1, 2),
    )

    override val definition = CropDefinition(
        name = "Snoozling",
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("SNOOZLING"),
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = wheatPositions,
                    blockState = wheatState(0),
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                        isSmall = false,
                            offset = Vec3(-0.03125, 0.15625, 0.0),
                            hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        ),
                        CropArmorStand(
                        isSmall = false,
                            offset = Vec3(-0.5625, -0.46875, 0.0),
                            hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        )
                    ),
                1..1
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                        isSmall = false,
                            offset = Vec3(-0.53125, -0.0625, 0.0625),
                            hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        )
                    )
                            +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(-0.03125, -0.46875, 0.0),
                                    Vec3(0.1875, 0.25, 0.5),
                                    Vec3(0.21875, 0.25, -0.5),
                                    Vec3(0.40625, 0.1875, 0.0)
                                ),
                                hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                            ),
                3..4,
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                        isSmall = false,
                            offset = Vec3(-0.53125, -0.0625, 0.0625),
                            hashString = "2c856bec39e5f5fc04fc4c7d90f7d404cee2c628d911c7a756ef5b72f2b876f4",
                        )
                    )
                            +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(0.15625, 0.25, 0.5),
                                    Vec3(0.15625, 0.25, -0.53125),
                                    Vec3(0.40625, 0.1875, 0.0),
                                    Vec3(-0.03125, -0.46875, 0.0)
                                ),
                                hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215"
                            ),
                5..5,
                allowRotation = true,
                // a snoozling drops asleep every fifth stage and stops growing until it is woken
                // by hand. The stand saying so comes and goes, so it is read rather than matched:
                // requiring it would make a woken snoozling fail to be a snoozling at all
                readers = listOf(CropStandReader.presence(CropStandReader.ASLEEP, "z"))
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = wheatPositions,
                    blockState = wheatState(5)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.0, 0.4375, 0.40625),
                        Vec3(0.0, -0.5625, 0.0),
                        Vec3(0.5, -0.65625, -0.375),
                        Vec3(-0.46875, -0.59375, -0.375),
                        Vec3(0.0, -0.25, -0.625),
                        Vec3(0.0, 0.9375, -0.90625)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 22.5f),
                        Rotations(0.0f, 0.0f, -22.5f),
                        Rotations(22.5f, 0.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        -180.0f,
                        0.0f,
                        0.0f,
                        -180.0f,
                        -180.0f
                    ),
                    hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                ) + listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.0, 0.84375),
                        hashString = "24c64afa58bef69ff567b012a2b1638cf475c5bdb050d382308399ffa0b06a8d"
                    )
                ),
                20..20,
                allowRotation = true
            )


        ),
        maxStage = 20,
        footprint = Footprint(3, 3),
        isMutation = true
    )
}

/*








sleeping hash^^

 */