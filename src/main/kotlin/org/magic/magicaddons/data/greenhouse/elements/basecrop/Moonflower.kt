package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Moonflower : CropDefinitionProvider {
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
                        matcher = {
                            it == "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                        }
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
                        matcher = {
                            it == "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                        }
                    )
                ),
                2..2,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, -0.25, 0.0),
                        matcher = {
                            it == "10ba39f5a3bdb0f3ed6547e6e688fc43d64fabc056f3418b2bbbdfedd7248ba9"
                        }
                    )
                ),
                3..3,
                allowRotation = true
            )


        ),
        isBaseCrop = true


    )
}