package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.Blocks

data class GreenhouseLayout(
    val id: String, // plot_# for grids, preset_# for presets
    var name: String? = null,
    val size: Int = 10,
    val slots: MutableList<GreenhouseSlot> = MutableList(100) { index ->
        val x = index % size
        val y = index / size

        GreenhouseSlot(
            x,
            y,
            Blocks.AIR.defaultBlockState()
        )
    },

    val elementInstances: MutableList<GreenhouseElementInstance> = mutableListOf(),
){
    private val slotIndex = slots.associateBy { it.x to it.y }
    fun getSlot(x: Int, y: Int) = slotIndex[x to y]
}
