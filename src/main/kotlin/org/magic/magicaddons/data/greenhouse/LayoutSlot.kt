package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.state.BlockState

class LayoutSlot(
    val x: Int,
    val y: Int,
    var soil: BlockState?,
    var mark: Marking? = null
) {
    /** the colour belongs to the marking, so a role reads the same everywhere */
    enum class Marking(val color: Int) {
        Target(0xFF2DBCF6.toInt()),
        Ingredient(0xFF89F336.toInt())
    }

    override fun toString(): String {
        return "$x,$y block: $soil"
    }
}
