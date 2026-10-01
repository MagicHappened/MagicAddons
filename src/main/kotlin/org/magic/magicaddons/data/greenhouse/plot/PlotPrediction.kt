package org.magic.magicaddons.data.greenhouse.plot

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.PlantStage

object PlotPrediction {

    class MutationChance(val crop: CropDefinition, val chance: Double)

    private const val MIN_ROLL_WEIGHT_TOTAL: Double = 100.0

    fun mutationChancesAtSlot(
        layout: PlotLayout,
        x: Int,
        y: Int,
        weightMultiplier: Double,
        ignoredPlant: Plant? = null
    ): List<MutationChance> {
        val weightByCrop = CropRegistry.allCrops
            .filter { crop -> (crop.spawnRule?.weight ?: 0) > 0 && missingSpawnConditions(layout, crop, x, y, ignoredPlant).isEmpty() }
            .associateWith { it.spawnRule!!.weight * weightMultiplier }
        val rollTotal = max(MIN_ROLL_WEIGHT_TOTAL, weightByCrop.values.sum())

        return weightByCrop.map { (crop, weight) -> MutationChance(crop, weight / rollTotal) }.sortedByDescending { it.chance }
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

        val footprintCells = crop.footprint.cellsFrom(x, y)
        val missing = mutableListOf<String>()

        if (rule.staysOffPlotEdge && (x == 0 || y == 0 || x + width == layout.size || y + height == layout.size)) missing += "touches the plot edge"

        if (footprintCells.any { (cellX, cellY) -> standingPlantAtPos(layout, cellX, cellY, ignoredPlant) != null }) missing += "no room"

        val footprintSoils = footprintCells.map { (cellX, cellY) -> layout.getSlot(cellX, cellY)?.soil }
        if (footprintSoils.any { it == null || it !in crop.requiredSoil }) {
            missing += "needs ${crop.requiredSoil.joinToString(" or ") { it.name.string }}"
        }

        missing += missingNeighbourConditions(layout, crop, x, y, ignoredPlant)

        if (rule.needsOwnEffectsAround) {
            val missingEffects = crop.effects - effectsReceivedAround(layout, crop, x, y, ignoredPlant)
            if (missingEffects.isNotEmpty()) missing += "no ${missingEffects.joinToString(", ") { it.label }} around it"
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
        val surroundingCellsByCrop = cellsSurrounding(layout, crop, x, y).mapNotNull { (cellX, cellY) -> standingPlantAtPos(layout, cellX, cellY, ignoredPlant) }
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
            .mapNotNull { (cellX, cellY) -> standingLayout.plantCovering(cellX, cellY) }
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

    private fun isStanding(plant: Plant, ignoredPlant: Plant?): Boolean =
        plant !== ignoredPlant && !(plant.slot.mark == LayoutSlot.Marking.Target && plant.growthStage == null)

    fun unplannedMutationSpots(
        layout: PlotLayout,
        plannedCropsBySlot: Map<Pair<Int, Int>, Set<CropDefinition>>
    ): Map<Pair<Int, Int>, List<CropDefinition>> {
        val spawningCrops = CropRegistry.allCrops.filter { crop ->
            (crop.spawnRule?.weight ?: 0) > 0
        }
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

    const val WATER_DEATH_LEVEL: Int = -100

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
        water - waterLossPerTick(waterEffectPercent) * ticks

    fun lowestWaterLevelStillAlive(predictedWater: Double, waterEffectPercent: Int): Double {
        val loss = waterLossPerTick(waterEffectPercent)
        if (loss <= 0.0 || predictedWater > WATER_DEATH_LEVEL) return predictedWater

        val fewestTicksSkipped = floor((WATER_DEATH_LEVEL - predictedWater) / loss) + 1

        return predictedWater + fewestTicksSkipped * loss
    }

    fun ticksUntilDeath(water: Double, waterEffectPercent: Int): Int? {
        val loss = waterLossPerTick(waterEffectPercent)
        if (loss <= 0.0) return null

        return ceil((water - WATER_DEATH_LEVEL) / loss).toInt()
    }

    fun formatWaterLevel(water: Double): String =
        if (water == floor(water)) water.toInt().toString() else "%.1f".format(water)

    fun timeUntilDeath(water: Double, waterEffectPercent: Int, remainingMs: Long, tickMs: Long): Long? {
        val ticks = ticksUntilDeath(water, waterEffectPercent) ?: return null

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
                graceTicksLeft > 0 && wouldDieNextTick(layout, spawn)
            }.keys
            frozenSpawns.forEach { graceTicksLeftBySpawn[it] = graceTicksLeftBySpawn.getValue(it) - 1 }

            val frozenStates = frozenSpawns.map { FrozenState(it) }
            GreenhouseGrid.simulateLayout(layout, 1)
            frozenStates.forEach { it.restore() }

            occupiedSpots.entries.forEach { entry ->
                val spawn = entry.value ?: return@forEach
                if ((spawn.waterLevel ?: 0.0) > PlotPrediction.WATER_DEATH_LEVEL) return@forEach

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

    private fun wouldDieNextTick(layout: PlotLayout, plant: Plant): Boolean {
        val water = plant.waterLevel ?: return false
        return waterLevelAfter(water, 1, GreenhouseGrid.waterEffectAt(layout, plant.slot)) <= PlotPrediction.WATER_DEATH_LEVEL
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
