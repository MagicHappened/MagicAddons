package org.magic.magicaddons.data.greenhouse.plot

import java.util.IdentityHashMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random
import org.magic.magicaddons.data.greenhouse.crops.*

/**
 * What a plot would do: which mutations can land on a slot, how its water runs down, and what it
 * grows into over a stretch of ticks. Everything here works on a layout handed to it rather than on
 * the plot the player is standing in, so the screens can ask about a plan that is not planted yet.
 */
object PlotPrediction {

    class MutationChance(val crop: CropDefinition, val chance: Double)

    private val POSITIVE_EFFECT_NAMES: Map<CropEffect.EffectKind, String> = mapOf(
        CropEffect.EffectKind.Yield to "Harvest Boost",
        CropEffect.EffectKind.Xp to "XP Boost",
        CropEffect.EffectKind.Water to "Water Retain",
        CropEffect.EffectKind.Drops to "Bonus Drops",
        CropEffect.EffectKind.Immunity to "Immunity",
        CropEffect.EffectKind.Spread to "Effect Spread"
    )

    /** a blank weight fills every roll up to this */
    private const val MIN_ROLL_WEIGHT_TOTAL: Double = 100.0

    fun mutationChancesAtSlot(
        layout: PlotLayout,
        x: Int,
        y: Int,
        weightMultiplier: Double,
        ignoredPlant: Plant? = null
    ): List<MutationChance> {
        val weights = CropRegistry.all
            .filter { crop -> (crop.spawnRule?.weight ?: 0) > 0 && missingSpawnConditions(layout, crop, x, y, ignoredPlant).isEmpty() }
            .associateWith { it.spawnRule!!.weight * weightMultiplier }
        val rollTotal = max(MIN_ROLL_WEIGHT_TOTAL, weights.values.sum())

        return weights.map { (crop, weight) -> MutationChance(crop, weight / rollTotal) }.sortedByDescending { it.chance }
    }

    fun missingSpawnConditions(
        layout: PlotLayout,
        crop: CropDefinition,
        x: Int,
        y: Int,
        ignoredPlant: Plant? = null
    ): List<String> {
        val rule = crop.spawnRule ?: return listOf("never appears on its own")
        val width = crop.footprint.width
        val height = crop.footprint.height
        if (x < 0 || y < 0 || x + width > layout.size || y + height > layout.size) return listOf("does not fit here")

        // a marked target that has not spawned yet is a wish: it takes up no room and is no neighbour
        fun plantOn(cellX: Int, cellY: Int): Plant? =
            layout.getSlot(cellX, cellY)?.let { layout.plantCovering(it) }?.takeUnless {
                it === ignoredPlant || (it.slot.mark == LayoutSlot.Marking.Target && it.growthStage == null)
            }

        val footprintCells = (x until x + width).flatMap { cellX -> (y until y + height).map { cellY -> cellX to cellY } }
        val missing = mutableListOf<String>()

        if (footprintCells.any { (cellX, cellY) -> plantOn(cellX, cellY) != null }) missing += "no room"

        val footprintSoils = footprintCells.map { (cellX, cellY) -> layout.getSlot(cellX, cellY)?.soil?.block }
        if (footprintSoils.any { it == null || it !in crop.requiredSoil }) {
            missing += "needs ${crop.requiredSoil.joinToString(" or ") { it.name.string }}"
        }

        val surroundingCells = ((x - 1)..(x + width)).flatMap { cellX -> ((y - 1)..(y + height)).map { cellY -> cellX to cellY } }
            .filter { (cellX, cellY) -> (cellX to cellY) !in footprintCells && cellX in 0 until layout.size && cellY in 0 until layout.size }
        val surroundingCellsByCrop = surroundingCells.mapNotNull { (cellX, cellY) -> plantOn(cellX, cellY) }
            .groupingBy { it.cropDef.name }
            .eachCount()

        if (rule.needsNoNeighbours && surroundingCellsByCrop.isNotEmpty()) missing += "needs no crop around it"

        rule.requiredNeighbourCells.forEach { (neighbourCrop, requiredCells) ->
            val missingCount = requiredCells - (surroundingCellsByCrop[neighbourCrop] ?: 0)
            if (missingCount > 0) missing += "$missingCount more $neighbourCrop"
        }

        if (rule.needsAllPositiveEffects) {
            val effectsReceived = footprintCells.flatMap { (cellX, cellY) -> layout.getSlot(cellX, cellY)?.let { layout.effectsAt(it) }.orEmpty() }
            val missingEffects = POSITIVE_EFFECT_NAMES.filterKeys { kind -> effectsReceived.none { it.kind == kind && it.percent >= 0 } }.values
            if (missingEffects.isNotEmpty()) missing += "no ${missingEffects.joinToString(", ")}"
        }

        return missing
    }

