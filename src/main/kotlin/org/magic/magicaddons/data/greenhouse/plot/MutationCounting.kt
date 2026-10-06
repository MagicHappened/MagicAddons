package org.magic.magicaddons.data.greenhouse.plot

import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime

class DiagnosisReading(
    val timesMutated: Int,
    val combinedRemaining: Int?,
    val readAt: Long,
    var spawnsSeenSince: Int = 0,
    var isStillExact: Boolean = true
) {
    val timesMutatedNow: Int get() = timesMutated + spawnsSeenSince

    val isFromThisTick: Boolean get() = GreenhouseTickTime.hasGrowthTickPassedSince(readAt) == false
}

class BlindSpawns(
    val x: Int,
    val y: Int,
    val cropId: String,
    val contributorSlots: Set<Pair<Int, Int>>,
    val createdAt: Long,
    val isBeforeTracking: Boolean,
    var atLeast: Int = 0,
    var isExact: Boolean = false
) {
    fun contributorsIn(layout: PlotLayout): List<Plant> = layout.plants.filter { plant ->
        (plant.slot.x to plant.slot.y) in contributorSlots && (plant.appearedAt ?: 0L) <= createdAt
    }
}

object MutationCounting {

    private class TeleporterSpot(val x: Int, val y: Int, val crop: CropDefinition, val credit: PlotPrediction.SpawnCredit)

    private val teleportingCrops: List<CropDefinition>
        get() = CropRegistry.allCrops.filter { it.teleportsWhileGrowing && it.spawnRule != null }

    private fun teleporterSpotsOf(layout: PlotLayout): List<TeleporterSpot> = teleportingCrops.flatMap { crop ->
        (0 until layout.size).flatMap { y ->
            (0 until layout.size).mapNotNull { x ->
                val plantOnSpot = layout.plantCovering(x, y)
                if (PlotPrediction.missingNeighbourConditions(layout, crop, x, y, ignoredPlant = plantOnSpot).isNotEmpty()) return@mapNotNull null

                TeleporterSpot(x, y, crop, PlotPrediction.spawnCreditOf(layout, crop, x, y, ignoredPlant = plantOnSpot))
            }
        }
    }

    fun startCountingFromZero(plant: Plant) {
        plant.seenSpawnsHelped = 0
        plant.isCountedFromStart = true
        plant.isMutationCountTracked = true
        plant.hasUncertainCredit = false
        plant.mutationsSpawned = 0
        plant.mutationsSpawnedIsMinimum = false
    }

    fun countSpawn(layout: PlotLayout, spawn: Plant) {
        val crop = spawn.cropDef
        val spawnX = spawn.slot.x
        val spawnY = spawn.slot.y

        if (crop.teleportsWhileGrowing) {
            val isOnItsSpawnSpot = spawn.highestStage == 1 &&
                    PlotPrediction.missingNeighbourConditions(layout, crop, spawnX, spawnY, ignoredPlant = spawn).isEmpty()
            if (!isOnItsSpawnSpot) return
        }

        val credit = PlotPrediction.spawnCreditOf(layout, crop, spawnX, spawnY, ignoredPlant = spawn)
        credit.uncertain.forEach(::loseExactCount)
        credit.certain.forEach {
            it.seenSpawnsHelped++
            creditReading(it)
        }
    }

    private fun loseExactCount(plant: Plant) {
        plant.isMutationCountTracked = false
        plant.hasUncertainCredit = true
        plant.lastDiagnosisReading?.isStillExact = false
    }

    private fun creditReading(plant: Plant) {
        val reading = plant.lastDiagnosisReading ?: return
        if (!reading.isStillExact) return

        when (GreenhouseTickTime.hasGrowthTickPassedSince(reading.readAt)) {
            true -> reading.spawnsSeenSince++
            false -> Unit
            null -> reading.isStillExact = false
        }
    }

