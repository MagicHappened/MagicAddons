package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.state.BlockState

class LayoutSlot(
    val x: Int,
    val y: Int,
    var placedBlock: BlockState?,
    var slotMark: Marking? = null
) {
    fun isCoordsEqual(other: LayoutSlot): Boolean {
        return x == other.x && y == other.y
    }

    /**
     * What a slot is for in a layout. [color] belongs to the marking rather than to whichever
     * widget happens to draw it, so a role reads as the same colour everywhere in the ui.
     */
    enum class Marking(val color: Int) {
        Target(0xFF2DBCF6.toInt()),
        Ingredient(0xFF89F336.toInt()),
        UniqueCrop(0xFFBB00BB.toInt())
    }

    override fun toString(): String {
        return "$x,$y block: $placedBlock"
    }
}
