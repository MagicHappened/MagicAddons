package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Chocoberry : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Chocoberry",
        skyblockId = SkyBlockItemId.item("CHOCOBERRY"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.03125, -0.75, 0.21875),
                        hashString = "4478a25a4c5189aa292e6076ae5938cf6c8253b7719e310118fb8312a9b62470",
                    )
                ),
                1..1,
                allowRotation = true
            ),

            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.34375, 0.0),
                        hashString = "4478a25a4c5189aa292e6076ae5938cf6c8253b7719e310118fb8312a9b62470"
                    )
                ),
                2..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        hashString = "4478a25a4c5189aa292e6076ae5938cf6c8253b7719e310118fb8312a9b62470"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.0625, 0.0),
                        hashString = "167bb9880a3ab37435a21b1f135a01a96cca45b49daeb4a1e91baf358e37d89d"
                    )
                ),
                6..6
            )




        ),
        maxStage = 6,
        isMutation = true
    )
}