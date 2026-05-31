package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.state.BlockState

class GreenhouseSlot(
    val x: Int,
    val y: Int,
    var placedBlock: BlockState?,
    var slotMark: Marking? = null
) {
    enum class Marking {
        Target,
        Ingredient,
        UniqueCrop
    }

    override fun toString(): String {
        return "$x,$y block: $placedBlock"
    }
}
