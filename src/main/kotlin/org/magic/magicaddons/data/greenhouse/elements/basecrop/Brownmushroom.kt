package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Brownmushroom : BaseCrop() {
    override val name: String = "Brown Mushroom"
    override val skyBlockId: SkyBlockId = SkyBlockItemId.item("BROWN_MUSHROOM")

    override val stageDefs: List<CropStage> = listOf(
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:brown_mushroom")
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    Vec3(0.0, -0.75, 0.0),
                    matcher = {
                        it == "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    }
                )
            ),
            stageRange = 1..1
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:brown_mushroom")
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.69, 0.0),
                    matcher = {
                        it == "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    }
                )
            ),
            2..2
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:brown_mushroom")
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.63, 0.0),
                    matcher = {
                        it == "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    }
                )
            ),
            3..3
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:brown_mushroom")
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.57, 0.0),
                    matcher = {
                        it == "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    }
                )
            ),
            4..4
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:brown_mushroom")
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.51, 0.0),
                    matcher = {
                        it == "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
                    }
                )
            ),
            5..5
        ),
        CropStage(
            blocks = listOf(
                CropBlockState(
                    offset = BlockPos(0,1,0),
                    matcher = {
                        it.isBlock("minecraft:brown_mushroom")
                    }
                )
            ),
            armorStands = listOf(
                CropArmorStand(
                    offset = Vec3(0.0, -0.45, 0.0),
                    matcher = {
                        it == "578897b83f51fb96b59ba418ff0868cef7bdf661e315ba5dbac51d876d1d15d"
                    }
                )
            ),
            6..6
        )




    )


    /*
    override val blocks: MutableList<Block> = mutableListOf(
        Blocks.BROWN_MUSHROOM,
    )
    override val standHashes: MutableList<String> = mutableListOf(
        "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
    )
    */
}