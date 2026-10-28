package org.magic.magicaddons.data.greenhouse.plot

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.DecayOutlook
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.PlantStage
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.DeadPlant
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Lonelily

object PlotPrediction {

    class MutationChance(val crop: CropDefinition, val chance: Double)

    private const val MIN_ROLL_WEIGHT_TOTAL: Double = 100.0

    private const val FULL_CHANCE_RING_SHARE: Double = 0.5

    fun mutationChancesAtSlot(
        layout: PlotLayout,
        x: Int,
        y: Int,
        weightMultiplier: Double,
        ignoredPlant: Plant? = null
    ): List<MutationChance> {
        val eligibleCrops = spawningCrops.filter { missingSpawnConditions(layout, it, x, y, ignoredPlant).isEmpty() }
        return chancesAmong(eligibleCrops, layout, x, y, weightMultiplier, ignoredPlant)
    }

    private val spawningCrops: List<CropDefinition> get() = CropRegistry.allCrops.filter { (it.spawnRule?.weight ?: 0) > 0 }

    private fun chancesAmong(
        eligibleCrops: List<CropDefinition>,
        layout: PlotLayout,
        x: Int,
        y: Int,
        weightMultiplier: Double,
        ignoredPlant: Plant?
    ): List<MutationChance> {
        val weightByCrop = eligibleCrops.associateWith { it.spawnRule!!.weight * weightMultiplier }
        val rollTotal = max(MIN_ROLL_WEIGHT_TOTAL, weightByCrop.values.sum())

        return weightByCrop
            .map { (crop, weight) -> MutationChance(crop, weight / rollTotal * ringFillFactor(layout, crop, x, y, ignoredPlant)) }
            .sortedByDescending { it.chance }
    }

    class AbsenceSpawns(val onTargets: Double, val otherOnTargets: Double)

    private const val ABSENCE_SIMULATION_RUNS: Int = 100

    const val MAX_COUNTED_SPAWNS: Int = GREENHOUSE_SIZE * GREENHOUSE_SIZE

    const val LIKELY_SPAWNS_HORIZON_TICKS: Int = 150

    private const val SPAWN_CHANCE_RUNS: Int = 10_000

    private const val STORED_CHANCE_DECIMALS: Double = 10_000.0

    private const val EMPTY_CELL: Int = -1

    const val NEVER_TICK: Int = Int.MAX_VALUE

    private const val LARGEST_FOOTPRINT: Int = 3

    private val neverPlannedCrops: Set<CropDefinition> = setOf(Lonelily.definition)

    fun expectedSpawnsDuringAbsence(
        layoutBefore: PlotLayout,
        targets: List<Plant>,
        ticks: Int,
        weightMultiplier: Double,
        random: Random = Random.Default
    ): AbsenceSpawns {
        val targetsByOrigin = targets.associateBy { it.slot.x to it.slot.y }
        val simulation = spawnSimulationOf(layoutBefore, weightMultiplier, nextTickInMs = null, tickMs = null) { _, _, _ -> false }
        var spawnsOnTargets = 0
        var otherSpawnsOnTargets = 0

        repeat(ABSENCE_SIMULATION_RUNS) {
            simulation.run(random, ticks) { _, spawnable, x, y ->
                val target = targetsByOrigin[x to y] ?: return@run
                if (target.acceptsCrop(simulation.cropOf(spawnable))) spawnsOnTargets++ else otherSpawnsOnTargets++
            }
        }
        return AbsenceSpawns(spawnsOnTargets.toDouble() / ABSENCE_SIMULATION_RUNS, otherSpawnsOnTargets.toDouble() / ABSENCE_SIMULATION_RUNS)
    }

    fun ticksUntil(remainingMs: Long?, nextTickInMs: Long?, tickMs: Long?): Int {
        if (remainingMs == null || nextTickInMs == null || tickMs == null || tickMs <= 0) return NEVER_TICK
        if (remainingMs <= nextTickInMs) return 0

        return ((remainingMs - nextTickInMs + tickMs - 1) / tickMs).coerceAtMost(NEVER_TICK.toLong()).toInt()
    }

    class SpawnChances(val chancesByTick: List<DoubleArray>, val crops: List<String>) {

        fun chanceOfAtLeast(count: Int, ticksPassed: Int): Double =
            chancesByTick[ticksPassed.coerceIn(0, chancesByTick.lastIndex)].getOrElse(count - 1) { 0.0 }

        fun chancesOfAtLeast(count: Int): List<Double> = chancesByTick.map { rounded(it.getOrElse(count - 1) { 0.0 }) }

        fun countReachedWith(confidence: Double, ticksPassed: Int): Int =
            chancesByTick[ticksPassed.coerceIn(0, chancesByTick.lastIndex)].count { it >= confidence }

        fun countsReachedWith(confidence: Double): List<Int> = chancesByTick.indices.map { countReachedWith(confidence, it) }

        fun storedRows(): List<List<Double>> = chancesByTick.map { row -> row.map(::rounded).dropLastWhile { it == 0.0 } }

        private fun rounded(chance: Double): Double = Math.round(chance * STORED_CHANCE_DECIMALS) / STORED_CHANCE_DECIMALS

        companion object {
            fun fromStoredRows(rows: List<List<Double>>, crops: List<String>): SpawnChances? {
                if (rows.isEmpty()) return null
                return SpawnChances(rows.map { row -> DoubleArray(MAX_COUNTED_SPAWNS) { row.getOrElse(it) { 0.0 } } }, crops)
            }
        }
    }

