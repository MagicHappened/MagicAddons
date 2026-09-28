package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.Footprint
import org.magic.magicaddons.data.greenhouse.crops.SIX_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Dustgrain
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Witherbloom
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.Creambloom
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.Duskbloom
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.Thornshade
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Snoozling {
    private val wheatPositions = listOf(
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 2),
        BlockPos(2, 1, 0),
        BlockPos(2, 1, 2)
    )

    val definition = CropDefinition(
        name = "Snoozling",
        tier = CropTier.Rare,
        dropMultiplier = 21.0,
        effects = setOf(
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("SNOOZLING"),
        standPoses = mapOf(
            "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999" to StandPose.Fixed(Rotations(-22.5f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, 0.15625, 0.03125),
                            hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                            isSmall = true
                        ),
                        StageStand(
                            isSmall = false,
                            offset = Vec3(0.0, -0.46875, 0.5625),
                            hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999"
                        )
                    ),
                1..2
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, 0.25, -0.1875),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(-0.5, 0.25, -0.21875),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                3..4
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "2c856bec39e5f5fc04fc4c7d90f7d404cee2c628d911c7a756ef5b72f2b876f4",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(-0.53125, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    )
                ),
                5..5,
                // read rather than matched: the sleep stand comes and goes, and requiring it would
                // make a woken snoozling fail to be a snoozling
                readers = listOf(
                    StandReader.standPresence(StandReader.ASLEEP, "z"),
                    StandReader.skullPresence(StandReader.ASLEEP, "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215")
                )
            ),
            // awake at stage 5; the entry above is the sleeping look
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.53125, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(2)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, -0.0625, 0.53125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(2)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, 0.0625, 0.71875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.15625, 0.625, 0.1875),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(3)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0625, 0.0625, 0.71875),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.03125),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.1875, -0.40625),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.5, 0.25, -0.15625),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.15625, 0.625, 0.1875),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(3)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        hashString = "2c856bec39e5f5fc04fc4c7d90f7d404cee2c628d911c7a756ef5b72f2b876f4",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.15625, -0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.4375, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    )
                ),
                10..10,
                readers = listOf(
                    StandReader.standPresence(StandReader.ASLEEP, "z"),
                    StandReader.skullPresence(StandReader.ASLEEP, "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215")
                )
            ),
            // awake at stage 10, the head level again
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(3)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.4375, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.15625, -0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.4375, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.15625, -0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(4)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.4375, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.15625, -0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                12..12
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(4)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.65625, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                13..14
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(4)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.125, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.5, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.65625, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                15..15
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(4)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.03125, -0.1875, 0.65625),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        hashString = "2c856bec39e5f5fc04fc4c7d90f7d404cee2c628d911c7a756ef5b72f2b876f4",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, -0.84375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.78125, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.65625, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215",
                        isSmall = true
                    )
                ),
                15..15,
                readers = listOf(
                    StandReader.standPresence(StandReader.ASLEEP, "z"),
                    StandReader.skullPresence(StandReader.ASLEEP, "885c448a847959a7ea71f79686516886692e2c80b5464725dde847d5ae5a7215")
                )
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(0)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.4375, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                16..16
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(5)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.4375, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    )
                ),
                17..17
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(5)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "77bb86dedeb827f2489aa0103d58d0e12e64a8152d5a0f5b1d4d208a3cb55999",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.9375, -0.90625),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                18..19
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = wheatPositions,
                    blockState = wheatState(5)
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.84375),
                        hashString = "24c64afa58bef69ff567b012a2b1638cf475c5bdb050d382308399ffa0b06a8d",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.25, -0.625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.9375, -0.90625),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, 0.4375, 0.40625),
                        headRotation = Rotations(22.5f, 0.0f, 0.0f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = true
                    ),
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        yRotation = -180.0f,
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(0.5, -0.65625, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, 22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.46875, -0.59375, -0.375),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7",
                        isSmall = false
                    )
                ),
                20..20
            )),
        decayTimeMs = SIX_DAY_DECAY_TIME_MS,
        maxStage = 20,
        // it drops asleep on arriving at each of these and grows no further until it is woken;
        // there is no sleep at 20, it goes straight to harvestable
        sleepStages = setOf(5, 10, 15),
        stallExplanation = "This snoozling is asleep and will not grow until it is woken.",
        footprint = Footprint(3, 3),
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Creambloom" to 4, "Dustgrain" to 3, "Witherbloom" to 3, "Duskbloom" to 3, "Thornshade" to 3))
    )
}
