package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.sunflowerState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Moonflower : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Moonflower",
        skyblockId = SkyBlockItemId.item("MOONFLOWER"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, 0.25, 0.0),
                        hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                    )
                ),
                1..1,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, -0.5625, 0.0),
                        hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                    )
                ),
                2..2,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, -0.25, 0.0),
                        hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                    )
                ),
                3..3,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(-0.1875, -0.15625, 0.0),
                            hashString = "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9",
                        )
                    )
                ,
                4..4,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(-0.1875, 0.0625, 0.0),
                            hashString = "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb",
                        )
                    )
                ,
                7..7,
                allowRotation = true
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
                        offset = Vec3(0.1875, 0.15625, 0.0),
                        hashString = "7775c5d80efc36c7b029470852aaf161e3733f8ae691fb5ed5450232630e4fcb"
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.5625, -0.1875),
                        hashString = "bd1001840c85349f87c6e20478317f4026b588514d8b1e78241a849d93f9cd94"
                    )
                ),
                15..15,
                allowRotation = true
            )



        ),
        maxStage = 15,
        isBaseCrop = true


    )
}