    internal class SpawnableCrop(
        val crop: CropDefinition,
        val id: Int,
        val weight: Double,
        val needsNoNeighbours: Boolean,
        val recipeIds: IntArray,
        val recipeCounts: IntArray,
        val originOk: BooleanArray,
        val haltTicksAfterSpawn: IntArray?,
        val counted: BooleanArray
    ) {
        val width: Int = crop.footprint.width
        val height: Int = crop.footprint.height
        val ringSize: Int = (width + 2) * (height + 2) - width * height
    }

    fun spawnSimulationOf(
        layout: PlotLayout,
        weightMultiplier: Double,
        nextTickInMs: Long?,
        tickMs: Long?,
        countsSpawn: (CropDefinition, Int, Int) -> Boolean
    ): SpawnSimulation {
        val size = layout.size
        val tileCount = size * size
        val idByCropName = linkedMapOf<String, Int>()
        fun idOf(cropName: String): Int = idByCropName.getOrPut(cropName) { idByCropName.size }
        val deadPlantId = idOf(DeadPlant.definition.name)

        val occupants = IntArray(tileCount) { EMPTY_CELL }
        val contributesUntil = IntArray(tileCount)
        val decaysAt = IntArray(tileCount) { NEVER_TICK }
        layout.plants.filter { isStanding(it, null) }.forEach { plant ->
            val id = idOf(plant.cropDef.name)
            val decayTick = if (layout.decayOutlookOf(plant).canDecay) ticksUntil(plant.decayRemainingMs, nextTickInMs, tickMs) else NEVER_TICK
            val haltTick = when {
                plant.isHalted -> 0
                else -> plant.ticksUntilWaterHalt(GreenhouseGrid.waterEffectAt(layout, plant.slot))?.let { (it - 1).coerceAtLeast(0) } ?: NEVER_TICK
            }
            plant.coveredCells.forEach { (x, y) ->
                if (x !in 0 until size || y !in 0 until size) return@forEach
                val cell = y * size + x
                occupants[cell] = id
                decaysAt[cell] = decayTick
                contributesUntil[cell] = minOf(decayTick, haltTick)
            }
        }

        val spawnables = spawningCrops
            .sortedWith(compareBy({ it.footprint.width }, { it.footprint.height }))
            .map { crop ->
                val rule = crop.spawnRule!!
                val width = crop.footprint.width
                val height = crop.footprint.height
                val originOk = BooleanArray(tileCount) { cell ->
                    val x = cell % size
                    val y = cell / size
                    x + width <= size && y + height <= size &&
                            !(rule.staysOffPlotEdge && (x == 0 || y == 0 || x + width == size || y + height == size)) &&
                            crop.footprint.cellsFrom(x, y).all { (cellX, cellY) -> layout.getSlot(cellX, cellY)?.soil?.let { it in crop.requiredSoil } == true } &&
                            (!rule.needsOwnEffectsAround || effectsReceivedAround(layout, crop, x, y, null).containsAll(crop.effects))
                }
                val isCountable = crop.maxStage == 1 && crop !in neverPlannedCrops
                SpawnableCrop(
                    crop = crop,
                    id = idOf(crop.name),
                    weight = rule.weight * weightMultiplier,
                    needsNoNeighbours = rule.needsNoNeighbours,
                    recipeIds = rule.requiredNeighbourCells.keys.map(::idOf).toIntArray(),
                    recipeCounts = rule.requiredNeighbourCells.values.toIntArray(),
                    originOk = originOk,
                    haltTicksAfterSpawn = if (!crop.needsWater) null else IntArray(tileCount) { cell ->
                        val slot = layout.getSlot(cell % size, cell / size) ?: return@IntArray NEVER_TICK
                        ticksUntilHalt(0.0, GreenhouseGrid.waterEffectAt(layout, slot)) ?: NEVER_TICK
                    },
                    counted = BooleanArray(tileCount) { cell -> isCountable && originOk[cell] && countsSpawn(crop, cell % size, cell / size) }
                )
            }
        return SpawnSimulation(size, idByCropName.size, deadPlantId, spawnables, occupants, contributesUntil, decaysAt)
    }

