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
        // null for no soil, air is an explicit block
        LayoutSlot(x, y, null)
    },
    val plants: MutableList<Plant> = mutableListOf(),
){
    private val slotIndex = slots.associateBy { it.x to it.y }
    fun getSlot(x: Int, y: Int) = slotIndex[x to y]

    fun copyContentsFrom(other: GreenhouseLayout) {
        slots.forEach { slot ->
            val otherSlot = other.getSlot(slot.x, slot.y)
            slot.soil = otherSlot?.soil
            slot.mark = otherSlot?.mark
        }
        plants.clear()
        other.plants.forEach { plant ->
            val slot = getSlot(plant.slot.x, plant.slot.y) ?: return@forEach
            plants.add(plant.copyForPrediction(slot))
        }
    }

    fun isEmpty(): Boolean =
        plants.isEmpty() && slots.all { it.soil == null && it.mark == null }

    fun deepCopy(): GreenhouseLayout = GreenhouseLayout(id = id, name = name, size = size).also { it.copyContentsFrom(this) }

    fun turned(turns: Int): GreenhouseLayout {
        val copy = GreenhouseLayout(id = id, name = name, size = size)

        slots.forEach { slot ->
            val (x, y) = turnedOrigin(slot.x, slot.y, 1, turns)
            copy.getSlot(x, y)?.let {
                it.soil = slot.soil
                it.mark = slot.mark
            }
        }
        plants.forEach { plant ->
            val footprintWidth = plant.cropDef.footprint.width
            val (x, y) = turnedOrigin(plant.slot.x, plant.slot.y, footprintWidth, turns)
            val slot = copy.getSlot(x, y) ?: return@forEach

            // a plant's mark sits on its top left slot, which the turn moved
            if (footprintWidth > 1) {
                val (markX, markY) = turnedOrigin(plant.slot.x, plant.slot.y, 1, turns)
                copy.getSlot(markX, markY)?.mark = null
                slot.mark = plant.slot.mark
            }
            copy.plants.add(plant.copyForPrediction(slot))
        }

        return copy
    }

    /** where the corner of a [span] wide square at ([x], [y]) lands after [turns] quarter turns clockwise */
    private fun turnedOrigin(x: Int, y: Int, span: Int, turns: Int): Pair<Int, Int> {
        val lastOrigin = size - span
        return when (Math.floorMod(turns, 4)) {
            1 -> (lastOrigin - y) to x
            2 -> (lastOrigin - x) to (lastOrigin - y)
            3 -> y to (lastOrigin - x)
            else -> x to y
        }
    }

    override fun toString(): String = displayName()

    enum class Kind { PRESET, MASTER_PRESET }

    val kind: Kind get() = if (id.startsWith(PRESET_PREFIX)) Kind.PRESET else Kind.MASTER_PRESET

    /** null for a placeholder whose id carries no number */
    val number: Int? get() = id.removePrefix(PRESET_PREFIX).removePrefix(MASTER_PRESET_PREFIX).substringBefore("_p").toIntOrNull()

    /** which plot of a multi-plot preset this is, counted from 1; null for the first and for garden plots */
    val part: Int? get() = id.substringAfter("_p", "").toIntOrNull()

    fun displayName(): String = name
        ?: part?.let { "Plot $it" }
        ?: number?.let { if (kind == Kind.PRESET) "Plot $it" else "Preset $it" }
        ?: id

    /** a crop's effects are what it gives its neighbours; spread passes on everything but itself */
    fun effectsAt(slot: LayoutSlot): Set<CropEffect> {
        // kept while the plants are unchanged, since every plant asks every frame
        val now = plantsHash()
        if (now != effectsCacheHash) {
            effectsCache.clear()
            effectsCacheHash = now
        }
        return effectsCache.getOrPut(slot.x * SLOT_KEY_STRIDE + slot.y) { computeEffectsAt(slot) }
    }

    private val effectsCache = HashMap<Int, Set<CropEffect>>()
    private var effectsCacheHash: Int = 0

    private fun plantsHash(): Int {
        var hash = plants.size
        plants.forEach { hash = hash * 31 + (it.slot.x * SLOT_KEY_STRIDE + it.slot.y) * 31 + it.cropDef.name.hashCode() }
        return hash
    }

    private fun computeEffectsAt(slot: LayoutSlot): Set<CropEffect> {
        val neighbours = plants.filter { !it.covers(slot) && it.isOrthogonallyBeside(slot) }

        val effects = neighbours.flatMapTo(mutableSetOf()) { it.cropDef.effects }

        neighbours.forEach { neighbour ->
            val neighbourEffects = grantedTo(neighbour.slot)

            if (CropEffect.EffectSpread in neighbourEffects) {
                effects += neighbourEffects - CropEffect.EffectSpread
            }
        }

        return effects
    }

    /** What the plants around [slot] give it directly, before any of it is spread further. */
    private fun grantedTo(slot: LayoutSlot): Set<CropEffect> = plants
        .filter { !it.covers(slot) && it.isOrthogonallyBeside(slot) }
        .flatMapTo(mutableSetOf()) { it.cropDef.effects }

    fun waterEffectAt(slot: LayoutSlot): Int = CropEffect.appliedEffect(effectsAt(slot), CropEffect.EffectKind.Water)

    fun plantCovering(slot: LayoutSlot): Plant? =
        plants.firstOrNull { it.covers(slot) }

    /** every soil the plant on [slot] can grow in */
    fun soilsAcceptedAt(slot: LayoutSlot): Set<Block> =
        plantCovering(slot)?.acceptedCrops?.flatMapTo(mutableSetOf()) { it.requiredSoil }.orEmpty()
    
    fun plantsSurrounding(plant: Plant): List<Plant> =
        plants.filter { other -> other !== plant && other.touchesFootprintOf(plant) }

    private fun Plant.touchesFootprintOf(other: Plant): Boolean {
        val myFootprint = cropDef.footprint
        val otherFootprint = other.cropDef.footprint

        return slot.x <= other.slot.x + otherFootprint.width && other.slot.x <= slot.x + myFootprint.width &&
                slot.y <= other.slot.y + otherFootprint.height && other.slot.y <= slot.y + myFootprint.height
    }

    private fun Plant.covers(slot: LayoutSlot): Boolean =
        slot.x in this.slot.x until this.slot.x + cropDef.footprint.width &&
                slot.y in this.slot.y until this.slot.y + cropDef.footprint.height

    private fun Plant.isOrthogonallyBeside(slot: LayoutSlot): Boolean {
        for (dx in 0 until cropDef.footprint.width) {
            for (dy in 0 until cropDef.footprint.height) {
                val distance = abs(this.slot.x + dx - slot.x) + abs(this.slot.y + dy - slot.y)

                if (distance == 1) return true
            }
        }

        return false
    }

    companion object {
        const val PRESET_PREFIX: String = "plot_"
        const val MASTER_PRESET_PREFIX: String = "preset_"

        fun plotId(number: Int): String = "$PRESET_PREFIX$number"
        fun presetId(number: Int): String = "$MASTER_PRESET_PREFIX$number"

        private const val SLOT_KEY_STRIDE: Int = 1024
    }
}
