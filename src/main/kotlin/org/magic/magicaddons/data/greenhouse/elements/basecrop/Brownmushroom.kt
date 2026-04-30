package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.BaseCrop
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Brownmushroom : BaseCrop() {
    override val name: String = "Brown Mushroom"
    override val SkyBlockId: SkyBlockId = SkyBlockItemId.item("BROWN_MUSHROOM")



    /*
    override val blocks: MutableList<Block> = mutableListOf(
        Blocks.BROWN_MUSHROOM,
    )
    override val standHashes: MutableList<String> = mutableListOf(
        "7019992b5d440f85d2b05148aa9b85f450985d5f16ae960d1cdb32e06e3c896f"
    )
    */
}