    fun missingConditionsForTarget(layout: PlotLayout, target: Plant): List<String> =
        missingSpawnConditions(layout, target.cropDef, target.slot.x, target.slot.y, ignoredPlant = target)

    fun unplannedMutationSpots(
        layout: PlotLayout,
        plannedCropsBySlot: Map<Pair<Int, Int>, Set<CropDefinition>>
    ): Map<Pair<Int, Int>, List<CropDefinition>> {
        val mutations = CropRegistry.all.filter { (it.spawnRule?.weight ?: 0) > 0 }
        val spots = linkedMapOf<Pair<Int, Int>, List<CropDefinition>>()

        for (y in 0 until layout.size) {
            for (x in 0 until layout.size) {
                val slot = layout.getSlot(x, y) ?: continue
                val plantOnSlot = layout.plantCovering(slot)

                // a target waiting to spawn leaves every cell it covers free, and a cell it only
                // covers can still be the corner a mutation of its own grows from
                val coveringTarget = plantOnSlot?.takeIf {
                    it.slot.mark == LayoutSlot.Marking.Target && it.growthStage == null
                }
                if (plantOnSlot != null && coveringTarget == null) continue

                val targetStartingHere = coveringTarget?.takeIf { it.slot === slot }
                val plannedCrops = plannedCropsBySlot[x to y].orEmpty() + targetStartingHere?.acceptedCrops.orEmpty()
                val spawnableMutations = mutations.filter { crop ->
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

    const val WATER_LOSS_PER_TICK: Int = 20

    const val WATER_DEATH_LEVEL: Int = -100

    const val WATER_FULL_LEVEL: Int = 100

    /** taken from each neighbour holding any water, corners included */
    const val DRAIN_PER_DONOR: Double = 2.5

    /**
     * The share of what it took that a draining plant keeps. The only ratio that reproduces a bud's
     * whole run of shown totals, 10.4 to 54.9 in steps of 20 taken, with the display rounding
     * ties down; 0.555 and 0.56 each miss one of them.
     */
    const val DRAIN_KEPT: Double = 0.556

    /**
     * What a draining plant has to have kept to leave a stage, per stage: ten taken a stage, which
     * is 5.5 as the plant shows it. Judged after the tick's drain has landed.
     */
    const val DRAIN_PER_STAGE: Double = 5.5

    /** clamped, so no amount of retain lets a plant gain water by standing still */
    fun waterLossPerTick(waterEffectPercent: Int): Double =
        (WATER_LOSS_PER_TICK * (1.0 - waterEffectPercent / 200.0)).coerceAtLeast(0.0)

    /** below the death level the gap says how many ticks the plant has been dead for in the estimate */
    fun waterLevelAfter(water: Double, ticks: Int, waterEffectPercent: Int): Double =
        water - waterLossPerTick(waterEffectPercent) * ticks

    /** where a plant predicted dead but still standing has to be: one tick from dying */
    fun lowestWaterLevelStillAlive(predicted: Double, waterEffectPercent: Int): Double {
        val loss = waterLossPerTick(waterEffectPercent)
        if (loss <= 0.0 || predicted > WATER_DEATH_LEVEL) return predicted

        val ticksSkipped = floor((WATER_DEATH_LEVEL - predicted) / loss) + 1

        return predicted + ticksSkipped * loss
    }

    /** the last tick counts even when it only takes the plant part of the way, as the game's own figure does */
    fun ticksUntilDeath(water: Double, waterEffectPercent: Int): Int? {
        val loss = waterLossPerTick(waterEffectPercent)
        if (loss <= 0.0) return null

        return ceil((water - WATER_DEATH_LEVEL) / loss).toInt()
    }

    /** whole when it is whole, otherwise to one place, the way the game writes it */
    fun formatWaterLevel(water: Double): String =
        if (water == floor(water)) water.toInt().toString() else "%.1f".format(water)

    /** what remains of the current tick plus whole ticks after it; the killing tick is not waited out */
    fun timeUntilDeath(water: Double, waterEffectPercent: Int, remainingMs: Long, tickMs: Long): Long? {
        val ticks = ticksUntilDeath(water, waterEffectPercent) ?: return null

        return remainingMs + (ticks - 1) * tickMs
    }

    class Result(
        val targetSpotsByCrop: Map<String, Int>,
        val harvestedPerVisit: Double,
        val harvestedPerFiftyTicks: Double,
        val ticksUntilFirstPlantDry: Int?,
        val blockedTargetCount: Int
    )

    private const val SIMULATED_RUNS: Int = 60
    private const val FIFTY_TICKS: Int = 50

    /** ticks left out of the count while the spots fill the first time */
    private const val WARM_UP_TICKS: Int = 30
    private const val MEASURED_TICKS: Int = 100

    /** out of the garden counts as offline */
    private const val OFFLINE_GRACE_TICKS: Int = 5

    private class TargetSpot(val x: Int, val y: Int, val crop: CropDefinition, val targetChance: Double, val unplannedChance: Double)

    fun simulateVisits(plan: PlotLayout, awayTicks: Int, weightMultiplier: Double, tickMs: Long, random: Random = Random.Default): Result {
        val targets = plan.plants.filter { it.slot.mark == LayoutSlot.Marking.Target && it.cropDef.spawnRule != null }
        val spots = targets.map { target -> targetSpotOf(plan, target, weightMultiplier) }

        val builtLayout = plan.deepCopy()
        builtLayout.plants.removeAll { copy -> targets.any { it.slot.x == copy.slot.x && it.slot.y == copy.slot.y } }
        builtLayout.plants.forEach { setGrownAndWatered(it) }

        val warmUpTicks = roundUpToMultiple(WARM_UP_TICKS, awayTicks)
        val measuredTicks = roundUpToMultiple(MEASURED_TICKS, awayTicks)
        var harvested = 0L

        repeat(SIMULATED_RUNS) {
            harvested += simulateOneRun(builtLayout, spots, awayTicks, warmUpTicks, measuredTicks, tickMs, random)
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
        val chances = PlotPrediction.mutationChancesAtSlot(plan, target.slot.x, target.slot.y, weightMultiplier, ignoredPlant = target)
        val (wanted, unplanned) = chances.partition { target.acceptsCrop(it.crop) }

        return TargetSpot(target.slot.x, target.slot.y, target.cropDef, wanted.sumOf { it.chance }, unplanned.sumOf { it.chance })
    }

    private fun simulateOneRun(
        builtLayout: PlotLayout,
        spots: List<TargetSpot>,
        awayTicks: Int,
        warmUpTicks: Int,
        measuredTicks: Int,
        tickMs: Long,
        random: Random
    ): Int {
        val layout = builtLayout.deepCopy()
        val occupiedSpots = mutableMapOf<TargetSpot, Plant?>()
        // a Plant hashes on its water and stage, which the simulation moves under it, so the spawn
        // is tracked by identity rather than by value
        val graceTicksLeftBySpawn: MutableMap<Plant, Int> = IdentityHashMap()
        var harvested = 0

        for (tick in 1..warmUpTicks + measuredTicks) {
            val frozenSpawns = graceTicksLeftBySpawn.filter { (spawn, graceTicksLeft) ->
                graceTicksLeft > 0 && wouldDieNextTick(layout, spawn)
            }.keys
            frozenSpawns.forEach { graceTicksLeftBySpawn[it] = graceTicksLeftBySpawn.getValue(it) - 1 }

            val frozenStates = frozenSpawns.map { FrozenState(it) }
            GreenhouseGrid.simulateLayout(layout, 1, tickMs)
            frozenStates.forEach { it.restore() }

            occupiedSpots.entries.forEach { entry ->
                val spawn = entry.value ?: return@forEach
                if ((spawn.waterLevel ?: 0.0) > PlotPrediction.WATER_DEATH_LEVEL) return@forEach

                // a dead spawn blocks the spot until the next visit
                layout.plants.remove(spawn)
                graceTicksLeftBySpawn.remove(spawn)
                entry.setValue(null)
            }

            spots.filter { it !in occupiedSpots }.forEach { spot ->
                val roll = random.nextDouble()
                when {
                    roll < spot.targetChance -> {
                        val slot = layout.getSlot(spot.x, spot.y) ?: return@forEach
                        val spawnedTarget = Plant(spot.crop.elementId, slot, growthStage = PlantStage.Known(1), cropDef = spot.crop)
                        spawnedTarget.waterLevel = if (spot.crop.needsWater) 0.0 else null
                        spawnedTarget.age = 0L
                        layout.plants.add(spawnedTarget)
                        occupiedSpots[spot] = spawnedTarget
                        graceTicksLeftBySpawn[spawnedTarget] = OFFLINE_GRACE_TICKS
                    }
                    // an unplanned spawn blocks the spot until the next visit
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
            // the grace period ends once the player is back
            graceTicksLeftBySpawn.replaceAll { _, _ -> 0 }
            layout.plants.forEach { waterToFull(it) }
        }
        return harvested
    }

    private fun wouldDieNextTick(layout: PlotLayout, plant: Plant): Boolean {
        val water = plant.waterLevel ?: return false
        return PlotPrediction.waterLevelAfter(water, 1, GreenhouseGrid.waterEffectAt(layout, plant.slot)) <= PlotPrediction.WATER_DEATH_LEVEL
    }

    private class FrozenState(val plant: Plant) {
        private val waterLevel = plant.waterLevel
        private val growthStage = plant.growthStage
        private val age = plant.age
        private val readings = plant.readings.toMap()

        fun restore() {
            plant.waterLevel = waterLevel
            plant.growthStage = growthStage
            plant.age = age
            plant.readings.clear()
            plant.readings.putAll(readings)
        }
    }

    private fun setGrownAndWatered(plant: Plant) {
        plant.growthStage = PlantStage.Known(plant.cropDef.maxStage)
        plant.age = 0L
        waterToFull(plant)
    }

    private fun waterToFull(plant: Plant) {
        if (!plant.cropDef.needsWater) return
        plant.waterLevel = PlotPrediction.WATER_FULL_LEVEL.toDouble()
        plant.waterBestCase = null
        plant.waterPredictedInDebt = false
    }

    private fun ticksUntilFirstPlantDries(plan: PlotLayout, targets: List<Plant>): Int? {
        val drainingTargets = targets.filter { it.cropDef.drainsNeighbours }

        return plan.plants.mapNotNull { plant ->
            if (!plant.cropDef.needsWater) return@mapNotNull null
            val isTarget = plant in targets
            val ownLossPerTick = if (isTarget && !plant.cropDef.drainsNeighbours) {
                PlotPrediction.waterLossPerTick(GreenhouseGrid.waterEffectAt(plan, plant.slot))
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
