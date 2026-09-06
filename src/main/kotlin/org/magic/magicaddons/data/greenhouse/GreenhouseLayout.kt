package org.magic.magicaddons.data.greenhouse

import kotlin.math.abs


data class GreenhouseLayout(
    val id: String, // plot_# for grids, preset_#[_p#] for presets
    var name: String? = null,
    val size: Int = GREENHOUSE_SIZE,
    val slots: List<LayoutSlot> = List(size * size) { index ->
        val x = index % size
        val y = index / size
        // no soil said: anything may stand there. Air is asked for by placing it from the shelf
        LayoutSlot(x, y, null)
    },
    val elementInstances: MutableList<GreenhouseElementInstance> = mutableListOf(),
){
    private val slotIndex = slots.associateBy { it.x to it.y }
    fun getSlot(x: Int, y: Int) = slotIndex[x to y]

    /** Becomes a copy of [other]: its soil, marks and plants, on this layout's own slots. */
    fun takeContentsFrom(other: GreenhouseLayout) {
        slots.forEach { slot ->
            val theirs = other.getSlot(slot.x, slot.y)
            slot.placedBlock = theirs?.placedBlock
            slot.slotMark = theirs?.slotMark
        }
        elementInstances.clear()
        other.elementInstances.forEach { instance ->
            val slot = getSlot(instance.slot.x, instance.slot.y) ?: return@forEach
            elementInstances.add(instance.copy(slot = slot))
        }
    }
    override fun toString(): String = displayName()

    enum class Kind { PLOT, PRESET }

    /** Whether this is a garden plot or a saved preset. */
    val kind: Kind get() = if (id.startsWith(PLOT_PREFIX)) Kind.PLOT else Kind.PRESET

    /** The plot or preset number in the id, null for a placeholder with none. */
    val number: Int? get() = id.removePrefix(PLOT_PREFIX).removePrefix(PRESET_PREFIX).substringBefore("_p").toIntOrNull()

    /** Which plot of a multi-plot preset this is, counted from 1; null for the first and for garden plots. */
    val part: Int? get() = id.substringAfter("_p", "").toIntOrNull()

    /** The given name, or the plot or preset number when it was never named. */
    fun displayName(): String = name
        ?: part?.let { "Plot $it" }
        ?: number?.let { if (kind == Kind.PLOT) "Plot $it" else "Preset $it" }
        ?: id

    /**
     * The effects a plant has are the ones its neighbours grant: a crop's effects are what it gives
     * away. Spread passes on everything but itself, so a buff travels two plants and no further.
     */
    fun effectsAt(slot: LayoutSlot): Set<CropEffect> {
        // asked for every plant every frame, so the answers are kept until the plants change
        val now = plantsFingerprint()
        if (now != effectsFingerprint) {
            effectsCache.clear()
            effectsFingerprint = now
        }
        return effectsCache.getOrPut(slot.x * SLOT_KEY_STRIDE + slot.y) { computeEffectsAt(slot) }
    }

    private val effectsCache = HashMap<Int, Set<CropEffect>>()
    private var effectsFingerprint: Int = 0

    /** A number that changes whenever a plant is added, removed or moved. */
    private fun plantsFingerprint(): Int {
        var hash = elementInstances.size
        elementInstances.forEach { hash = hash * 31 + (it.slot.x * SLOT_KEY_STRIDE + it.slot.y) * 31 + it.cropDef.name.hashCode() }
        return hash
    }

    private fun computeEffectsAt(slot: LayoutSlot): Set<CropEffect> {
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
    fun waterEffectAt(slot: LayoutSlot): Int = CropEffect.total(effectsAt(slot), CropEffect.Kind.Water)

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

    companion object {
        const val PLOT_PREFIX: String = "plot_"
        const val PRESET_PREFIX: String = "preset_"

        fun plotId(number: Int): String = "$PLOT_PREFIX$number"
        fun presetId(number: Int): String = "$PRESET_PREFIX$number"

        private const val SLOT_KEY_STRIDE: Int = 1024
    }
}
