package org.magic.magicaddons.data.greenhouse.plot

import net.minecraft.world.level.block.Block

class LayoutSlot(
    val x: Int,
    val y: Int,
    var soil: Block?,
    var mark: Marking? = null
) {
    enum class Marking(val color: Int) {
        Target(0xFF2DBCF6.toInt()),
        Ingredient(0xFF89F336.toInt())
    }

    override fun toString(): String {
        return "$x,$y block: $soil"
    }
}
