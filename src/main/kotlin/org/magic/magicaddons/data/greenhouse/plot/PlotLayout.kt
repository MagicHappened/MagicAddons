package org.magic.magicaddons.data.greenhouse.plot

import kotlin.math.abs
import net.minecraft.world.level.block.Block
import org.magic.magicaddons.data.greenhouse.crops.*

data class PlotLayout(
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

    fun copyContentsFrom(other: PlotLayout) {
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

    fun freshCopy(): PlotLayout = PlotLayout(id = id, name = name, size = size).also { it.copyContentsFrom(this) }

    fun turnedBy(quarterTurns: Int): PlotLayout {
        val copy = PlotLayout(id = id, name = name, size = size)

        slots.forEach { slot ->
            val (x, y) = turnedOrigin(slot.x, slot.y, 1, quarterTurns)
            copy.getSlot(x, y)?.let {
                it.soil = slot.soil
                it.mark = slot.mark
            }
        }
        plants.forEach { plant ->
            val footprintWidth = plant.cropDef.footprint.width
            val (x, y) = turnedOrigin(plant.slot.x, plant.slot.y, footprintWidth, quarterTurns)
            val slot = copy.getSlot(x, y) ?: return@forEach

            if (footprintWidth > 1) {
                val (markX, markY) = turnedOrigin(plant.slot.x, plant.slot.y, 1, quarterTurns)
                copy.getSlot(markX, markY)?.mark = null
                slot.mark = plant.slot.mark
            }
            copy.plants.add(plant.copyForPrediction(slot))
        }

        return copy
    }

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

    enum class Kind { PLOT_PRESET, GREENHOUSE_PRESET }

    val kind: Kind get() = if (id.startsWith(PLOT_PREFIX)) Kind.PLOT_PRESET else Kind.GREENHOUSE_PRESET

    val number: Int? get() = id.removePrefix(PLOT_PREFIX).removePrefix(GREENHOUSE_PRESET_PREFIX).substringBefore("_p").toIntOrNull()

    val part: Int? get() = id.substringAfter("_p", "").toIntOrNull()

    fun displayName(): String = name
        ?: part?.let { "Plot $it" }
        ?: number?.let { if (kind == Kind.PLOT_PRESET) "Plot $it" else "Preset $it" }
        ?: id

    fun effectsAt(slot: LayoutSlot): Set<CropEffect> {
        val hashNow = plantsHash()
        if (hashNow != previousPlantHash) {
            effectsCache.clear()
            previousPlantHash = hashNow
        }
        return effectsCache.getOrPut(slot.x * SLOT_KEY_STRIDE + slot.y) { computeEffectsAt(slot) }
    }

    private val effectsCache = HashMap<Int, Set<CropEffect>>()

    private var previousPlantHash: Int = 0

    private fun plantsHash(): Int {
        var hash = plants.size
        plants.forEach { hash = hash * 31 + (it.slot.x * SLOT_KEY_STRIDE + it.slot.y) * 31 + it.cropDef.name.hashCode() }
        return hash
    }

    private fun computeEffectsAt(slot: LayoutSlot): Set<CropEffect> {
        val neighbours = plants.filter { !it.covers(slot) && it.isOrthogonallyBeside(slot) }

        val effects = neighbours.flatMapTo(mutableSetOf()) { it.cropDef.effects }

        neighbours.forEach { neighbour ->
            val neighbourEffects = grantedToSlotBeforeSpread(neighbour.slot)

            if (CropEffect.EffectSpread in neighbourEffects) {
                effects += neighbourEffects - CropEffect.EffectSpread
            }
        }

        return effects
    }

    private fun grantedToSlotBeforeSpread(slot: LayoutSlot): Set<CropEffect> = plants
        .filter { !it.covers(slot) && it.isOrthogonallyBeside(slot) }
        .flatMapTo(mutableSetOf()) { it.cropDef.effects }

    fun waterEffectAt(slot: LayoutSlot): Int = CropEffect.appliedEffect(effectsAt(slot), CropEffect.EffectKind.Water)

    fun plantCovering(slot: LayoutSlot): Plant? =
        plants.firstOrNull { it.covers(slot) }

    fun plantCovering(x: Int, y: Int): Plant? = getSlot(x, y)?.let { plantCovering(it) }

    fun soilsPlantAcceptsAt(slot: LayoutSlot): Set<Block> =
        plantCovering(slot)?.requiredSoils.orEmpty()
    
    fun plantsSurrounding(plant: Plant): List<Plant> =
        plants.filter { other -> other !== plant && other.touchesFootprintOf(plant) }

    private fun Plant.touchesFootprintOf(other: Plant): Boolean {
        val myFootprint = cropDef.footprint
        val otherFootprint = other.cropDef.footprint

        return slot.x <= other.slot.x + otherFootprint.width && other.slot.x <= slot.x + myFootprint.width &&
                slot.y <= other.slot.y + otherFootprint.height && other.slot.y <= slot.y + myFootprint.height
    }

    private fun Plant.isOrthogonallyBeside(slot: LayoutSlot): Boolean =
        coveredCells.any { (cellX, cellY) -> abs(cellX - slot.x) + abs(cellY - slot.y) == 1 }

    companion object {
        const val PLOT_PREFIX: String = "plot_"
        const val GREENHOUSE_PRESET_PREFIX: String = "preset_"

        fun plotId(number: Int): String = "$PLOT_PREFIX$number"
        fun presetId(number: Int): String = "$GREENHOUSE_PRESET_PREFIX$number"

        private const val SLOT_KEY_STRIDE: Int = 1024
    }
}
