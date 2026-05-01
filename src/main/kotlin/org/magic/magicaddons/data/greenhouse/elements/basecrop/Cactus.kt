package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Cactus : BaseCrop() {
    override val name: String = "Cactus"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("CACTUS")

    override val stageDefs: List<CropStage> = listOf(
        CropStage(
            blocks = listOf(),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.5, 0.0),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                )
            ),
            1..1
        )
        ,
        CropStage(
            blocks = listOf(),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.5, 0.0),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                ),
                CropArmorStand(
                    offset = Vec3(0.0, 0.78125, 0.0),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                )
            ),
            2..2
        ),CropStage(
            blocks = listOf(
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, 0.09375, 0.0),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                ),
                CropArmorStand(
                    offset = Vec3(0.0, -0.5, 0.0),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                )
            ),
            3..3
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0, 1, 0),
                    matcher = {
                        it.isBlock("minecraft:cactus") &&
                                it.getIntProperty("age") == 0
                    }
                ),
                CropBlockState(
                    offset = BlockPos(0, 2, 0),
                    matcher = {
                        it.isBlock("minecraft:cactus") &&
                                it.getIntProperty("age") == 0
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(-0.3125, 1.0, 0.0),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                )
            ),
            6..6
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:cactus") &&
                                it.getIntProperty("age") == 0
                    }
                ),
                CropBlockState(
                    offset = BlockPos(0,2,0),
                    matcher = {
                        it.isBlock("minecraft:cactus") &&
                                it.getIntProperty("age") == 0
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.03125, 2.59375, -0.15625),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                ),
                CropArmorStand(
                    offset = Vec3(0.0, 1.0, 0.3125),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                ),
                CropArmorStand(
                    offset = Vec3(0.0, 1.5, 0.0),
                    matcher = {
                        it == "d4b3ea5cb6b6f046e326621ca11ffb7d6aec22d66c0d81e5039b19ee4400309f"
                    }
                )
            ),
            7..7
        )







    )
}