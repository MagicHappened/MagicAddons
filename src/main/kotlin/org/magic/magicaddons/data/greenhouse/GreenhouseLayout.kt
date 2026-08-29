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
    /**
     * The effects a plant standing on [slot] has.
     *
     * A crop's own effects are what it gives away rather than what it holds: a cactus keeps its
     * neighbours' water and not its own. So what a plant has is what the plants around it grant,
     * and a plant with nothing beside it has nothing at all.
     *
     * Effect spread is the second helping. A neighbour that has been granted effect spread passes
     * on everything else it has been granted, so a buff can travel two plants from where it came
     * from, but only two: spread never passes on spread itself, which is what keeps a pair of
     * plants from handing the same buffs back and forth forever.
     *
     * A set, so two melons beside one plant are one water retain rather than two. What a plant has
     * is which effects it has, not how many times it was given them.
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
     * How much longer a plant on [slot] holds its water, as a percentage.
     *
     * The pieces are added together, so a hundred percent retain beside a thirty percent drain
     * comes to seventy. Measured rather than assumed: two pumpkins in one greenhouse, both dry,
     * one with both effects and one with only the retain, were given thirteen and seventeen hours
     * by the game itself, and both fall out of a single tick period at seventy and a hundred
     * percent. Either effect winning outright would have given a different pair. See
     * notes/water-formula.md.
     *
     * What that pair cannot separate is whether the drain is added or multiplied in, since both
     * come to thirteen a tick here; a fifty percent retain beside a drain would tell them apart.
     * The yield effects are still guessed at wherever they are eventually added up.
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