    fun noteUnwatchedTicks(layout: PlotLayout, blindSpawns: MutableList<BlindSpawns>) {
        layout.plants.forEach { it.lastDiagnosisReading?.isStillExact = false }

        val now = System.currentTimeMillis()
        teleporterSpotsOf(layout).forEach { spot ->
            spot.credit.uncertain.forEach(::loseExactCount)
            val contributorSlots = spot.credit.certain.map { it.slot.x to it.slot.y }.toSet()
            if (contributorSlots.isEmpty()) return@forEach

            val cropId = spot.crop.elementId
            val sameGroup = blindSpawns.firstOrNull { blind ->
                !blind.isBeforeTracking && blind.x == spot.x && blind.y == spot.y && blind.cropId == cropId &&
                        blind.contributorsIn(layout).map { it.slot.x to it.slot.y }.toSet() == contributorSlots
            }
            if (sameGroup != null) {
                sameGroup.isExact = false
            } else {
                blindSpawns += BlindSpawns(spot.x, spot.y, cropId, contributorSlots, now, isBeforeTracking = false)
            }
        }
    }

    private fun addSpawnsBeforeTracking(layout: PlotLayout, blindSpawns: MutableList<BlindSpawns>) {
        val now = System.currentTimeMillis()
        teleporterSpotsOf(layout).forEach { spot ->
            val ring = spot.credit.certain + spot.credit.uncertain
            if (ring.isEmpty() || ring.any { it.isCountedFromStart }) return@forEach

            val cropId = spot.crop.elementId
            if (blindSpawns.any { it.isBeforeTracking && it.x == spot.x && it.y == spot.y && it.cropId == cropId }) return@forEach

            spot.credit.uncertain.forEach(::loseExactCount)
            spot.credit.certain.filterNot { it.hasUncertainCredit }.forEach { it.isMutationCountTracked = true }
            blindSpawns += BlindSpawns(spot.x, spot.y, cropId, spot.credit.certain.map { it.slot.x to it.slot.y }.toSet(), now, isBeforeTracking = true)
        }
    }

    fun noteDiagnosis(layout: PlotLayout, plant: Plant, reading: DiagnosisReading, blindSpawns: List<BlindSpawns>) {
        plant.lastDiagnosisReading = reading
        if (blindSpawns.any { blind -> !blind.isExact && plant in blind.contributorsIn(layout) }) return

        plant.seenSpawnsHelped = reading.timesMutated - blindSpawns.filter { plant in it.contributorsIn(layout) }.sumOf { it.atLeast }
        plant.isMutationCountTracked = true
        plant.hasUncertainCredit = false
    }

    fun recountPlot(layout: PlotLayout, blindSpawns: MutableList<BlindSpawns>): Map<BlindSpawns, List<Plant>> {
        addSpawnsBeforeTracking(layout, blindSpawns)

        val contributorsByBlind = blindSpawns.associateWith { it.contributorsIn(layout) }
        blindSpawns.removeAll { contributorsByBlind[it].isNullOrEmpty() }
        val liveContributors = contributorsByBlind.filterValues { it.isNotEmpty() }

        val solution = BlindSpawnSolver.solve(layout, liveContributors)
        if (solution == null) {
            Common.LOGGER.warn("Mutation counts in ${layout.displayName()} don't fit the diagnostics readings, keeping the previous counts")
        } else {
            solution.spawnsByBlind.forEach { (blind, spawns) ->
                blind.atLeast = spawns.first
                blind.isExact = spawns.first == spawns.last
            }
        }

        val blindsByPlant = liveContributors.entries
            .flatMap { (blind, contributors) -> contributors.map { it to blind } }
            .groupBy({ it.first }, { it.second })

        fun updateCount(plant: Plant) {
            val blinds = blindsByPlant[plant].orEmpty()
            val solvedSum = solution?.blindSpawnsByPlant?.get(plant)?.takeIf { it.first == it.last }
            val blindSpawnCount = solvedSum?.first ?: blinds.sumOf { it.atLeast }
            val isCounted = plant.isMutationCountTracked && !plant.hasUncertainCredit &&
                    (solvedSum != null || blinds.all { it.isExact })

            val reading = plant.lastDiagnosisReading
            val isReadingExact = reading?.isStillExact == true

            plant.mutationsSpawned = maxOf(plant.seenSpawnsHelped + blindSpawnCount, reading?.timesMutatedNow ?: 0)
            plant.mutationsSpawnedIsMinimum = !isCounted && !isReadingExact
        }

        layout.plants.forEach(::updateCount)
        if (inferFromCombinedReadings(layout)) layout.plants.forEach(::updateCount)
        reportCombinedMismatches(layout)
        return liveContributors
    }

