package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Creambloom : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Creambloom",
        skyblockId = SkyBlockItemId.item("CREAMBLOOM"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, -0.65625, 0.0),
                            hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a",
                        )
                    )
                ,
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.0625, 0.0),
                        hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a"
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "60e98c0b598e2f5ce7c5ee8183ed157c5436a00585da711f8a87f24ea1ff055b"
                    )
                ),
                6..6
            )



        ),
        maxStage = 6,
        isMutation = true
    )
}