package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.state.BlockState

class GreenhouseSlot(
    val x: Int,
    val y: Int,
    var placedBlock: BlockState?,
    var slotMark: Marking? = null
) {
    //todo add marking and add it to codec
    enum class Marking {
        Target,
        Ingredient,
        UniqueCrop
    }




}