    class SpawnSimulation internal constructor(
        private val size: Int,
        cropCount: Int,
        private val deadPlantId: Int,
        private val spawnables: List<SpawnableCrop>,
        private val occupants: IntArray,
        private val contributesUntil: IntArray,
        private val decaysAt: IntArray
    ) {
        private val tileCount = size * size

        val fingerprint: Int = listOf(
            occupants.contentHashCode(),
            contributesUntil.contentHashCode(),
            decaysAt.contentHashCode(),
            spawnables.map { it.weight }.hashCode(),
            spawnables.map { it.originOk.contentHashCode() }.hashCode(),
            spawnables.map { it.counted.contentHashCode() }.hashCode(),
            spawnables.map { it.haltTicksAfterSpawn?.contentHashCode() ?: 0 }.hashCode()
        ).fold(0) { hash, part -> 31 * hash + part }

        private val cells = IntArray(tileCount)
        private val until = IntArray(tileCount)
        private val dirty = BooleanArray(tileCount)
        private val eligibleCount = IntArray(tileCount)
        private val eligibleCrop = IntArray(tileCount * spawnables.size)
        private val eligibleChance = DoubleArray(tileCount * spawnables.size)
        private val ringCounts = IntArray(cropCount)
        private val spawnCells = IntArray(tileCount)
        private val spawnCrops = IntArray(tileCount)
        private var events: Array<MutableList<Int>> = emptyArray()

        private val startEligibleCount: IntArray
        private val startEligibleCrop: IntArray
        private val startEligibleChance: DoubleArray

        init {
            occupants.copyInto(cells)
            contributesUntil.copyInto(until)
            for (cell in 0 until tileCount) if (cells[cell] == EMPTY_CELL) refreshEligibility(cell, 0)
            startEligibleCount = eligibleCount.copyOf()
            startEligibleCrop = eligibleCrop.copyOf()
            startEligibleChance = eligibleChance.copyOf()
        }

        fun cropOf(spawnable: Int): CropDefinition = spawnables[spawnable].crop

        fun spawnChancesByTick(random: Random, isCancelled: () -> Boolean): SpawnChances? {
            val horizon = LIKELY_SPAWNS_HORIZON_TICKS
            val hits = Array(horizon + 1) { IntArray(MAX_COUNTED_SPAWNS) }
            val spawnsAtTick = IntArray(horizon)
            val spawnsBySpawnable = IntArray(spawnables.size)

            repeat(SPAWN_CHANCE_RUNS) {
                if (isCancelled()) return null
                spawnsAtTick.fill(0)
                run(random, horizon) { tick, spawnable, x, y ->
                    if (!spawnables[spawnable].counted[y * size + x]) return@run
                    spawnsAtTick[tick]++
                    spawnsBySpawnable[spawnable]++
                }
                var spawned = 0
                for (tick in 0 until horizon) {
                    spawned += spawnsAtTick[tick]
                    val row = hits[tick + 1]
                    for (index in 0 until min(spawned, MAX_COUNTED_SPAWNS)) row[index]++
                }
            }
            val chancesByTick = hits.map { row -> DoubleArray(MAX_COUNTED_SPAWNS) { row[it].toDouble() / SPAWN_CHANCE_RUNS } }
            val crops = spawnables.indices.filter { spawnsBySpawnable[it] > 0 }.sortedByDescending { spawnsBySpawnable[it] }.map { spawnables[it].crop.name }
            return SpawnChances(chancesByTick, crops)
        }

        fun run(random: Random, horizon: Int, onSpawn: (tick: Int, spawnable: Int, x: Int, y: Int) -> Unit) {
            occupants.copyInto(cells)
            contributesUntil.copyInto(until)
            startEligibleCount.copyInto(eligibleCount)
            startEligibleCrop.copyInto(eligibleCrop)
            startEligibleChance.copyInto(eligibleChance)
            dirty.fill(false)
            if (events.size < horizon) events = Array(horizon) { mutableListOf() } else events.forEach { it.clear() }
            for (cell in 0 until tileCount) {
                if (decaysAt[cell] < horizon) events[decaysAt[cell]].add(cell)
                if (contributesUntil[cell] in 1 until horizon && contributesUntil[cell] != decaysAt[cell]) events[contributesUntil[cell]].add(cell)
            }

            for (tick in 0 until horizon) {
                for (cell in events[tick]) {
                    if (decaysAt[cell] == tick && cells[cell] == occupants[cell]) {
                        cells[cell] = if (cells[cell] == deadPlantId) EMPTY_CELL else deadPlantId
                        until[cell] = NEVER_TICK
                    }
                    markDirtyAround(cell)
                }

                var spawnCount = 0
                for (cell in 0 until tileCount) {
                    if (cells[cell] != EMPTY_CELL) continue
                    if (dirty[cell]) {
                        refreshEligibility(cell, tick)
                        dirty[cell] = false
                    }
                    val count = eligibleCount[cell]
                    if (count == 0) continue
                    var roll = random.nextDouble()
                    val base = cell * spawnables.size
                    for (index in 0 until count) {
                        roll -= eligibleChance[base + index]
                        if (roll < 0) {
                            spawnCells[spawnCount] = cell
                            spawnCrops[spawnCount] = eligibleCrop[base + index]
                            spawnCount++
                            break
                        }
                    }
                }

                for (index in 0 until spawnCount) {
                    val origin = spawnCells[index]
                    val spawnable = spawnables[spawnCrops[index]]
                    if (!isFootprintEmpty(origin, spawnable)) continue
                    val haltTicks = spawnable.haltTicksAfterSpawn?.get(origin) ?: NEVER_TICK
                    val contributesUntilTick = if (haltTicks == NEVER_TICK) NEVER_TICK else tick + haltTicks
                    val x = origin % size
                    val y = origin / size
                    for (dy in 0 until spawnable.height) {
                        for (dx in 0 until spawnable.width) {
                            val cell = (y + dy) * size + x + dx
                            cells[cell] = spawnable.id
                            until[cell] = contributesUntilTick
                            if (contributesUntilTick < horizon) events[contributesUntilTick].add(cell)
                            markDirtyAround(cell)
                        }
                    }
                    onSpawn(tick, spawnCrops[index], x, y)
                }
            }
        }

        private fun isFootprintEmpty(origin: Int, spawnable: SpawnableCrop): Boolean {
            val x = origin % size
            val y = origin / size
            for (dy in 0 until spawnable.height) {
                for (dx in 0 until spawnable.width) {
                    if (cells[(y + dy) * size + x + dx] != EMPTY_CELL) return false
                }
            }
            return true
        }

        private fun markDirtyAround(cell: Int) {
            val x = cell % size
            val y = cell / size
            for (originY in (y - LARGEST_FOOTPRINT).coerceAtLeast(0)..(y + 1).coerceAtMost(size - 1)) {
                for (originX in (x - LARGEST_FOOTPRINT).coerceAtLeast(0)..(x + 1).coerceAtMost(size - 1)) {
                    dirty[originY * size + originX] = true
                }
            }
        }

        private fun refreshEligibility(cell: Int, tick: Int) {
            val x = cell % size
            val y = cell / size
            val base = cell * spawnables.size
            var count = 0
            var weightSum = 0.0
            var ringWidth = -1
            var ringHeight = -1
            var contributing = 0

            for ((index, spawnable) in spawnables.withIndex()) {
                if (!spawnable.originOk[cell] || !isFootprintEmpty(cell, spawnable)) continue
                if (spawnable.width != ringWidth || spawnable.height != ringHeight) {
                    ringWidth = spawnable.width
                    ringHeight = spawnable.height
                    contributing = countRing(x, y, ringWidth, ringHeight, tick)
                }
                if (spawnable.needsNoNeighbours && contributing > 0) continue
                var hasRecipe = true
                for (part in spawnable.recipeIds.indices) {
                    if (ringCounts[spawnable.recipeIds[part]] < spawnable.recipeCounts[part]) {
                        hasRecipe = false
                        break
                    }
                }
                if (!hasRecipe) continue
                val fill = if (spawnable.recipeIds.isEmpty()) 1.0 else min(1.0, contributing / (spawnable.ringSize * FULL_CHANCE_RING_SHARE))
                eligibleCrop[base + count] = index
                eligibleChance[base + count] = spawnable.weight * fill
                weightSum += spawnable.weight
                count++
            }
            val rollTotal = max(MIN_ROLL_WEIGHT_TOTAL, weightSum)
            for (index in 0 until count) eligibleChance[base + index] /= rollTotal
            eligibleCount[cell] = count
        }

        private fun countRing(x: Int, y: Int, width: Int, height: Int, tick: Int): Int {
            ringCounts.fill(0)
            var contributing = 0
            for (dy in -1..height) {
                for (dx in -1..width) {
                    if (dx in 0 until width && dy in 0 until height) continue
                    val ringX = x + dx
                    val ringY = y + dy
                    if (ringX !in 0 until size || ringY !in 0 until size) continue
                    val cell = ringY * size + ringX
                    val id = cells[cell]
                    if (id == EMPTY_CELL || until[cell] <= tick) continue
                    ringCounts[id]++
                    contributing++
                }
            }
            return contributing
        }
    }

