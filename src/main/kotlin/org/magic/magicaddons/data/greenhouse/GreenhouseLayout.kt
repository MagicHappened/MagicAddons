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
    override fun toString(): String = displayName()

    fun displayName(): String = name ?: id

    /** The water effects reaching a slot, as a total signed percentage. Only direct neighbours count. */

    /**
     * The effects a plant has are the ones its neighbours grant: a crop's effects are what it gives
     * away. Spread passes on everything but itself, so a buff travels two plants and no further.
     */
    fun effectsAt(slot: LayoutSlot): Set<CropEffect> {
        val neighbours = elementInstances.filter { !it.covers(slot) && it.touches(slot) }

        val granted = neighbours.flatMapTo(mutableSetOf()) { it.cropDef.effects }

        neighbours.forEach { neighbour ->
            val theirs = grantedTo(neighbour.slot)

            if (CropEffect.EffectSpread in theirs) {
                granted += theirs - CropEffect.EffectSpread
            }
        }

        return granted
    }

    /** What the plants around [slot] give it directly, before any of it is spread further. */
    private fun grantedTo(slot: LayoutSlot): Set<CropEffect> = elementInstances
        .filter { !it.covers(slot) && it.touches(slot) }
        .flatMapTo(mutableSetOf()) { it.cropDef.effects }

    /**
     * How much longer a plant holds its water, as a percentage: the pieces are added, drains being
     * negative. Measured rather than assumed, in notes/water-formula.md.
     */
    fun waterEffectAt(slot: LayoutSlot): Int = effectsAt(slot)
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
