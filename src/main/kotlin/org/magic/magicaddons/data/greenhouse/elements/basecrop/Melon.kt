package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Melon : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Melon",
        skyblockId = SkyBlockItemId.item("MELON"),
        aliases = listOf(SkyBlockItemId.item("MELON_SEEDS")),
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
                        offset = Vec3(0.0, 0.125, 0.0),
                        hashString = "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4"
                    )
                ),
                1..1,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.28125, 0.0),
                        hashString = "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4"
                    )
                ),
                2..2,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "360549bf880605bba628e89b1cca4b8a0e428b61d879f45edd9f45469d87aec4"
                    )
                ),
                3..3,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9"
                    )
                ),
                5..5,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.21875, 0.1875, -0.1875),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.53125, 0.0),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                    )
                ),
                7..7,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.53125, 0.0),
                            hashString = "192600cad8dbec5b6a6ec4dcf9bb4e9cd76190cad80aeee8b047de719cf5e36d",
                        ),
                        CropArmorStand(
                            offset = Vec3(0.1875, 0.1875, -0.21875),
                            hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9",
                        )
                    )
                ,
                8..8,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.0625, -0.46875, 0.0),
                        hashString = "192600cad8dbec5b6a6ec4dcf9bb4e9cd76190cad80aeee8b047de719cf5e36d"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.09375, -0.625, 0.09375),
                        hashString = "afa92dd43afed9e640cf3d3b008ca5199634ec8512de5e1f5eeaecd761296cb9"
                    )
                ),
                9..9,
                allowRotation = true
            ), //todo check other rotations
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0625, -0.46875, 0.0),
                        hashString = "192600cad8dbec5b6a6ec4dcf9bb4e9cd76190cad80aeee8b047de719cf5e36d"
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.09375, -0.625, -0.09375),
                        hashString = "192600cad8dbec5b6a6ec4dcf9bb4e9cd76190cad80aeee8b047de719cf5e36d"
                    )
                ),
                10..10,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands =
                    CropArmorStand.matcherPattern(
                        listOf(
                            Vec3(-0.0625, -0.46875, 0.0),
                            Vec3(0.09375, -0.625, 0.09375)
                        ),
                        listOf(
                            Rotations(0.0f, 0.0f, -22.5f),
                            Rotations(22.5f, 0.0f, 22.5f)
                        ),
                        hashString = "fdfae4b11048bc1ce96ed150134e79f16e2bcaf12d43fa0ff0e27fb2e0852130"
                    )
                ,
                11..11,
                allowRotation = true
            )



        ),
        maxStage = 11,
        isBaseCrop = true

    )
}