    // hypixel multiplies mutation chances by the amount of crops around the spawn cell, reaching the actual chance at 4 (25% per crop)
    private fun ringFillFactor(layout: PlotLayout, crop: CropDefinition, x: Int, y: Int, ignoredPlant: Plant?): Double {
        if (crop.spawnRule?.requiredNeighbourCells.isNullOrEmpty()) return 1.0

        val fullRingSize = (crop.footprint.width + 2) * (crop.footprint.height + 2) - crop.footprint.width * crop.footprint.height
        val plantedRingCells = cellsSurrounding(layout, crop, x, y).count { (cellX, cellY) -> contributingPlantAtPos(layout, cellX, cellY, ignoredPlant) != null }
        return min(1.0, plantedRingCells / (fullRingSize * FULL_CHANCE_RING_SHARE))
    }

    fun missingSpawnConditions(
        layout: PlotLayout,
        crop: CropDefinition,
        x: Int,
        y: Int,
        ignoredPlant: Plant? = null
    ): List<String> {
        val rule = crop.spawnRule ?: return listOf("never appears on its own")
        if (!fitsInPlot(layout, crop, x, y)) return listOf(DOES_NOT_FIT)

        val missing = missingFootprintConditions(layout, crop, x, y, ignoredPlant).toMutableList()

        missing += missingNeighbourConditions(layout, crop, x, y, ignoredPlant)

        if (rule.needsOwnEffectsAround) {
            val missingEffects = crop.effects - effectsReceivedAround(layout, crop, x, y, ignoredPlant)
            if (missingEffects.isNotEmpty()) missing += "no ${missingEffects.joinToString(", ") { it.label }} around it"
        }

        return missing
    }

    private const val DOES_NOT_FIT: String = "does not fit here"

    private fun fitsInPlot(layout: PlotLayout, crop: CropDefinition, x: Int, y: Int): Boolean =
        x >= 0 && y >= 0 && x + crop.footprint.width <= layout.size && y + crop.footprint.height <= layout.size

    private fun missingFootprintConditions(layout: PlotLayout, crop: CropDefinition, x: Int, y: Int, ignoredPlant: Plant?): List<String> {
        if (!fitsInPlot(layout, crop, x, y)) return listOf(DOES_NOT_FIT)

        val footprintCells = crop.footprint.cellsFrom(x, y)
        val missing = mutableListOf<String>()

        if (crop.spawnRule?.staysOffPlotEdge == true &&
            (x == 0 || y == 0 || x + crop.footprint.width == layout.size || y + crop.footprint.height == layout.size)
        ) {
            missing += "touches the plot edge"
        }

        if (footprintCells.any { (cellX, cellY) -> standingPlantAtPos(layout, cellX, cellY, ignoredPlant) != null }) missing += "no room"

        val footprintSoils = footprintCells.map { (cellX, cellY) -> layout.getSlot(cellX, cellY)?.soil }
        if (footprintSoils.any { it == null || it !in crop.requiredSoil }) {
            missing += "needs ${crop.requiredSoil.joinToString(" or ") { it.name.string }}"
        }
        return missing
    }

