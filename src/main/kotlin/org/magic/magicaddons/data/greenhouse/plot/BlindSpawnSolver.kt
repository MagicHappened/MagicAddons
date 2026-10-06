package org.magic.magicaddons.data.greenhouse.plot

import org.magic.magicaddons.data.greenhouse.crops.Plant

object BlindSpawnSolver {

    private const val SEARCH_NODE_LIMIT: Int = 2_000_000

    private enum class Bound { Exact, AtLeast, AtMost }

    private class Sum(val blindIndexes: IntArray, val target: Int, val bound: Bound)

    private class CombinedRemaining(val minimum: Int, val members: List<Member>, val target: Int)

    private class Member(val fixedSpawns: Int, val blindIndexes: IntArray)

    class Solution(val spawnsByBlind: Map<BlindSpawns, IntRange>, val blindSpawnsByPlant: Map<Plant, IntRange>)

    fun solve(layout: PlotLayout, contributorsByBlind: Map<BlindSpawns, List<Plant>>): Solution? {
        val blinds = contributorsByBlind.keys.toList()
        if (blinds.isEmpty()) return Solution(emptyMap(), emptyMap())

        val lowest = IntArray(blinds.size) { blinds[it].atLeast }
        val isFixed = BooleanArray(blinds.size) { blinds[it].isExact }
        val blindIndexesByPlant = blinds.indices
            .flatMap { index -> contributorsByBlind.getValue(blinds[index]).map { it to index } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, indexes) -> indexes.toIntArray() }

        val sums = blindIndexesByPlant.mapNotNull { (plant, blindIndexes) -> sumOf(plant, blindIndexes) }
        if (sums.any { it.bound != Bound.AtLeast && it.target < 0 }) return null

        val bounding = sums.filter { it.bound != Bound.AtLeast }
        val searched = BooleanArray(blinds.size) { index -> !isFixed[index] && sums.any { it.bound == Bound.Exact && index in it.blindIndexes } }
        val highest = IntArray(blinds.size) { index ->
            when {
                isFixed[index] -> lowest[index]
                searched[index] -> bounding.filter { index in it.blindIndexes }.minOf { it.target }
                else -> Int.MAX_VALUE
            }
        }
        if (blinds.indices.any { highest[it] < lowest[it] }) return null

        fun isKnown(index: Int) = isFixed[index] || searched[index]
        val combinedChecks = combinedChecksOf(layout, blindIndexesByPlant)
            .filter { check -> check.members.all { member -> member.blindIndexes.all(::isKnown) } }
        val solvablePlants = blindIndexesByPlant.filterValues { indexes -> indexes.all(::isKnown) }

        val search = Search(lowest, highest, searched, sums.filter { sum -> sum.blindIndexes.all(::isKnown) }, combinedChecks, solvablePlants)
        if (!search.run() || search.solutionCount == 0) return null

