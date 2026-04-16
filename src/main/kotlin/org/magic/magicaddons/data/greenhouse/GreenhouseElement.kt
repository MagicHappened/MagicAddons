package org.magic.magicaddons.data.greenhouse

import net.minecraft.block.Block
import net.minecraft.block.Blocks
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId

abstract class GreenhouseElement {
    abstract val name: String
    abstract val SkyBlockId: SkyBlockId?

    val requiredSoil: Block = Blocks.FARMLAND

    // abstract val buffs: todo later lol
    val needsWater: Boolean = false
    val isBaseCrop : Boolean = false
    val isMutation : Boolean = false
    val isRareCrop : Boolean = false


    abstract var inSlot: GHSlot? //todo if gh slot expands to just real world needs to change here

}