    fun missingNeighbourConditions(
        layout: PlotLayout,
        crop: CropDefinition,
        x: Int,
        y: Int,
        ignoredPlant: Plant? = null
    ): List<String> {
        val rule = crop.spawnRule ?: return emptyList()
        val surroundingCellsByCrop = cellsSurrounding(layout, crop, x, y).mapNotNull { (cellX, cellY) -> contributingPlantAtPos(layout, cellX, cellY, ignoredPlant) }
            .groupingBy { it.cropDef.name }
            .eachCount()

        val missing = mutableListOf<String>()
        if (rule.needsNoNeighbours && surroundingCellsByCrop.isNotEmpty()) missing += "needs no crop around it"

        rule.requiredNeighbourCells.forEach { (neighbourCrop, requiredCells) ->
            val missingCount = requiredCells - (surroundingCellsByCrop[neighbourCrop] ?: 0)
            if (missingCount > 0) missing += "$missingCount more $neighbourCrop"
        }
        return missing
    }

    private fun cellsSurrounding(layout: PlotLayout, crop: CropDefinition, x: Int, y: Int): List<Pair<Int, Int>> {
        val footprintCells = crop.footprint.cellsFrom(x, y)
        return ((x - 1)..(x + crop.footprint.width)).flatMap { cellX -> ((y - 1)..(y + crop.footprint.height)).map { cellY -> cellX to cellY } }
            .filter { (cellX, cellY) -> (cellX to cellY) !in footprintCells && cellX in 0 until layout.size && cellY in 0 until layout.size }
    }

    private fun effectsReceivedAround(layout: PlotLayout, crop: CropDefinition, x: Int, y: Int, ignoredPlant: Plant?): Set<CropEffect> {
        val standingLayout = layoutOfStandingPlants(layout, ignoredPlant)
        return cellsSurrounding(layout, crop, x, y)
            .mapNotNull { (cellX, cellY) -> standingLayout.plantCovering(cellX, cellY)?.takeUnless { it.isHalted } }
            .distinct()
            .flatMapTo(mutableSetOf()) { standingLayout.effectsReceivedBy(it) }
    }

    private fun layoutOfStandingPlants(layout: PlotLayout, ignoredPlant: Plant?): PlotLayout {
        val absentPlantSlots = layout.plants.filterNot { isStanding(it, ignoredPlant) }.map { it.slot.x to it.slot.y }
        if (absentPlantSlots.isEmpty()) return layout
        return layout.freshCopy().also { copy -> copy.plants.removeAll { (it.slot.x to it.slot.y) in absentPlantSlots } }
    }

    private fun standingPlantAtPos(layout: PlotLayout, cellX: Int, cellY: Int, ignoredPlant: Plant?): Plant? =
        layout.plantCovering(cellX, cellY)?.takeIf { isStanding(it, ignoredPlant) }

    private fun contributingPlantAtPos(layout: PlotLayout, cellX: Int, cellY: Int, ignoredPlant: Plant?): Plant? =
        standingPlantAtPos(layout, cellX, cellY, ignoredPlant)?.takeUnless { it.isHalted }

    fun plantsContributingTo(layout: PlotLayout, crop: CropDefinition, x: Int, y: Int, ignoredPlant: Plant? = null): Set<Plant> {
        val recipe = crop.spawnRule?.requiredNeighbourCells.orEmpty()

        return cellsSurrounding(layout, crop, x, y)
            .mapNotNull { (cellX, cellY) -> contributingPlantAtPos(layout, cellX, cellY, ignoredPlant) }
            .filterTo(mutableSetOf()) { recipe.isEmpty() || it.cropDef.name in recipe }
    }

    class CreditDraw(val credited: Int, val candidates: Set<Plant>)

    class SpawnCredit(val certain: Set<Plant>, val draws: List<CreditDraw>) {
        val uncertain: Set<Plant> get() = draws.flatMapTo(HashSet()) { it.candidates }
    }

    fun spawnCreditOf(layout: PlotLayout, crop: CropDefinition, x: Int, y: Int, ignoredPlant: Plant? = null): SpawnCredit {
        val recipe = crop.spawnRule?.requiredNeighbourCells.orEmpty()
        val contributingCells = cellsSurrounding(layout, crop, x, y).mapNotNull { (cellX, cellY) -> contributingPlantAtPos(layout, cellX, cellY, ignoredPlant) }
        if (recipe.isEmpty()) return SpawnCredit(contributingCells.toSet(), emptyList())

        val (exactlyNeeded, moreThanNeeded) = contributingCells
            .filter { it.cropDef.name in recipe }
            .groupBy { it.cropDef.name }
            .entries.partition { (cropName, cells) -> cells.size <= recipe.getValue(cropName) }
        return SpawnCredit(
            exactlyNeeded.flatMap { it.value }.toSet(),
            moreThanNeeded.map { (cropName, cells) -> CreditDraw(recipe.getValue(cropName), cells.toSet()) }
        )
    }

    class HelpedSpawnSpots(val count: Int, val hasTeleportingMutation: Boolean)

