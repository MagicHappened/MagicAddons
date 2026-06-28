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

    enum class Marking {
        Target,
        Ingredient,
        UniqueCrop
    }

    override fun toString(): String {
        return "$x,$y block: $placedBlock"
    }
}
