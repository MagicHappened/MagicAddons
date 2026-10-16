package org.magic.magicaddons.data.greenhouse.plot

import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.ChorusFruit
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime

class Diagnosis(
    val timesMutated: Int,
    val combinedMutatesRemaining: Int?,
    val diagnosedAt: Long,
    var spawnsSeenSince: Int = 0,
    var isExact: Boolean = true
) {
    val timesMutatedNow: Int get() = timesMutated + spawnsSeenSince

    val isFromCurrentTick: Boolean get() = GreenhouseTickTime.hasGrowthTickPassedSince(diagnosedAt) == false
}

class UncertainSpawns(
    val spawnX: Int,
    val spawnY: Int,
    val spawnedCropId: String,
    val contributorCells: Set<Pair<Int, Int>>,
    val createdAt: Long,
    val isInEveryDiagnosis: Boolean,
    var minimumCount: Int = 0,
    var isCountExact: Boolean = false,
    val maximumCount: Int? = null,
    val drawGroupId: String? = null,
    val drawCreditedCount: Int = 0,
    val drawCandidateCount: Int = 0
) {
    fun contributorsIn(layout: PlotLayout): List<Plant> = layout.plants.filter { plant ->
        (plant.slot.x to plant.slot.y) in contributorCells && (plant.appearedAt ?: 0L) <= createdAt
    }

    fun isIncludedIn(diagnosis: Diagnosis): Boolean = isInEveryDiagnosis || createdAt <= diagnosis.diagnosedAt
}

object TimesMutatedCounter {

    private class ChorusSpawnSpot(val x: Int, val y: Int, val spawnCredit: PlotPrediction.SpawnCredit)

    private fun chorusSpawnSpotsOf(layout: PlotLayout): List<ChorusSpawnSpot> {
        val chorus = ChorusFruit.definition
        return (0 until layout.size).flatMap { y ->
            (0 until layout.size).mapNotNull { x ->
                val plantOnSpot = layout.plantCovering(x, y)
                if (PlotPrediction.missingNeighbourConditions(layout, chorus, x, y, ignoredPlant = plantOnSpot).isNotEmpty()) return@mapNotNull null

                ChorusSpawnSpot(x, y, PlotPrediction.spawnCreditOf(layout, chorus, x, y, ignoredPlant = plantOnSpot))
            }
        }
    }

    fun incrementSpawnCounts(layout: PlotLayout, spawn: Plant, uncertainSpawns: MutableList<UncertainSpawns>) {
        val crop = spawn.cropDef
        val spawnX = spawn.slot.x
        val spawnY = spawn.slot.y

        if (crop == ChorusFruit.definition) {
            val isOnItsSpawnSpot = spawn.highestStage == 1 &&
                    PlotPrediction.missingNeighbourConditions(layout, crop, spawnX, spawnY, ignoredPlant = spawn).isEmpty()
            if (!isOnItsSpawnSpot) return
        }

        val spawnCredit = PlotPrediction.spawnCreditOf(layout, crop, spawnX, spawnY, ignoredPlant = spawn)
        spawnCredit.certain.forEach {
            it.seenTimesMutated++
            addSpawnAfterDiagnosis(it)
        }

        val now = System.currentTimeMillis()
        spawnCredit.draws.forEach { draw ->
            val drawGroupId = "$spawnX,$spawnY,${crop.elementId},$now,${draw.candidates.first().cropDef.name}"
            draw.candidates.forEach { candidate ->
                uncertainSpawns += UncertainSpawns(
                    spawnX, spawnY, crop.elementId, setOf(candidate.slot.x to candidate.slot.y), now, isInEveryDiagnosis = false,
                    maximumCount = 1, drawGroupId = drawGroupId, drawCreditedCount = draw.credited, drawCandidateCount = draw.candidates.size
                )
            }
        }
    }

    private fun markCountUncertain(plant: Plant) {
        plant.isTimesMutatedTracked = false
        plant.hasUncertainCredit = true
        plant.lastDiagnosis?.isExact = false
    }

    private fun addSpawnAfterDiagnosis(plant: Plant) {
        val diagnosis = plant.lastDiagnosis ?: return
        if (!diagnosis.isExact) return

        when (GreenhouseTickTime.hasGrowthTickPassedSince(diagnosis.diagnosedAt)) {
            true -> diagnosis.spawnsSeenSince++
            false -> Unit
            null -> diagnosis.isExact = false
        }
    }

