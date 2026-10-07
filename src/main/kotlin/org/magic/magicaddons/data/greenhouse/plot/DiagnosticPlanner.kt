package org.magic.magicaddons.data.greenhouse.plot

import kotlin.math.abs
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.Plant

object DiagnosticPlanner {

    private const val EPSILON: Double = 1e-9

    var isLinePinned: Boolean = false

    fun needsDiagnosing(plant: Plant): Boolean {
        val minimum = plant.cropDef.minMutationsBeforeDecay ?: return false
        if (plant.cropDef.decayTimeMs == NEVER_DECAYS || plant.isGrowingTeleporter) return false

        return plant.mutationsSpawnedIsMinimum && plant.mutationsSpawned < minimum
    }

    fun plantsToDiagnose(layout: PlotLayout, contributorsByBlind: Map<BlindSpawns, List<Plant>>): Set<Plant> {
        val unknownBlindKeys = contributorsByBlind.keys.filter { !it.isExact }
        val unknownBlinds = unknownBlindKeys.map { contributorsByBlind.getValue(it).toSet() }
        val plantsWithOwnUnknown = layout.plants.filter { !it.isMutationCountTracked || it.hasUncertainCredit }
        val columnCount = unknownBlinds.size + plantsWithOwnUnknown.size
        val ownColumnByPlant = plantsWithOwnUnknown.withIndex().associate { (index, plant) -> plant to unknownBlinds.size + index }

        fun rowOf(plant: Plant): DoubleArray {
            val row = DoubleArray(columnCount)
            unknownBlinds.forEachIndexed { index, contributors -> if (plant in contributors) row[index] = 1.0 }
            ownColumnByPlant[plant]?.let { row[it] = 1.0 }
            return row
        }

        val basis = Basis(columnCount)
        layout.plants.filter { it.lastDiagnosisReading?.isStillExact == true }.forEach { basis.add(rowOf(it)) }
        contributorsByBlind.keys.filter { it.drawId != null }.groupBy { it.drawId }.values
            .filter { draw -> draw.size == draw.first().drawSize }
            .forEach { draw ->
                val row = DoubleArray(columnCount)
                unknownBlindKeys.forEachIndexed { index, blind -> if (blind in draw) row[index] = 1.0 }
                basis.add(row)
            }

        val targets = layout.plants.filter(::needsDiagnosing).associateWith(::rowOf).toMutableMap()
        val chosen = LinkedHashSet<Plant>(targets.filterValues { row -> row.all { it == 0.0 } }.keys)
        targets.keys.removeAll(chosen)

        while (true) {
            targets.entries.removeAll { (_, row) -> basis.contains(row) }
            if (targets.isEmpty()) return chosen

            val residueByTarget = targets.mapValues { (_, row) -> basis.residueOf(row) }
            val best = layout.plants
                .filter { it !in chosen }
                .map { candidate ->
                    val residue = basis.residueOf(rowOf(candidate))
                    candidate to residueByTarget.count { (_, targetResidue) -> isMultipleOf(targetResidue, residue) }
                }
                .filter { (_, covered) -> covered > 0 }
                .maxWithOrNull(compareBy({ (_, covered) -> covered }, { (candidate, _) -> candidate in targets }))
                ?.first ?: return chosen + targets.keys

            chosen += best
            basis.add(rowOf(best))
        }
    }

    private fun isMultipleOf(vector: DoubleArray, of: DoubleArray): Boolean {
        val pivot = of.indexOfFirst { abs(it) > EPSILON }
        if (pivot < 0) return false

        val scale = vector[pivot] / of[pivot]
        return vector.indices.all { abs(vector[it] - scale * of[it]) <= EPSILON }
    }

    private class Basis(private val columnCount: Int) {
        private val rows = mutableListOf<DoubleArray>()
        private val pivots = mutableListOf<Int>()

        fun residueOf(vector: DoubleArray): DoubleArray {
            val residue = vector.copyOf()
            rows.forEachIndexed { index, row ->
                val factor = residue[pivots[index]]
                if (abs(factor) > EPSILON) for (column in 0 until columnCount) residue[column] -= factor * row[column]
            }
            return residue
        }

        fun contains(vector: DoubleArray): Boolean = residueOf(vector).all { abs(it) <= EPSILON }

        fun add(vector: DoubleArray) {
            val residue = residueOf(vector)
            val pivot = residue.indexOfFirst { abs(it) > EPSILON }
            if (pivot < 0) return

            val scale = residue[pivot]
            for (column in 0 until columnCount) residue[column] /= scale
            rows.forEach { row ->
                val factor = row[pivot]
                if (abs(factor) > EPSILON) for (column in 0 until columnCount) row[column] -= factor * residue[column]
            }
            rows += residue
            pivots += pivot
        }
    }
}
