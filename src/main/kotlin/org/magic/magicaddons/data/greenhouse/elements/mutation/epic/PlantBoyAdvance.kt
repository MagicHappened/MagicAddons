package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object PlantBoyAdvance : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "PlantBoy Advance",
        skyblockId = SkyBlockItemId.item("PLANTBOY_ADVANCE"),
        stageDefs = listOf(
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
                allowRotation = true
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
                allowRotation = true
            ),

            ),
        maxStage = 12,
        footprint = Footprint(2, 2),
        needsWater = false,
        isMutation = true
    )
}