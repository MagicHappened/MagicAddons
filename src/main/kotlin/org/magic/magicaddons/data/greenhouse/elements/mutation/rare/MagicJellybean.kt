package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.sugarcaneState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object MagicJellybean : CropDefinitionProvider {

    fun generateStages(): List<CropStage> {
        val baseStandOffset = Vec3(0.0, -0.21875, 0.0)

        val cycleSize = 12
        val maxStage = 120

        val stages = mutableListOf<CropStage>()

        for (stage in 1..maxStage) {

            val cycleIndex = stage % cycleSize
            val cycleStart = (stage / cycleSize) * cycleSize

            val caneHeight = (stage / cycleSize).coerceAtMost(10)

            val blocks = (1..caneHeight).map {
                CropBlockState(
                    offset = BlockPos(0, it, 0),
                    blockState = TODO()
//                    matcher = {
//                        it.isBlock("minecraft:sugar_cane") &&
//                                it.getIntProperty("age") == 0
//                    }
                )
            }.toMutableList()
            if (stage < maxStage){
                blocks += CropBlockState(
                    offset = BlockPos(0, caneHeight + 1, 0),
                    blockState = TODO()
//                    matcher = {
//                        it.isBlock("minecraft:melon_stem")
//                    }
                )
            }

            val baseArmorStands = (0 until caneHeight).map { i ->
                CropArmorStand(
                    offset = Vec3(
                        0.0,
                        baseStandOffset.y + i * 1.0,
                        0.0
                    ),
                    hashString = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                )
            }.toMutableList()

            val armorStands = baseArmorStands.toMutableList()

            val isTransition = cycleIndex >= 10

            if (isTransition) {
                armorStands += CropArmorStand(
                    offset = Vec3(
                        0.0,
                        baseStandOffset.y + caneHeight * 1.0,
                        0.0
                    ),
                    hashString = "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24"
                )
            }
            stages += CropStage(
                blocks = blocks,
                armorStands = armorStands,
                stageRange = stage..stage,
                allowRotation = true
            )
        }

        return stages
    }
    override val definition = CropDefinition(
        name = "Magic Jellybean",
        skyblockId = SkyBlockItemId.item("MAGIC_JELLYBEAN"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.78125, 0.0),
                        hashString = "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24"
                    )
                ),
                9..9,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = sugarcaneState()
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.21875, 0.0),
                        hashString = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 1.59375, 0.0),
                        hashString = "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24"
                    )
                ),
                18..18,
                allowRotation = true
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0),
                        BlockPos(0, 4, 0),
                        BlockPos(0, 5, 0),
                        BlockPos(0, 6, 0),
                        BlockPos(0, 7, 0)
                    ),
                    blockState = sugarcaneState()
                ) +
                        CropBlockState(
                            offset = BlockPos(0, 8, 0),
                            blockState = melonStemState(7)
                        ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 7.59375, 0.0),
                            hashString = "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24",
                        )
                    )
                            +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(0.0, -0.21875, 0.0),
                                    Vec3(0.0, 0.78125, 0.0),
                                    Vec3(0.0, 1.78125, 0.0),
                                    Vec3(0.0, 2.78125, 0.0),
                                    Vec3(0.0, 3.78125, 0.0),
                                    Vec3(0.0, 4.78125, 0.0),
                                    Vec3(0.0, 5.78125, 0.0)
                                ),
                                hashString = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                            ),
                90..90
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0),
                        BlockPos(0, 4, 0),
                        BlockPos(0, 5, 0),
                        BlockPos(0, 6, 0),
                        BlockPos(0, 7, 0)
                    ),
                    blockState = sugarcaneState()
                ) +
                        CropBlockState(
                            offset = BlockPos(0, 8, 0),
                            blockState = melonStemState(6)
                        ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 7.78125, 0.0),
                            hashString = "e3f23b34867472673a484f4baea5f51fbf93abe4d11e2808b6634970150bde24",
                        )
                    )
                            +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(0.0, -0.21875, 0.0),
                                    Vec3(0.0, 0.78125, 0.0),
                                    Vec3(0.0, 1.78125, 0.0),
                                    Vec3(0.0, 2.78125, 0.0),
                                    Vec3(0.0, 3.78125, 0.0),
                                    Vec3(0.0, 4.78125, 0.0),
                                    Vec3(0.0, 5.78125, 0.0)
                                ),
                                hashString = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                            ),
                94..94
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0),
                        BlockPos(0, 4, 0),
                        BlockPos(0, 5, 0),
                        BlockPos(0, 6, 0),
                        BlockPos(0, 7, 0)
                    ),
                    blockState = sugarcaneState()
                ) +
                        CropBlockState(
                            offset = BlockPos(0, 8, 0),
                            blockState = melonStemState(6)
                        ),
                armorStands =
                    CropArmorStand.matcherPattern(
                        listOf(
                            Vec3(0.0, -0.21875, 0.0),
                            Vec3(0.0, 0.78125, 0.0),
                            Vec3(0.0, 1.78125, 0.0),
                            Vec3(0.0, 2.78125, 0.0),
                            Vec3(0.0, 3.78125, 0.0),
                            Vec3(0.0, 4.78125, 0.0),
                            Vec3(0.0, 5.78125, 0.0),
                            Vec3(0.0, 6.78125, 0.0)
                        ),
                        hashString = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                    ),
                95..95
            ),
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0),
                        BlockPos(0, 4, 0),
                        BlockPos(0, 5, 0),
                        BlockPos(0, 6, 0),
                        BlockPos(0, 7, 0),
                        BlockPos(0, 8, 0)
                    ),
                    blockState = sugarcaneState()
                ) +
                        CropBlockState(
                            offset = BlockPos(0, 9, 0),
                            blockState = melonStemState(3)
                        ),
                armorStands =
                    CropArmorStand.matcherPattern(
                        listOf(
                            Vec3(0.0, -0.21875, 0.0),
                            Vec3(0.0, 0.78125, 0.0),
                            Vec3(0.0, 1.78125, 0.0),
                            Vec3(0.0, 2.78125, 0.0),
                            Vec3(0.0, 3.78125, 0.0),
                            Vec3(0.0, 4.78125, 0.0),
                            Vec3(0.0, 5.78125, 0.0),
                            Vec3(0.0, 6.78125, 0.0)
                        ),
                        hashString = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                    ),
                96..96
            ),
            CropStage(
                blocks =             CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 2, 0),
                        BlockPos(0, 3, 0),
                        BlockPos(0, 4, 0),
                        BlockPos(0, 5, 0),
                        BlockPos(0, 6, 0),
                        BlockPos(0, 7, 0),
                        BlockPos(0, 8, 0),
                        BlockPos(0, 9, 0),
                        BlockPos(0, 10, 0)
                    ),
                    blockState = sugarcaneState()
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.0, -0.21875, 0.0),
                        Vec3(0.0, 0.78125, 0.0),
                        Vec3(0.0, 1.78125, 0.0),
                        Vec3(0.0, 2.78125, 0.0),
                        Vec3(0.0, 3.78125, 0.0),
                        Vec3(0.0, 4.78125, 0.0),
                        Vec3(0.0, 5.78125, 0.0),
                        Vec3(0.0, 6.78125, 0.0),
                        Vec3(0.0, 7.78125, 0.0),
                        Vec3(0.0, 8.78125, 0.0)
                    ),
                    rotations = listOf(
                        Rotations(-22.5f, 22.5f, -22.5f),
                        Rotations(-22.5f, 0.0f, 22.5f),
                        Rotations(22.5f, -22.5f, 0.0f),
                        Rotations(-22.5f, 22.5f, -22.5f),
                        Rotations(-22.5f, 0.0f, 22.5f),
                        Rotations(22.5f, -22.5f, 0.0f),
                        Rotations(-22.5f, 22.5f, -22.5f),
                        Rotations(-22.5f, 0.0f, 22.5f),
                        Rotations(22.5f, -22.5f, 0.0f),
                        Rotations(-22.5f, 22.5f, -22.5f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        90.0f,
                        90.0f,
                        90.0f,
                        90.0f,
                        90.0f,
                        90.0f,
                        90.0f,
                        90.0f,
                        90.0f,
                        90.0f
                    ),
                    hashString = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81"
                ),
                120..120
            )



        ),
        maxStage = 120,
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true





    )
}