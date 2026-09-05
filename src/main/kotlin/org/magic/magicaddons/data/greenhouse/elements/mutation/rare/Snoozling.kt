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
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999" to StandPose.Fixed(Rotations(-22.5f, 0.0f, 0.0f)),
            "24c64afa58bef69ff567b012a2b1638cf475c5bdb050d382308399ffa0b06a8d" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = wheatPositions,
                    blockState = wheatState(0),
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 0.15625, 0.03125),
                            hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        ),
                        CropArmorStand(
                            isSmall = false,
                            offset = Vec3(0.0, -0.46875, 0.5625),
                            hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        )
                    ),
                // collected at stages one and two and identical at both, so the two are one stage
                // as far as looking at the plant goes
                1..2
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
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "2c856bec39e5f5fc04fc4c7d90f7d404cee2c628d911c7a756ef5b72f2b876f4",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215"
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.53125, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    )
                ),
                5..5,
                // read rather than matched: the sleep stand comes and goes, and requiring it would
                // make a woken snoozling fail to be a snoozling
                readers = listOf(CropStandReader.presence(CropStandReader.ASLEEP, "z"))
            ),
            // awake at stage 5 with the corner wheat grown to age 2
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(2)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                5..5,
                readers = listOf(CropStandReader.presence(CropStandReader.ASLEEP, "z"))
            ),
            // awake at stage 5; the entry above may be the sleeping look
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.53125, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                5..5,
                readers = listOf(CropStandReader.presence(CropStandReader.ASLEEP, "z"))
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(2)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(3)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0625, 0.0625, 0.71875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.15625, 0.625, 0.1875),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(3)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "2c856bec39e5f5fc04fc4c7d90f7d404cee2c628d911c7a756ef5b72f2b876f4",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, -0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.4375, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    )
                ),
                10..10,
                readers = listOf(CropStandReader.presence(CropStandReader.ASLEEP, "z"))
            ),
            // awake at stage 10, the head level again
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(3)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.4375, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, -0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.4375, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, -0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(4)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.4375, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, -0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                12..12
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(4)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                13..13
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(4)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                14..14
            ),
            // awake at stage 15: the skulls are the awake ones, so it files beside the sleeping look when that is recorded
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(4)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.125, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                15..15
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                16..16
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.9375, -0.90625),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                18..18
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.9375, -0.90625),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                19..19
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "24c64afa58bef69ff567b012a2b1638cf475c5bdb050d382308399ffa0b06a8d",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.9375, -0.90625),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                20..20
            ),
            // as placed
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 2),
                        BlockPos(2, 1, 0),
                        BlockPos(2, 1, 2)
                    ),
                    blockState = wheatState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "24c64afa58bef69ff567b012a2b1638cf475c5bdb050d382308399ffa0b06a8d",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.9375, -0.90625),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                    )
                ),
                20..20,
                placed = true
            )),
        decayTimeMs = SIX_DAY_DECAY_TIME_MS,
        maxStage = 20,
        // it drops asleep on arriving at each of these and grows no further until it is woken;
        // there is no sleep at 20, it goes straight to harvestable
        sleepStages = setOf(5, 10, 15),
        footprint = Footprint(3, 3),
        isMutation = true
    )
}

/*








sleeping hash^^

 */