package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.FIVE_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.crops.Footprint
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Snoozling
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object PlantBoyAdvance {
    val definition = CropDefinition(
        name = "PlantBoy Advance",
        tier = CropTier.Epic,
        dropMultiplier = 23.0,
        effects = setOf(
            CropEffect.HarvestBoost
        ),
        skyblockId = SkyBlockItemId.item("PLANTBOY_ADVANCE"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "765accb195aad0d7212eedd647e3f80ed5d4acdffe4329ead074f7587f366457" to StandPose.Fixed(Rotations(-22.5f, 0.0f, 0.0f)),
            "f10e337f0a17a99e94bed4e8b13d5c7863debdd301f457da19763cf73a34d200" to StandPose.Fixed(Rotations(-45.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(2)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.34375, -0.84375, 0.0),
                        Vec3(-0.3125, -0.84375, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.65625, 0.1875),
                        headRotation = Rotations(-65.0f, 0.0f, 0.0f),
                        hashString = "a842c0c12f515281c228b2827f1c34d12b19833fa84083c6bee831245ceaa914"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(2)
                ) + StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(1)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.34375, -0.84375, 0.0),
                        Vec3(-0.3125, -0.84375, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.65625, 0.28125),
                        headRotation = Rotations(-45.0f, 0.0f, 0.0f),
                        hashString = "a842c0c12f515281c228b2827f1c34d12b19833fa84083c6bee831245ceaa914"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(3)
                ) + StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(1)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.28125, -0.75, 0.0),
                        Vec3(-0.3125, -0.75, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.65625, 0.09375),
                        hashString = "f10e337f0a17a99e94bed4e8b13d5c7863debdd301f457da19763cf73a34d200"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(4)
                ) + StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(1)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.5, -0.75, 0.0),
                        Vec3(-0.5, -0.75, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.59375, 0.0),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "f10e337f0a17a99e94bed4e8b13d5c7863debdd301f457da19763cf73a34d200"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(5)
                ) + StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(1)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.5, -0.75, 0.0),
                        Vec3(-0.5, -0.75, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "f10e337f0a17a99e94bed4e8b13d5c7863debdd301f457da19763cf73a34d200"
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(5)
                ) + StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(2)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.5, -0.65625, 0.0),
                        Vec3(-0.5, -0.65625, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "e39c5ac1ad3751dd6b349eba679a046ff9ca8bfc321bff6ec63ea88506cb79e"
                    )
                ),
                6..7
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(6)
                ) + StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(2)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.5, -0.65625, 0.0),
                        Vec3(-0.5, -0.65625, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "e39c5ac1ad3751dd6b349eba679a046ff9ca8bfc321bff6ec63ea88506cb79e"
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 1, 1),
                        blockState = melonStemState(2)
                    ),
                    StageBlock(
                        offset = BlockPos(1, 1, 0),
                        blockState = melonStemState(6)
                    ),
                    StageBlock(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.5625, 0.0),
                            headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                            hashString = "1822281949d048a10d54ed72cdd4c222312a86fbf946ba56aea35f5142d0ee7a",
                            isSmall = false
                        )
                    )
                            +
                            StageStand.atOffsets(
                                offsets = listOf(
                                    Vec3(-0.5, -0.65625, 0.0),
                                    Vec3(0.5, -0.65625, 0.0)
                                ),
                                rotations = listOf(
                                    Rotations(45.0f, 0.0f, 0.0f),
                                    Rotations(45.0f, 180.0f, 0.0f)
                                ),
                                yRotations = listOf(
                                    90.0f,
                                    90.0f
                                ),
                                hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                                isSmall = false
                            ),
                9..9
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(7)
                ) + StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(3)
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.5625, 0.0),
                            headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                            hashString = "1822281949d048a10d54ed72cdd4c222312a86fbf946ba56aea35f5142d0ee7a",
                            isSmall = false
                        )
                    )
                            +
                            StageStand.atOffsets(
                                offsets = listOf(
                                    Vec3(0.5, -0.65625, 0.0),
                                    Vec3(-0.5, -0.65625, 0.0)
                                ),
                                rotations = listOf(
                                    Rotations(45.0f, 180.0f, 0.0f),
                                    Rotations(45.0f, 0.0f, 0.0f)
                                ),
                                yRotations = listOf(
                                    90.0f,
                                    90.0f
                                ),
                                hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                                isSmall = false
                            ),
                10..10
            ),
            CropStage(
                blocks = StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(7)
                ) + StageBlock.atPositions(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(4)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.5, -0.65625, 0.0),
                        Vec3(-0.5, -0.65625, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "1822281949d048a10d54ed72cdd4c222312a86fbf946ba56aea35f5142d0ee7a"
                    )
                ),
                11..11
            ),
            CropStage(
                blocks =             StageBlock.atPositions(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(7)
                ) +             StageBlock.atPositions(
                    listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = StageStand.atOffsets(
                    offsets = listOf(
                        Vec3(0.5, -0.65625, 0.0),
                        Vec3(-0.5, -0.65625, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    StageStand(
                    offset = Vec3(0.0, -0.4375, 0.0),
                    hashString = "765accb195aad0d7212eedd647e3f80ed5d4acdffe4329ead074f7587f366457",
                    isSmall = false
                    )
                ),
                12..12
            )


        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 12,
        footprint = Footprint(2, 2),
        rotatesWithPlot = false,
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Snoozling" to 6, "Thunderling" to 6))
    )
}