    fun addUnwatchedSpawns(layout: PlotLayout, uncertainSpawns: MutableList<UncertainSpawns>) {
        layout.plants.forEach { it.lastDiagnosis?.isExact = false }

        val now = System.currentTimeMillis()
        val chorusId = ChorusFruit.definition.elementId
        chorusSpawnSpotsOf(layout).forEach { spot ->
            spot.spawnCredit.uncertain.forEach(::markCountUncertain)
            val contributorCells = spot.spawnCredit.certain.map { it.slot.x to it.slot.y }.toSet()
            if (contributorCells.isEmpty()) return@forEach

            val existingGroup = uncertainSpawns.firstOrNull { group ->
                !group.isInEveryDiagnosis && group.spawnX == spot.x && group.spawnY == spot.y && group.spawnedCropId == chorusId &&
                        group.contributorsIn(layout).map { it.slot.x to it.slot.y }.toSet() == contributorCells
            }
            if (existingGroup != null) {
                existingGroup.isCountExact = false
            } else {
                uncertainSpawns += UncertainSpawns(spot.x, spot.y, chorusId, contributorCells, now, isInEveryDiagnosis = false)
            }
        }
    }

    private fun addSpawnsInEveryDiagnosis(layout: PlotLayout, uncertainSpawns: MutableList<UncertainSpawns>) {
        val now = System.currentTimeMillis()
        val chorusId = ChorusFruit.definition.elementId
        chorusSpawnSpotsOf(layout).forEach { spot ->
            val contributingPlants = spot.spawnCredit.certain + spot.spawnCredit.uncertain
            if (contributingPlants.isEmpty() || contributingPlants.any { it.isCountedFromStart }) return@forEach

            if (uncertainSpawns.any { it.isInEveryDiagnosis && it.spawnX == spot.x && it.spawnY == spot.y && it.spawnedCropId == chorusId }) return@forEach

            spot.spawnCredit.uncertain.forEach(::markCountUncertain)
            spot.spawnCredit.certain.filterNot { it.hasUncertainCredit }.forEach { it.isTimesMutatedTracked = true }
            uncertainSpawns += UncertainSpawns(
                spot.x, spot.y, chorusId, spot.spawnCredit.certain.map { it.slot.x to it.slot.y }.toSet(), now, isInEveryDiagnosis = true
            )
        }
    }

    fun applyDiagnosis(layout: PlotLayout, plant: Plant, diagnosis: Diagnosis, uncertainSpawns: MutableList<UncertainSpawns>) {
        plant.lastDiagnosis = diagnosis
        val uncertainInDiagnosis = uncertainSpawns.filter { it.isIncludedIn(diagnosis) && plant in it.contributorsIn(layout) }

        if (uncertainInDiagnosis.any { !it.isCountExact } || chorusSpawnSpotsOf(layout).any { plant in it.spawnCredit.certain }) return

        plant.seenTimesMutated = diagnosis.timesMutated - uncertainInDiagnosis.sumOf { it.minimumCount }
        plant.isTimesMutatedTracked = true
        plant.hasUncertainCredit = false
    }

    private val plotsWarnedUnsolvable = HashSet<String>()

    fun countInputsSignatureOf(layout: PlotLayout, uncertainSpawns: List<UncertainSpawns>): Int {
        var signature = 17
        fun mix(value: Any?) {
            signature = signature * 31 + value.hashCode()
        }
        layout.plants.forEach { plant ->
            mix(plant.slot.x); mix(plant.slot.y); mix(plant.elementId); mix(plant.appearedAt); mix(plant.isHalted)
            mix(plant.seenTimesMutated); mix(plant.isTimesMutatedTracked); mix(plant.hasUncertainCredit); mix(plant.isCountedFromStart)
            mix(plant.timesMutated); mix(plant.isTimesMutatedMinimum)
            plant.lastDiagnosis?.let { diagnosis ->
                mix(diagnosis.timesMutated); mix(diagnosis.combinedMutatesRemaining); mix(diagnosis.diagnosedAt)
                mix(diagnosis.spawnsSeenSince); mix(diagnosis.isExact); mix(diagnosis.isFromCurrentTick)
            }
        }
        uncertainSpawns.forEach { group ->
            mix(group.spawnX); mix(group.spawnY); mix(group.spawnedCropId); mix(group.contributorCells); mix(group.createdAt)
            mix(group.minimumCount); mix(group.isCountExact); mix(group.drawGroupId)
        }
        return signature
    }

