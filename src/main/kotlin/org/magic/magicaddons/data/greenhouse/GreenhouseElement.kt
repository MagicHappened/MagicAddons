package org.magic.magicaddons.data.greenhouse

import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId

data class Footprint(val width: Int, val height: Int)

abstract class GreenhouseElement {
    abstract val name: String
    abstract val SkyBlockId: SkyBlockId?

    open val footprint: Footprint = Footprint(1,1)
    open val requiredSoil: Block = Blocks.FARMLAND



    // abstract val buffs: todo later lol
    val needsWater: Boolean = false
    val isBaseCrop : Boolean = false
    val isMutation : Boolean = false
    val isRareCrop : Boolean = false

}