    private class CombinedReading(val crop: CropDefinition, val minimum: Int, val remaining: Int, val readAt: Long)

    private val reportedMismatches = HashSet<String>()

    private fun owedBy(timesMutated: Int, minimum: Int): Int = if (timesMutated in 1 until minimum) minimum - timesMutated else 0

    private fun mostOwedBy(lowestTimesMutated: Int, minimum: Int): Int = owedBy(maxOf(lowestTimesMutated, 1), minimum)

    private fun combinedReadingsOf(layout: PlotLayout): List<CombinedReading> = layout.plants
        .mapNotNull { plant -> plant.lastDiagnosisReading?.takeIf { it.combinedRemaining != null && it.isFromThisTick }?.let { plant.cropDef to it } }
        .groupBy({ it.first }, { it.second })
        .mapNotNull { (crop, readings) ->
            val minimum = crop.minMutationsBeforeDecay ?: return@mapNotNull null
            val latest = readings.maxBy { it.readAt }
            CombinedReading(crop, minimum, latest.combinedRemaining!!, latest.readAt)
        }

    private fun inferFromCombinedReadings(layout: PlotLayout): Boolean {
        var isChanged = false

        combinedReadingsOf(layout).forEach { combined ->
            val members = layout.plants.filter { it.cropDef == combined.crop }
            val known = members.filter { !it.mutationsSpawnedIsMinimum }
            val unknown = members.filter { it.mutationsSpawnedIsMinimum && it.mutationsSpawned < combined.minimum }
            if (unknown.isEmpty()) return@forEach

            val remaining = combined.remaining - known.sumOf { owedBy(it.mutationsSpawned, combined.minimum) }
            when {
                remaining < 0 -> Unit
                remaining == unknown.sumOf { mostOwedBy(it.mutationsSpawned, combined.minimum) } -> unknown.forEach {
                    isChanged = noteDerivedCount(it, maxOf(it.mutationsSpawned, 1), combined, isExact = true) || isChanged
                }
                remaining == 0 -> unknown.filter { it.mutationsSpawned >= 1 }.forEach {
                    isChanged = noteDerivedCount(it, combined.minimum, combined, isExact = false) || isChanged
                }
                unknown.size == 1 -> {
                    val plant = unknown.single()
                    val timesMutated = combined.minimum - remaining
                    if (timesMutated >= maxOf(plant.mutationsSpawned, 1)) {
                        isChanged = noteDerivedCount(plant, timesMutated, combined, isExact = true) || isChanged
                    }
                }
            }
        }
        return isChanged
    }

    private fun noteDerivedCount(plant: Plant, timesMutated: Int, combined: CombinedReading, isExact: Boolean): Boolean {
        val reading = plant.lastDiagnosisReading
        if (reading != null && reading.isStillExact) return false
        if (!isExact && (reading?.timesMutatedNow ?: 0) >= timesMutated) return false

        plant.lastDiagnosisReading = DiagnosisReading(timesMutated, null, combined.readAt, isStillExact = isExact)
        return true
    }

    private fun reportCombinedMismatches(layout: PlotLayout) {
        combinedReadingsOf(layout).forEach { combined ->
            val members = layout.plants.filter { it.cropDef == combined.crop }
            if (members.any { it.mutationsSpawnedIsMinimum && it.mutationsSpawned < combined.minimum }) return@forEach

            val counted = members.sumOf { owedBy(it.mutationsSpawned, combined.minimum) }
            if (counted == combined.remaining) return@forEach

            val reportKey = "${layout.displayName()}|${combined.crop.name}|${combined.readAt}"
            if (!reportedMismatches.add(reportKey)) return@forEach

            val suspects = members.filter { it.lastDiagnosisReading?.let { reading -> reading.isFromThisTick && reading.isStillExact } != true }
            val listed = suspects.ifEmpty { members }.joinToString(", ") { "(${it.slot.x},${it.slot.y}) counted ${it.mutationsSpawned}" }
            Common.LOGGER.info(
                "[mutation count] ${layout.displayName()} ${combined.crop.name}: Combined Mutates Remaining reads ${combined.remaining} " +
                        "but the counts give $counted, off by ${combined.remaining - counted}. Not read this tick: $listed"
            )
        }
    }
}