    fun spawnSpotsHelpedByPlant(layout: PlotLayout): Map<Plant, HelpedSpawnSpots> {
        val spotCountByPlant = HashMap<Plant, Int>()
        val plantsHelpingTeleporters = HashSet<Plant>()

        for (y in 0 until layout.size) {
            for (x in 0 until layout.size) {
                val plantOnCell = layout.plantCovering(x, y)
                val plantsHelpingHere = HashSet<Plant>()

                spawningCrops.forEach { crop ->
                    val growingTeleporter = plantOnCell?.takeIf { crop.teleportsWhileGrowing && it.cropDef == crop && !it.isFullyGrown }
                    val spawnHoldingItsSpot = plantOnCell?.takeIf { !crop.teleportsWhileGrowing && it.cropDef == crop && it.slot.x == x && it.slot.y == y }
                    val reopeningOccupant = growingTeleporter ?: spawnHoldingItsSpot
                    if (plantOnCell != null && reopeningOccupant == null) return@forEach
                    if (missingSpawnConditions(layout, crop, x, y, ignoredPlant = reopeningOccupant).isNotEmpty()) return@forEach

                    val contributors = plantsContributingTo(layout, crop, x, y, reopeningOccupant)
                    if (crop.teleportsWhileGrowing) plantsHelpingTeleporters += contributors else plantsHelpingHere += contributors
                }
                plantsHelpingHere.forEach { spotCountByPlant.merge(it, 1, Int::plus) }
            }
        }
        return layout.plants.associateWith { HelpedSpawnSpots(spotCountByPlant[it] ?: 0, it in plantsHelpingTeleporters) }
    }

    fun decayOutlookOf(layout: PlotLayout, plant: Plant, helpedSpotsOf: (Plant) -> HelpedSpawnSpots?): DecayOutlook {
        if (plant.cropDef.minMutationsBeforeDecay == null || plant.timesMutated == 0) return ownDecayOutlookOf(plant, helpedSpotsOf(plant))

        val minimum = plant.cropDef.minMutationsBeforeDecay
        val sameCrop = layout.plants.filter { it.cropDef == plant.cropDef }
        val pool = sameCrop.filter { it.timesMutated >= 1 }
        val combinedReadThisTick = sameCrop
            .mapNotNull { member -> member.lastDiagnosis?.takeIf { it.combinedMutatesRemaining != null && it.isFromCurrentTick } }
            .maxByOrNull { it.diagnosedAt }
            ?.combinedMutatesRemaining
        val mutationsLeft = combinedReadThisTick ?: TimesMutatedCounter.combinedMutatesRemainingOf(pool.map { it.timesMutated }, minimum)
        val isMutationsLeftMinimum = combinedReadThisTick == null && pool.any { it.isTimesMutatedMinimum }
        val mayJoinPool = combinedReadThisTick == null && sameCrop.any { it.timesMutated == 0 && it.isTimesMutatedMinimum }

        val helpedByMember = pool.map { helpedSpotsOf(it) }
        val helpsTeleporter = helpedByMember.any { it?.hasTeleportingMutation == true }
        val spotsAcrossPool = helpedByMember.sumOf { it?.count ?: 0 }

        val kind = when {
            mutationsLeft == 0 && !mayJoinPool -> DecayOutlook.Kind.OnTime
            mutationsLeft > 0 && !isMutationsLeftMinimum && !helpsTeleporter && spotsAcrossPool == 0 -> DecayOutlook.Kind.Never
            isMutationsLeftMinimum || mayJoinPool || helpsTeleporter -> DecayOutlook.Kind.AfterUncountedSpawns
            else -> DecayOutlook.Kind.AfterSpawns
        }
        val isReachable = isMutationsLeftMinimum || mayJoinPool || helpsTeleporter || mutationsLeft <= spotsAcrossPool
        return DecayOutlook(kind, mutationsLeft, helpedSpotsOf(plant)?.count ?: 0, pool.size, isMutationsLeftMinimum, isReachable)
    }

    private fun ownDecayOutlookOf(plant: Plant, helpedSpots: HelpedSpawnSpots?): DecayOutlook {
        val mutationsLeft = plant.mutationsLeftToSpawn
        val spotCount = helpedSpots?.count ?: 0
        val helpsTeleporter = helpedSpots?.hasTeleportingMutation == true

        val kind = when {
            mutationsLeft == 0 -> DecayOutlook.Kind.OnTime
            plant.isTimesMutatedMinimum || helpsTeleporter -> DecayOutlook.Kind.AfterUncountedSpawns
            spotCount > 0 -> DecayOutlook.Kind.AfterSpawns
            else -> DecayOutlook.Kind.Never
        }
        val isReachable = plant.isTimesMutatedMinimum || helpsTeleporter || mutationsLeft <= spotCount
        return DecayOutlook(kind, mutationsLeft, spotCount, isMutationsLeftMinimum = plant.isTimesMutatedMinimum, isReachableFromCurrentSpots = isReachable)
    }

    private fun isStanding(plant: Plant, ignoredPlant: Plant?): Boolean =
        plant !== ignoredPlant && !(plant.slot.mark == LayoutSlot.Marking.Target && plant.growthStage == null)

