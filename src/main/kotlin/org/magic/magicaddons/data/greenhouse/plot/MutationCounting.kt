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
    var isExact: Boolean = false,
    val atMost: Int? = null,
    val drawId: String? = null,
    val drawCredited: Int = 0,
    val drawSize: Int = 0
) {
    fun contributorsIn(layout: PlotLayout): List<Plant> = layout.plants.filter { plant ->
        (plant.slot.x to plant.slot.y) in contributorSlots && (plant.appearedAt ?: 0L) <= createdAt
    }

    fun isCountedIn(reading: DiagnosisReading): Boolean = isBeforeTracking || createdAt <= reading.readAt
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

    fun countSpawn(layout: PlotLayout, spawn: Plant, blindSpawns: MutableList<BlindSpawns>) {
        val crop = spawn.cropDef
        val spawnX = spawn.slot.x
        val spawnY = spawn.slot.y

        if (crop.teleportsWhileGrowing) {
            val isOnItsSpawnSpot = spawn.highestStage == 1 &&
                    PlotPrediction.missingNeighbourConditions(layout, crop, spawnX, spawnY, ignoredPlant = spawn).isEmpty()
            if (!isOnItsSpawnSpot) return
        }

        val credit = PlotPrediction.spawnCreditOf(layout, crop, spawnX, spawnY, ignoredPlant = spawn)
        credit.certain.forEach {
            it.seenSpawnsHelped++
            creditReading(it)
        }

        val now = System.currentTimeMillis()
        credit.draws.forEach { draw ->
            val drawId = "$spawnX,$spawnY,${crop.elementId},$now,${draw.candidates.first().cropDef.name}"
            draw.candidates.forEach { candidate ->
                blindSpawns += BlindSpawns(
                    spawnX, spawnY, crop.elementId, setOf(candidate.slot.x to candidate.slot.y), now, isBeforeTracking = false,
                    atMost = 1, drawId = drawId, drawCredited = draw.credited, drawSize = draw.candidates.size
                )
            }
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

    fun noteDiagnosis(layout: PlotLayout, plant: Plant, reading: DiagnosisReading, blindSpawns: MutableList<BlindSpawns>) {
        plant.lastDiagnosisReading = reading
        val blindsInReading = blindSpawns.filter { it.isCountedIn(reading) && plant in it.contributorsIn(layout) }

        if (blindsInReading.any { !it.isExact } || teleporterSpotsOf(layout).any { plant in it.credit.certain }) return

        plant.seenSpawnsHelped = reading.timesMutated - blindsInReading.sumOf { it.atLeast }
        plant.isMutationCountTracked = true
        plant.hasUncertainCredit = false
    }

    private val warnedUnsolvedPlots = HashSet<String>()

    fun signatureOf(layout: PlotLayout, blindSpawns: List<BlindSpawns>): Int {
        var signature = 17
        fun mix(value: Any?) {
            signature = signature * 31 + value.hashCode()
        }
        layout.plants.forEach { plant ->
            mix(plant.slot.x); mix(plant.slot.y); mix(plant.elementId); mix(plant.appearedAt); mix(plant.isHalted)
            mix(plant.seenSpawnsHelped); mix(plant.isMutationCountTracked); mix(plant.hasUncertainCredit); mix(plant.isCountedFromStart)
            mix(plant.mutationsSpawned); mix(plant.mutationsSpawnedIsMinimum)
            plant.lastDiagnosisReading?.let { reading ->
                mix(reading.timesMutated); mix(reading.combinedRemaining); mix(reading.readAt)
                mix(reading.spawnsSeenSince); mix(reading.isStillExact); mix(reading.isFromThisTick)
            }
        }
        blindSpawns.forEach { blind ->
            mix(blind.x); mix(blind.y); mix(blind.cropId); mix(blind.contributorSlots); mix(blind.createdAt)
            mix(blind.atLeast); mix(blind.isExact); mix(blind.drawId)
        }
        return signature
    }

    fun recountPlot(layout: PlotLayout, blindSpawns: MutableList<BlindSpawns>): Map<BlindSpawns, List<Plant>> {
        addSpawnsBeforeTracking(layout, blindSpawns)

        val contributorsByBlind = blindSpawns.associateWith { it.contributorsIn(layout) }
        blindSpawns.removeAll { contributorsByBlind[it].isNullOrEmpty() }
        val liveContributors = contributorsByBlind.filterValues { it.isNotEmpty() }

        val solution = BlindSpawnSolver.solve(layout, liveContributors)
        if (solution == null) {
            if (warnedUnsolvedPlots.add(layout.displayName())) {
                Common.LOGGER.warn("Mutation counts in ${layout.displayName()} don't fit the diagnostics readings, keeping the previous counts")
            }
        } else {
            warnedUnsolvedPlots.remove(layout.displayName())
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
            val blindsAfterReading = reading?.let { blinds.filterNot { blind -> blind.isCountedIn(it) } }.orEmpty()
            val isReadingExact = reading?.isStillExact == true && blindsAfterReading.all { it.isExact }
            val readTimesMutated = (reading?.timesMutatedNow ?: 0) + blindsAfterReading.sumOf { it.atLeast }

            plant.mutationsSpawned = maxOf(plant.seenSpawnsHelped + blindSpawnCount, readTimesMutated)
            plant.mutationsSpawnedIsMinimum = !isCounted && !isReadingExact
        }

        layout.plants.forEach(::updateCount)
        if (inferFromCombinedReadings(layout)) layout.plants.forEach(::updateCount)
        reportCombinedMismatches(layout)
        return liveContributors
    }

    private class CombinedReading(val crop: CropDefinition, val minimum: Int, val remaining: Int, val readAt: Long)

    private val reportedMismatches = HashSet<String>()

    fun combinedRemainingOf(timesMutatedByPlant: List<Int>, minimum: Int): Int =
        timesMutatedByPlant.filter { it >= 1 }.sumOf { minimum - it }.coerceAtLeast(0)

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
            if (combined.remaining <= 0) return@forEach
            val members = layout.plants.filter { it.cropDef == combined.crop }
            val (unknown, known) = members.partition { it.mutationsSpawnedIsMinimum }
            if (unknown.isEmpty()) return@forEach

            val knownPool = known.filter { it.mutationsSpawned >= 1 }
            val knownTimesMutated = knownPool.sumOf { it.mutationsSpawned }

            if (unknown.all { it.mutationsSpawned >= 1 }) {
                val unknownTimesMutated = (knownPool.size + unknown.size) * combined.minimum - combined.remaining - knownTimesMutated
                when {
                    unknown.size == 1 && unknownTimesMutated >= unknown.single().mutationsSpawned ->
                        isChanged = noteDerivedCount(unknown.single(), unknownTimesMutated, combined) || isChanged
                    unknownTimesMutated == unknown.sumOf { it.mutationsSpawned } ->
                        unknown.forEach { isChanged = noteDerivedCount(it, it.mutationsSpawned, combined) || isChanged }
                }
                return@forEach
            }

            val plant = unknown.singleOrNull() ?: return@forEach
            val isOutOfPool = knownPool.sumOf { combined.minimum - it.mutationsSpawned } == combined.remaining
            val timesMutatedIfInPool = (knownPool.size + 1) * combined.minimum - combined.remaining - knownTimesMutated
            val isInPool = timesMutatedIfInPool >= maxOf(plant.mutationsSpawned, 1)
            when {
                isOutOfPool && !isInPool -> isChanged = noteDerivedCount(plant, 0, combined) || isChanged
                isInPool && !isOutOfPool -> isChanged = noteDerivedCount(plant, timesMutatedIfInPool, combined) || isChanged
            }
        }
        return isChanged
    }

    private fun noteDerivedCount(plant: Plant, timesMutated: Int, combined: CombinedReading): Boolean {
        val reading = plant.lastDiagnosisReading
        if (reading != null && reading.isStillExact) return false

        plant.lastDiagnosisReading = DiagnosisReading(timesMutated, null, combined.readAt)
        return true
    }

    private fun reportCombinedMismatches(layout: PlotLayout) {
        combinedReadingsOf(layout).forEach { combined ->
            val members = layout.plants.filter { it.cropDef == combined.crop }
            if (members.any { it.mutationsSpawnedIsMinimum }) return@forEach

            val counted = combinedRemainingOf(members.map { it.mutationsSpawned }, combined.minimum)
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