        val spawnsByBlind = blinds.indices.associate { index ->
            val range = if (searched[index]) search.lowestFound[index]..search.highestFound[index] else lowest[index]..highest[index]
            blinds[index] to range
        }
        return Solution(spawnsByBlind, search.sumRangeByPlant)
    }

    private fun sumOf(plant: Plant, blindIndexes: IntArray): Sum? {
        val reading = plant.lastDiagnosisReading ?: return null
        val target = reading.timesMutatedNow - plant.seenSpawnsHelped
        val isOwnCountKnown = plant.isMutationCountTracked && !plant.hasUncertainCredit

        return when {
            isOwnCountKnown && reading.isStillExact -> Sum(blindIndexes, target, Bound.Exact)
            isOwnCountKnown -> Sum(blindIndexes, target, Bound.AtLeast)
            reading.isStillExact -> Sum(blindIndexes, target, Bound.AtMost)
            else -> null
        }
    }

    private fun combinedChecksOf(layout: PlotLayout, blindIndexesByPlant: Map<Plant, IntArray>): List<CombinedRemaining> = layout.plants
        .filter { plant -> plant.lastDiagnosisReading?.let { it.combinedRemaining != null && it.isFromThisTick } == true }
        .groupBy { it.cropDef }
        .mapNotNull { (crop, readPlants) ->
            val minimum = crop.minMutationsBeforeDecay ?: return@mapNotNull null
            val members = layout.plants.filter { it.cropDef == crop }.map { plant ->
                val reading = plant.lastDiagnosisReading?.takeIf { it.isFromThisTick && it.isStillExact }
                when {
                    reading != null -> Member(reading.timesMutatedNow, IntArray(0))
                    plant.isMutationCountTracked && !plant.hasUncertainCredit ->
                        Member(plant.seenSpawnsHelped, blindIndexesByPlant[plant] ?: IntArray(0))
                    else -> return@mapNotNull null
                }
            }
            CombinedRemaining(minimum, members, readPlants.last().lastDiagnosisReading!!.combinedRemaining!!)
        }

    private class Search(
        val lowest: IntArray,
        val highest: IntArray,
        val searched: BooleanArray,
        val sums: List<Sum>,
        val combinedChecks: List<CombinedRemaining>,
        val solvablePlants: Map<Plant, IntArray>
    ) {
        val lowestFound = IntArray(lowest.size) { Int.MAX_VALUE }
        val highestFound = IntArray(lowest.size) { Int.MIN_VALUE }
        val sumRangeByPlant = HashMap<Plant, IntRange>()
        var solutionCount = 0
        private val spawns = lowest.copyOf()
        private val searchOrder = searched.indices.filter { searched[it] }
        private var nodesVisited = 0

        fun run(): Boolean = place(0)

        private fun place(orderIndex: Int): Boolean {
            if (++nodesVisited > SEARCH_NODE_LIMIT) return false
            if (orderIndex == searchOrder.size) {
                if (sums.all { isMet(it) } && combinedChecks.all { isMet(it) }) noteSolution()
                return true
            }
            val blindIndex = searchOrder[orderIndex]
            for (count in lowest[blindIndex]..highest[blindIndex]) {
                spawns[blindIndex] = count
                if (sums.any { isOverTarget(it, orderIndex) }) break
                if (!place(orderIndex + 1)) return false
            }
            spawns[blindIndex] = lowest[blindIndex]
            return true
        }

        private fun isOverTarget(sum: Sum, placedUpTo: Int): Boolean {
            if (sum.bound == Bound.AtLeast) return false
            val placed = searchOrder.subList(0, placedUpTo + 1)
            return sum.blindIndexes.filter { !searched[it] || it in placed }.sumOf { spawns[it] } > sum.target
        }

        private fun isMet(sum: Sum): Boolean {
            val total = sum.blindIndexes.sumOf { spawns[it] }
            return when (sum.bound) {
                Bound.Exact -> total == sum.target
                Bound.AtLeast -> total >= sum.target
                Bound.AtMost -> total <= sum.target
            }
        }

        private fun isMet(check: CombinedRemaining): Boolean {
            val remaining = check.members.sumOf { member ->
                val timesMutated = member.fixedSpawns + member.blindIndexes.sumOf { spawns[it] }
                if (timesMutated >= 1) (check.minimum - timesMutated).coerceAtLeast(0) else 0
            }
            return remaining == check.target
        }

        private fun noteSolution() {
            solutionCount++
            for (index in spawns.indices) {
                lowestFound[index] = minOf(lowestFound[index], spawns[index])
                highestFound[index] = maxOf(highestFound[index], spawns[index])
            }
            solvablePlants.forEach { (plant, blindIndexes) ->
                val sum = blindIndexes.sumOf { spawns[it] }
                val range = sumRangeByPlant[plant]
                sumRangeByPlant[plant] = if (range == null) sum..sum else minOf(range.first, sum)..maxOf(range.last, sum)
            }
        }
    }
}