    fun recountPlot(layout: PlotLayout, uncertainSpawns: MutableList<UncertainSpawns>): Map<UncertainSpawns, List<Plant>> {
        addSpawnsInEveryDiagnosis(layout, uncertainSpawns)

        val contributorsByUncertain = uncertainSpawns.associateWith { it.contributorsIn(layout) }
        uncertainSpawns.removeAll { contributorsByUncertain[it].isNullOrEmpty() }
        val contributorsByLiveUncertain = contributorsByUncertain.filterValues { it.isNotEmpty() }

        val solvedCounts = UncertainSpawnSolver.solve(layout, contributorsByLiveUncertain)
        if (solvedCounts == null) {
            if (plotsWarnedUnsolvable.add(layout.displayName())) {
                Common.LOGGER.warn("Mutation counts in ${layout.displayName()} don't fit the diagnostics readings, keeping the previous counts")
            }
        } else {
            plotsWarnedUnsolvable.remove(layout.displayName())
            solvedCounts.spawnsByBlind.forEach { (group, spawns) ->
                group.minimumCount = spawns.first
                group.isCountExact = spawns.first == spawns.last
            }
        }

        val uncertainByPlant = contributorsByLiveUncertain.entries
            .flatMap { (group, contributors) -> contributors.map { it to group } }
            .groupBy({ it.first }, { it.second })

        fun updateTimesMutated(plant: Plant) {
            val plantGroups = uncertainByPlant[plant].orEmpty()
            val solvedUncertainCount = solvedCounts?.blindSpawnsByPlant?.get(plant)?.takeIf { it.first == it.last }
            val uncertainSpawnCount = solvedUncertainCount?.first ?: plantGroups.sumOf { it.minimumCount }
            val isCountExact = plant.isTimesMutatedTracked && !plant.hasUncertainCredit &&
                    (solvedUncertainCount != null || plantGroups.all { it.isCountExact })

            val diagnosis = plant.lastDiagnosis
            val uncertainAfterDiagnosis = diagnosis?.let { plantGroups.filterNot { group -> group.isIncludedIn(it) } }.orEmpty()
            val isDiagnosisExact = diagnosis?.isExact == true && uncertainAfterDiagnosis.all { it.isCountExact }
            val diagnosedTimesMutated = (diagnosis?.timesMutatedNow ?: 0) + uncertainAfterDiagnosis.sumOf { it.minimumCount }

            plant.timesMutated = maxOf(plant.seenTimesMutated + uncertainSpawnCount, diagnosedTimesMutated)
            plant.isTimesMutatedMinimum = !isCountExact && !isDiagnosisExact
        }

        layout.plants.forEach(::updateTimesMutated)
        if (deriveCountsFromCombinedDiagnoses(layout)) layout.plants.forEach(::updateTimesMutated)
        reportCombinedMismatches(layout)
        return contributorsByLiveUncertain
    }

    private class CombinedDiagnosis(val crop: CropDefinition, val minMutationsBeforeDecay: Int, val combinedMutatesRemaining: Int, val diagnosedAt: Long)

    private val reportedCombinedMismatches = HashSet<String>()

    fun combinedMutatesRemainingOf(timesMutatedOfPlants: List<Int>, minMutationsBeforeDecay: Int): Int =
        timesMutatedOfPlants.filter { it >= 1 }.sumOf { minMutationsBeforeDecay - it }.coerceAtLeast(0)

    private fun combinedDiagnosesOf(layout: PlotLayout): List<CombinedDiagnosis> = layout.plants
        .mapNotNull { plant ->
            val diagnosis = plant.lastDiagnosis?.takeIf { it.isFromCurrentTick } ?: return@mapNotNull null
            val remaining = diagnosis.combinedMutatesRemaining ?: return@mapNotNull null
            Triple(plant.cropDef, remaining, diagnosis.diagnosedAt)
        }
        .groupBy { it.first }
        .mapNotNull { (crop, diagnoses) ->
            val minMutationsBeforeDecay = crop.minMutationsBeforeDecay ?: return@mapNotNull null
            val latest = diagnoses.maxBy { it.third }
            CombinedDiagnosis(crop, minMutationsBeforeDecay, latest.second, latest.third)
        }

