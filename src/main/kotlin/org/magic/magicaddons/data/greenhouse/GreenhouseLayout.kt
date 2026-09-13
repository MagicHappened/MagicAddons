package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.Block
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
            // a plain copy keeps the constructor's fields and drops the rest, placed among them
            elementInstances.add(instance.copyForPrediction(slot))
        }
    }
    /** A copy on plants of its own, so nothing done to it reaches this layout. */
    fun copy(): GreenhouseLayout = GreenhouseLayout(id = id, name = name, size = size).also { it.takeContentsFrom(this) }

    /** a copy turned [turns] quarter turns clockwise */
    fun turned(turns: Int): GreenhouseLayout {
        val copy = GreenhouseLayout(id = id, name = name, size = size)

        slots.forEach { slot ->
            val (x, y) = turnedOrigin(slot.x, slot.y, 1, turns)
            copy.getSlot(x, y)?.let {
                it.placedBlock = slot.placedBlock
                it.slotMark = slot.slotMark
            }
        }
        elementInstances.forEach { instance ->
            val (x, y) = turnedOrigin(instance.slot.x, instance.slot.y, instance.cropDef.footprint.width, turns)
            val slot = copy.getSlot(x, y) ?: return@forEach
            copy.elementInstances.add(instance.copyForPrediction(slot))
        }

        return copy
    }

    /** where the corner of a [span] wide square at ([x], [y]) lands after [turns] quarter turns clockwise */
    private fun turnedOrigin(x: Int, y: Int, span: Int, turns: Int): Pair<Int, Int> {
        val last = size - span
        return when (Math.floorMod(turns, 4)) {
            1 -> (last - y) to x
            2 -> (last - x) to (last - y)
            3 -> y to (last - x)
            else -> x to y
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

    /** The plant whose footprint lies over [slot], or null when the cell is bare. */
    fun plantCovering(slot: LayoutSlot): GreenhouseElementInstance? =
        elementInstances.firstOrNull { it.covers(slot) }

    /**
     * Every soil the plant on [slot] grows in, so ground that is already one of them is left alone
     * rather than dug up for the one the preset happens to name. A bare cell takes only what it was
     * given.
     */
    fun soilsAcceptedAt(slot: LayoutSlot): Set<Block> =
        plantCovering(slot)?.everyCrop?.flatMapTo(mutableSetOf()) { it.requiredSoil }.orEmpty()

    /** Every other plant with a cell beside one of [instance]'s, corners included. */
    fun plantsAround(instance: GreenhouseElementInstance): List<GreenhouseElementInstance> =
        elementInstances.filter { other -> other !== instance && other.isAround(instance) }

    private fun GreenhouseElementInstance.isAround(other: GreenhouseElementInstance): Boolean {
        val mine = cropDef.footprint
        val theirs = other.cropDef.footprint

        return slot.x <= other.slot.x + theirs.width && other.slot.x <= slot.x + mine.width &&
                slot.y <= other.slot.y + theirs.height && other.slot.y <= slot.y + mine.height
    }

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
