package org.magic.magicaddons.data.greenhouse

import kotlin.math.abs

import net.minecraft.world.level.block.Blocks

data class GreenhouseLayout(
    val id: String, // plot_# for grids, preset_# for presets
    var name: String? = null,
    val size: Int = 10,
    val slots: List<LayoutSlot> = List(100) { index ->
        val x = index % size
        val y = index / size
        LayoutSlot(
            x,
            y,
            Blocks.AIR.defaultBlockState()
        )
    },
    val elementInstances: MutableList<GreenhouseElementInstance> = mutableListOf(),
){
    private val slotIndex = slots.associateBy { it.x to it.y }
    fun getSlot(x: Int, y: Int) = slotIndex[x to y]
    override fun toString(): String {
        return "${name ?: "unnamed"}: $id"
    }

    fun displayName(): String = name ?: id

    /**
     * The water effects reaching [slot], as a total signed percentage.
     *
     * Only what stands directly beside it counts, which is how the game words every one of them. A
     * crop bigger than one slot reaches from any cell it covers, and never counts itself.
     */
    fun waterEffectAt(slot: LayoutSlot): Int = elementInstances
        .filter { !it.covers(slot) && it.touches(slot) }
        .flatMap { it.cropDef.effects }
        .filter { it.kind == CropEffect.Kind.Water }
        .sumOf { it.percent }

    private fun GreenhouseElementInstance.covers(slot: LayoutSlot): Boolean =
        slot.x in this.slot.x until this.slot.x + cropDef.footprint.width &&
                slot.y in this.slot.y until this.slot.y + cropDef.footprint.height

    /** Whether this plant occupies a cell orthogonally beside [slot]. */
    private fun GreenhouseElementInstance.touches(slot: LayoutSlot): Boolean {
        for (dx in 0 until cropDef.footprint.width) {
            for (dy in 0 until cropDef.footprint.height) {
                val dist = abs(this.slot.x + dx - slot.x) + abs(this.slot.y + dy - slot.y)

                if (dist == 1) return true
            }
        }

        return false
    }
}
