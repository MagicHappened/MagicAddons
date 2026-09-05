package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object PlantBoyAdvance : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "PlantBoy Advance",
        effects = setOf(
            CropEffect.HarvestBoost
        ),
        skyblockId = SkyBlockItemId.item("PLANTBOY_ADVANCE"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "765accb195aad0d7212eedd647e3f80ed5d4acdffe4329ead074f7587f366457" to StandPose.Fixed(Rotations(-22.5f, 0.0f, 0.0f)),
            "f10e337f0a17a99e94bed4e8b13d5c7863debdd301f457da19763cf73a34d200" to StandPose.Fixed(Rotations(-45.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(2)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.3125, -0.84375, 0.0),
                        Vec3(-0.34375, -0.84375, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 0.0f, 0.0f),
                        Rotations(45.0f, 180.0f, 0.0f)
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.65625, -0.1875),
                        headRotation = Rotations(-65.0f, 0.0f, 0.0f),
                        hashString = "a842c0c12f515281c228b2827f1c34d12b19833fa84083c6bee831245ceaa914"
                    )
                ),
                1..1,
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(2)
                ) + CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(1)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(-0.34375, -0.84375, 0.0),
                        Vec3(0.3125, -0.84375, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.65625, -0.28125),
                        headRotation = Rotations(-45.0f, 0.0f, 0.0f),
                        hashString = "a842c0c12f515281c228b2827f1c34d12b19833fa84083c6bee831245ceaa914"
                    )
                ),
                2..2,
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(3)
                ) + CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(1)
                ),
                armorStands = CropArmorStand.matcherPattern(
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
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.65625, 0.09375),
                        hashString = "f10e337f0a17a99e94bed4e8b13d5c7863debdd301f457da19763cf73a34d200"
                    )
                ),
                3..3,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 1, 1),
                        blockState = melonStemState(2)
                    ),
                    CropBlockState(
                        offset = BlockPos(1, 1, 0),
                        blockState = melonStemState(6)
                    ),
                    CropBlockState(
                        offset = BlockPos(1, 1, 1),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.5625, 0.0),
                            hashString = "1822281949d048a10d54ed72cdd4c222312a86fbf946ba56aea35f5142d0ee7a",
                        )
                    )
                            +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(-0.5, -0.65625, 0.0),
                                    Vec3(0.5, -0.65625, 0.0)
                                ),
                                hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337"
                            ),
                9..9,
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(7)
                ) + CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(3)
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.5625, 0.0),
                            hashString = "1822281949d048a10d54ed72cdd4c222312a86fbf946ba56aea35f5142d0ee7a",
                        )
                    )
                            +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(0.5, -0.65625, 0.0),
                                    Vec3(-0.5, -0.65625, 0.0)
                                ),
                                hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337"
                            ),
                10..10,
            ),
            CropStage(
                blocks =             CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(7)
                ) +             CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.5, -0.65625, 0.0),
                        Vec3(-0.5, -0.65625, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) + listOf(
                    CropArmorStand(
                    offset = Vec3(0.0, -0.4375, 0.0),
                    hashString = "765accb195aad0d7212eedd647e3f80ed5d4acdffe4329ead074f7587f366457",
                    isSmall = false
                    )
                ),
                12..12,
            )


        ,
            // as placed
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(1, 1, 0)
                    ),
                    blockState = melonStemState(7)
                ) +             CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = melonStemState(4)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.5, -0.65625, 0.0),
                        Vec3(-0.5, -0.65625, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(45.0f, 180.0f, 0.0f),
                        Rotations(45.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f
                    ),
                    hashString = "9eaf5fc0bf98649111f53d7516b18dec5d9d13f19273bef2b2b04f068ca9d337",
                    isSmall = false
                ) +
                listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "765accb195aad0d7212eedd647e3f80ed5d4acdffe4329ead074f7587f366457",
                        isSmall = false
                    )
                ),
                12..12,
                placed = true
            )),
        // five days decay time
        maxStage = 12,
        footprint = Footprint(2, 2),
        rotatesWithPlot = false,
        needsWater = false,
        isMutation = true
    )
}