    private fun deriveCountsFromCombinedDiagnoses(layout: PlotLayout): Boolean {
        var hasDerivedCount = false

        combinedDiagnosesOf(layout).forEach { combined ->
            if (combined.combinedMutatesRemaining <= 0) return@forEach
            val cropPlants = layout.plants.filter { it.cropDef == combined.crop }
            val (uncertainPlants, exactPlants) = cropPlants.partition { it.isTimesMutatedMinimum }
            if (uncertainPlants.isEmpty()) return@forEach

            val exactPoolPlants = exactPlants.filter { it.timesMutated >= 1 }
            val exactPoolTimesMutated = exactPoolPlants.sumOf { it.timesMutated }

            if (uncertainPlants.all { it.timesMutated >= 1 }) {
                val uncertainTimesMutated = (exactPoolPlants.size + uncertainPlants.size) * combined.minMutationsBeforeDecay -
                        combined.combinedMutatesRemaining - exactPoolTimesMutated
                when {
                    uncertainPlants.size == 1 && uncertainTimesMutated >= uncertainPlants.single().timesMutated ->
                        hasDerivedCount = noteDerivedCount(uncertainPlants.single(), uncertainTimesMutated, combined) || hasDerivedCount
                    uncertainTimesMutated == uncertainPlants.sumOf { it.timesMutated } ->
                        uncertainPlants.forEach { hasDerivedCount = noteDerivedCount(it, it.timesMutated, combined) || hasDerivedCount }
                }
                return@forEach
            }

            val plant = uncertainPlants.singleOrNull() ?: return@forEach
            val isOutOfPool = exactPoolPlants.sumOf { combined.minMutationsBeforeDecay - it.timesMutated } == combined.combinedMutatesRemaining
            val timesMutatedIfInPool = (exactPoolPlants.size + 1) * combined.minMutationsBeforeDecay - combined.combinedMutatesRemaining - exactPoolTimesMutated
            val isInPool = timesMutatedIfInPool >= maxOf(plant.timesMutated, 1)
            when {
                isOutOfPool && !isInPool -> hasDerivedCount = noteDerivedCount(plant, 0, combined) || hasDerivedCount
                isInPool && !isOutOfPool -> hasDerivedCount = noteDerivedCount(plant, timesMutatedIfInPool, combined) || hasDerivedCount
            }
        }
        return hasDerivedCount
    }

    private fun noteDerivedCount(plant: Plant, timesMutated: Int, combined: CombinedDiagnosis): Boolean {
        val diagnosis = plant.lastDiagnosis
        if (diagnosis != null && diagnosis.isExact) return false

        plant.lastDiagnosis = Diagnosis(timesMutated, null, combined.diagnosedAt)
        return true
    }

    private fun reportCombinedMismatches(layout: PlotLayout) {
        combinedDiagnosesOf(layout).forEach { combined ->
            val cropPlants = layout.plants.filter { it.cropDef == combined.crop }
            if (cropPlants.any { it.isTimesMutatedMinimum }) return@forEach

            val countedRemaining = combinedMutatesRemainingOf(cropPlants.map { it.timesMutated }, combined.minMutationsBeforeDecay)
            if (countedRemaining == combined.combinedMutatesRemaining) return@forEach

            val reportKey = "${layout.displayName()}|${combined.crop.name}|${combined.diagnosedAt}"
            if (!reportedCombinedMismatches.add(reportKey)) return@forEach

            val suspects = cropPlants.filter { it.lastDiagnosis?.let { diagnosis -> diagnosis.isFromCurrentTick && diagnosis.isExact } != true }
            val suspectList = suspects.ifEmpty { cropPlants }.joinToString(", ") { "(${it.slot.x},${it.slot.y}) counted ${it.timesMutated}" }
            Common.LOGGER.info(
                "[mutation count] ${layout.displayName()} ${combined.crop.name}: Combined Mutates Remaining reads ${combined.combinedMutatesRemaining} " +
                        "but the counts give $countedRemaining, off by ${combined.combinedMutatesRemaining - countedRemaining}. Not read this tick: $suspectList"
            )
        }
    }
}