    fun unplannedMutationSpots(
        layout: PlotLayout,
        plannedCropsBySlot: Map<Pair<Int, Int>, Set<CropDefinition>>
    ): Map<Pair<Int, Int>, List<CropDefinition>> {
        val spots = linkedMapOf<Pair<Int, Int>, List<CropDefinition>>()

        for (y in 0 until layout.size) {
            for (x in 0 until layout.size) {
                val slot = layout.getSlot(x, y) ?: continue
                val plantOnSlot = layout.plantCovering(slot)

                val coveringTarget = plantOnSlot?.takeIf {
                    it.slot.mark == LayoutSlot.Marking.Target && it.growthStage == null
                }
                if (plantOnSlot != null && coveringTarget == null) continue

                val targetStartingHere = coveringTarget?.takeIf { it.slot === slot }
                val plannedCrops = plannedCropsBySlot[x to y].orEmpty() + targetStartingHere?.acceptedCrops.orEmpty()
                val spawnableMutations = spawningCrops.filter { crop ->
                    crop !in plannedCrops && missingSpawnConditions(layout, crop, x, y, ignoredPlant = coveringTarget).isEmpty()
                }
                if (spawnableMutations.isNotEmpty()) spots[x to y] = spawnableMutations
            }
        }
        return spots
    }

    fun targetCropsBySlot(plan: PlotLayout?): Map<Pair<Int, Int>, Set<CropDefinition>> =
        plan?.plants
            ?.filter { it.slot.mark == LayoutSlot.Marking.Target }
            ?.associate { (it.slot.x to it.slot.y) to it.acceptedCrops.toSet() }
            .orEmpty()

    private const val WATER_LOSS_PER_TICK: Int = 20

    const val WATER_HALT_LEVEL: Int = -100

    const val NEGATIVE_WATER_SKIP_CHANCE: Double = 0.175

    const val WATER_FULL_LEVEL: Int = 100

    // the plant near the soggy bud drain amount
    const val DRAIN_PER_DONOR: Double = 2.5

    // the ratio of water drained from the donor to what the soggybud got
    // eg for 1 donor the soggy gets 1.39 water from the constant above too.
    const val DRAIN_KEPT: Double = 0.556

    // 5.5 is what soggybud needs in "Water Drained" added for each stage of growth, otherwise stalled
    const val DRAIN_PER_STAGE: Double = 5.5

    fun waterLossPerTick(waterEffectPercent: Int): Double =
        (WATER_LOSS_PER_TICK * (1.0 - waterEffectPercent / 200.0)).coerceAtLeast(0.0)

    fun waterLevelAfter(water: Double, ticks: Int, waterEffectPercent: Int): Double =
        (water - waterLossPerTick(waterEffectPercent) * ticks).coerceAtLeast(WATER_HALT_LEVEL.toDouble())

    fun ticksUntilHalt(water: Double, waterEffectPercent: Int): Int? {
        val loss = waterLossPerTick(waterEffectPercent)
        if (loss <= 0.0) return null

        return ceil((water - WATER_HALT_LEVEL) / loss).toInt()
    }

    fun formatWaterLevel(water: Double): String =
        if (water == floor(water)) water.toInt().toString() else "%.1f".format(water)

    fun timeUntilHalt(water: Double, waterEffectPercent: Int, remainingMs: Long, tickMs: Long): Long? {
        val ticks = ticksUntilHalt(water, waterEffectPercent) ?: return null

        return remainingMs + (ticks - 1) * tickMs
    }

    // TODO: code isnt accurate with some spawning rules, to be implemented properly, currently unused
    class Result(
        val targetSpotsByCrop: Map<String, Int>,
        val harvestedPerVisit: Double,
        val harvestedPerFiftyTicks: Double,
        val ticksUntilFirstPlantDry: Int?,
        val blockedTargetCount: Int
    )

    private const val SIMULATED_RUNS: Int = 60
    private const val FIFTY_TICKS: Int = 50

    private const val WARM_UP_TICKS: Int = 30
    private const val MEASURED_TICKS: Int = 100

    private const val OFFLINE_GRACE_TICKS: Int = 5

    private class TargetSpot(val x: Int, val y: Int, val crop: CropDefinition, val targetChance: Double, val unplannedChance: Double)

    fun simulateVisits(plan: PlotLayout, awayTicks: Int, weightMultiplier: Double, random: Random = Random.Default): Result {
        val targets = plan.plants.filter { it.slot.mark == LayoutSlot.Marking.Target && it.cropDef.spawnRule != null }
        val spots = targets.map { target -> targetSpotOf(plan, target, weightMultiplier) }

        val builtLayout = plan.freshCopy()
        builtLayout.plants.removeAll { copy -> targets.any { it.slot.x == copy.slot.x && it.slot.y == copy.slot.y } }
        builtLayout.plants.forEach { setGrownAndWatered(it) }

        val warmUpTicks = roundUpToMultiple(WARM_UP_TICKS, awayTicks)
        val measuredTicks = roundUpToMultiple(MEASURED_TICKS, awayTicks)
        var harvested = 0L

        repeat(SIMULATED_RUNS) {
            harvested += simulateOneRun(builtLayout, spots, awayTicks, warmUpTicks, measuredTicks, random)
        }

        val harvestedPerTick = harvested.toDouble() / SIMULATED_RUNS / measuredTicks
        return Result(
            targetSpotsByCrop = targets.groupingBy { it.cropDef.name }.eachCount(),
            harvestedPerVisit = harvestedPerTick * awayTicks,
            harvestedPerFiftyTicks = harvestedPerTick * FIFTY_TICKS,
            ticksUntilFirstPlantDry = ticksUntilFirstPlantDries(plan, targets),
            blockedTargetCount = spots.count { it.targetChance <= 0.0 }
        )
    }

