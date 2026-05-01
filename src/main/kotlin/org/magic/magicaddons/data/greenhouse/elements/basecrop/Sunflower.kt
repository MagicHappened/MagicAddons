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
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Sunflower : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Sunflower",
        skyblockId = SkyBlockItemId.item("DOUBLE_PLANT"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.1875, 0.25, 0.0),
                        matcher = {
                            it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
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
                            it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
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
                            it == "b40d6fc1e1b67c58d7f82350bcac083f9e9547f9131236463164417fbdd3bee4"
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