    private fun targetSpotOf(plan: PlotLayout, target: Plant, weightMultiplier: Double): TargetSpot {
        val chances = mutationChancesAtSlot(plan, target.slot.x, target.slot.y, weightMultiplier, ignoredPlant = target)
        val (wanted, unplanned) = chances.partition { target.acceptsCrop(it.crop) }

        return TargetSpot(target.slot.x, target.slot.y, target.cropDef, wanted.sumOf { it.chance }, unplanned.sumOf { it.chance })
    }

    private fun simulateOneRun(
        builtLayout: PlotLayout,
        spots: List<TargetSpot>,
        awayTicks: Int,
        warmUpTicks: Int,
        measuredTicks: Int,
        random: Random
    ): Int {
        val layout = builtLayout.freshCopy()
        val occupiedSpots = mutableMapOf<TargetSpot, Plant?>()
        val graceTicksLeftBySpawn: MutableMap<Plant, Int> = mutableMapOf()
        var harvested = 0

        for (tick in 1..warmUpTicks + measuredTicks) {
            val frozenSpawns = graceTicksLeftBySpawn.filter { (spawn, graceTicksLeft) ->
                graceTicksLeft > 0 && wouldHaltNextTick(layout, spawn)
            }.keys
            frozenSpawns.forEach { graceTicksLeftBySpawn[it] = graceTicksLeftBySpawn.getValue(it) - 1 }

            val frozenStates = frozenSpawns.map { FrozenState(it) }
            GreenhouseGrid.simulateLayout(layout, 1)
            frozenStates.forEach { it.restore() }

            spots.filter { it !in occupiedSpots }.forEach { spot ->
                val roll = random.nextDouble()
                when {
                    roll < spot.targetChance -> {
                        val slot = layout.getSlot(spot.x, spot.y) ?: return@forEach
                        val spawnedTarget = Plant(spot.crop.elementId, slot, growthStage = PlantStage.Known(1), cropDef = spot.crop)
                        spawnedTarget.waterLevel = if (spot.crop.needsWater) 0.0 else null
                        spawnedTarget.appearedAt = System.currentTimeMillis()
                        layout.plants.add(spawnedTarget)
                        occupiedSpots[spot] = spawnedTarget
                        graceTicksLeftBySpawn[spawnedTarget] = OFFLINE_GRACE_TICKS
                    }
                    roll < spot.targetChance + spot.unplannedChance -> occupiedSpots[spot] = null
                }
            }

            if (tick % awayTicks != 0) continue

            occupiedSpots.entries.removeAll { (_, plant) ->
                when {
                    plant == null -> true
                    plant.isFullyGrown -> {
                        layout.plants.remove(plant)
                        graceTicksLeftBySpawn.remove(plant)
                        if (tick > warmUpTicks) harvested++
                        true
                    }
                    else -> false
                }
            }
            graceTicksLeftBySpawn.replaceAll { _, _ -> 0 }
            layout.plants.forEach { waterToFull(it) }
        }
        return harvested
    }

    private fun wouldHaltNextTick(layout: PlotLayout, plant: Plant): Boolean {
        val water = plant.waterLevel ?: return false
        return waterLevelAfter(water, 1, GreenhouseGrid.waterEffectAt(layout, plant.slot)) <= PlotPrediction.WATER_HALT_LEVEL
    }

    private class FrozenState(val plant: Plant) {
        private val waterLevel = plant.waterLevel
        private val growthStage = plant.growthStage
        private val appearedAt = plant.appearedAt
        private val readings = plant.readings.toMap()

        fun restore() {
            plant.waterLevel = waterLevel
            plant.growthStage = growthStage
            plant.appearedAt = appearedAt
            plant.readings.clear()
            plant.readings.putAll(readings)
        }
    }

    private fun setGrownAndWatered(plant: Plant) {
        plant.growthStage = PlantStage.Known(plant.cropDef.maxStage)
        plant.appearedAt = System.currentTimeMillis()
        waterToFull(plant)
    }

    private fun waterToFull(plant: Plant) {
        if (!plant.cropDef.needsWater) return
        plant.waterLevel = WATER_FULL_LEVEL.toDouble()
        plant.waterBestCase = null
        plant.waterPredictedNegative = false
    }

    private fun ticksUntilFirstPlantDries(plan: PlotLayout, targets: List<Plant>): Int? {
        val drainingTargets = targets.filter { it.cropDef.drainsNeighbours }

        return plan.plants.mapNotNull { plant ->
            if (!plant.cropDef.needsWater) return@mapNotNull null
            val isTarget = plant in targets
            val ownLossPerTick = if (isTarget && !plant.cropDef.drainsNeighbours) {
                waterLossPerTick(GreenhouseGrid.waterEffectAt(plan, plant.slot))
            } else {
                0.0
            }
            val drainedPerTick = if (isTarget) 0.0 else drainingTargets.count { it in plan.plantsSurrounding(plant) } * PlotPrediction.DRAIN_PER_DONOR
            val waterLossPerTick = ownLossPerTick + drainedPerTick
            if (waterLossPerTick <= 0.0) null else (PlotPrediction.WATER_FULL_LEVEL / waterLossPerTick).toInt()
        }.minOrNull()
    }

    private fun roundUpToMultiple(ticks: Int, step: Int): Int = ((ticks + step - 1) / step) * step
}
