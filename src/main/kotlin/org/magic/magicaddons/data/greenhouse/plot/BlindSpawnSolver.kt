package org.magic.magicaddons.data.greenhouse.plot

import org.magic.magicaddons.data.greenhouse.crops.Plant

object BlindSpawnSolver {

    private const val SEARCH_NODE_LIMIT: Int = 2_000_000

    private enum class Bound { Exact, AtLeast, AtMost }

    private class Sum(val blindIndexes: IntArray, val target: Int, val bound: Bound, val isFromReading: Boolean)

    private class CombinedRemaining(val minimum: Int, val members: List<Member>, val target: Int)

    private class Member(val fixedSpawns: Int, val blindIndexes: IntArray)

    class Solution(val spawnsByBlind: Map<BlindSpawns, IntRange>, val blindSpawnsByPlant: Map<Plant, IntRange>)

    fun solve(layout: PlotLayout, contributorsByBlind: Map<BlindSpawns, List<Plant>>): Solution? {
        val blinds = contributorsByBlind.keys.toList()
        if (blinds.isEmpty()) return Solution(emptyMap(), emptyMap())

        val lowest = IntArray(blinds.size) { blinds[it].atLeast }
        val isFixed = BooleanArray(blinds.size) { blinds[it].isExact }
        val cap = IntArray(blinds.size) { blinds[it].atMost ?: Int.MAX_VALUE }
        val blindIndexesByPlant = blinds.indices
            .flatMap { index -> contributorsByBlind.getValue(blinds[index]).map { it to index } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, indexes) -> indexes.toIntArray() }

        val readingSums = blindIndexesByPlant.mapNotNull { (plant, blindIndexes) -> readingSumOf(plant, blindIndexes, blinds) }
        val drawSums = drawSumsOf(blinds)
        val sums = readingSums + drawSums
        if (sums.any { it.bound != Bound.AtLeast && it.target < 0 }) return null

        val highest = IntArray(blinds.size) { index ->
            if (isFixed[index]) return@IntArray lowest[index]
            sums.filter { it.bound != Bound.AtLeast && index in it.blindIndexes }.minOfOrNull { it.target }?.coerceAtMost(cap[index]) ?: cap[index]
        }
        if (blinds.indices.any { highest[it] < lowest[it] }) return null

        val searched = searchedBlinds(blinds.size, isFixed, highest, readingSums, drawSums)
        fun isKnown(index: Int) = isFixed[index] || searched[index]

        val combinedChecks = combinedChecksOf(layout, blindIndexesByPlant)
            .filter { check -> check.members.all { member -> member.blindIndexes.all(::isKnown) } }
        val usedSums = sums.filter { sum -> sum.blindIndexes.all(::isKnown) }
        val solvablePlants = blindIndexesByPlant.filterValues { indexes -> indexes.all(::isKnown) }

        val lowestFound = lowest.copyOf()
        val highestFound = highest.copyOf()
        val partialRangeByPlant = HashMap<Plant, IntRange>()

        for (component in componentsOf(searched, usedSums, combinedChecks)) {
            val inComponent = BooleanArray(blinds.size) { it in component }
            val search = Search(
                lowest, highest, component,
                usedSums.filter { sum -> sum.blindIndexes.any { inComponent[it] } },
                combinedChecks.filter { check -> check.members.any { member -> member.blindIndexes.any { inComponent[it] } } },
                solvablePlants.mapValues { (_, indexes) -> indexes.filter { inComponent[it] }.toIntArray() }.filterValues { it.isNotEmpty() }
            )
            if (!search.run() || search.solutionCount == 0) return null

            component.forEach { index ->
                lowestFound[index] = search.lowestFound[index]
                highestFound[index] = search.highestFound[index]
            }
            search.partialRangeByPlant.forEach { (plant, range) ->
                val known = partialRangeByPlant[plant]
                partialRangeByPlant[plant] = if (known == null) range else (known.first + range.first)..(known.last + range.last)
            }
        }

        val sumRangeByPlant = solvablePlants.mapValues { (plant, indexes) ->
            val fixedSum = indexes.filterNot { searched[it] }.sumOf { lowest[it] }
            val partial = partialRangeByPlant[plant] ?: 0..0
            (fixedSum + partial.first)..(fixedSum + partial.last)
        }
        val spawnsByBlind = blinds.indices.associate { index -> blinds[index] to lowestFound[index]..highestFound[index] }
        return Solution(spawnsByBlind, sumRangeByPlant)
    }

    private fun readingSumOf(plant: Plant, blindIndexes: IntArray, blinds: List<BlindSpawns>): Sum? {
        val reading = plant.lastDiagnosisReading ?: return null
        val indexesInReading = blindIndexes.filter { blinds[it].isCountedIn(reading) }.toIntArray()
        val target = reading.timesMutatedNow - plant.seenSpawnsHelped
        val isOwnCountKnown = plant.isMutationCountTracked && !plant.hasUncertainCredit
        val isExact = reading.isStillExact

        return when {
            isOwnCountKnown && isExact -> Sum(indexesInReading, target, Bound.Exact, isFromReading = true)
            isOwnCountKnown -> Sum(indexesInReading, target, Bound.AtLeast, isFromReading = true)
            isExact -> Sum(indexesInReading, target, Bound.AtMost, isFromReading = true)
            else -> null
        }
    }

    private fun drawSumsOf(blinds: List<BlindSpawns>): List<Sum> = blinds.indices
        .filter { blinds[it].drawId != null }
        .groupBy { blinds[it].drawId }
        .mapNotNull { (_, indexes) ->
            val draw = blinds[indexes.first()]
            if (indexes.size != draw.drawSize) return@mapNotNull null
            Sum(indexes.toIntArray(), draw.drawCredited, Bound.Exact, isFromReading = false)
        }

    private fun searchedBlinds(count: Int, isFixed: BooleanArray, highest: IntArray, readingSums: List<Sum>, drawSums: List<Sum>): BooleanArray {
        val searched = BooleanArray(count) { index ->
            !isFixed[index] && highest[index] != Int.MAX_VALUE && readingSums.any { index in it.blindIndexes }
        }
        var isGrowing = true
        while (isGrowing) {
            isGrowing = false
            drawSums.filter { draw -> draw.blindIndexes.any { searched[it] } }.forEach { draw ->
                draw.blindIndexes.filter { !isFixed[it] && !searched[it] }.forEach {
                    searched[it] = true
                    isGrowing = true
                }
            }
        }
        return searched
    }

    private fun componentsOf(searched: BooleanArray, sums: List<Sum>, checks: List<CombinedRemaining>): List<List<Int>> {
        val parent = IntArray(searched.size) { it }
        fun root(index: Int): Int {
            var current = index
            while (parent[current] != current) current = parent[current].also { parent[current] = parent[parent[current]] }
            return current
        }
        fun join(indexes: List<Int>) {
            val searchedIndexes = indexes.filter { searched[it] }
            searchedIndexes.drop(1).forEach { parent[root(it)] = root(searchedIndexes.first()) }
        }

        sums.forEach { join(it.blindIndexes.toList()) }
        checks.forEach { check -> join(check.members.flatMap { it.blindIndexes.toList() }) }
        return searched.indices.filter { searched[it] }.groupBy(::root).values.toList()
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
        component: List<Int>,
        val sums: List<Sum>,
        val combinedChecks: List<CombinedRemaining>,
        val plantIndexes: Map<Plant, IntArray>
    ) {
        val lowestFound = IntArray(lowest.size) { Int.MAX_VALUE }
        val highestFound = IntArray(lowest.size) { Int.MIN_VALUE }
        val partialRangeByPlant = HashMap<Plant, IntRange>()
        var solutionCount = 0
        private val spawns = lowest.copyOf()
        private val sumIdsByBlind = HashMap<Int, IntArray>()
        private val searchOrder: List<Int>
        private val partial = LongArray(sums.size)
        private val stillToPlaceAtMost = LongArray(sums.size)
        private var nodesVisited = 0

        init {
            val inComponent = component.toHashSet()
            sums.forEachIndexed { sumId, sum ->
                sum.blindIndexes.forEach { index ->
                    if (index in inComponent) stillToPlaceAtMost[sumId] += highest[index].toLong() else partial[sumId] += lowest[index].toLong()
                }
            }
            component.forEach { index ->
                sumIdsByBlind[index] = sums.indices.filter { index in sums[it].blindIndexes }.toIntArray()
            }
            searchOrder = component.sortedWith(compareBy({ -(sumIdsByBlind[it]?.size ?: 0) }, { highest[it] - lowest[it] }))
            component.forEach { index -> sumIdsByBlind.getValue(index).forEach { partial[it] += lowest[index].toLong() } }
            component.forEach { index -> sumIdsByBlind.getValue(index).forEach { stillToPlaceAtMost[it] -= lowest[index].toLong() } }
        }

        fun run(): Boolean = place(0)

        private fun place(orderIndex: Int): Boolean {
            if (++nodesVisited > SEARCH_NODE_LIMIT) return false
            if (orderIndex == searchOrder.size) {
                if (sums.indices.all { isMet(it) } && combinedChecks.all { isMet(it) }) noteSolution()
                return true
            }
            val blindIndex = searchOrder[orderIndex]
            val sumIds = sumIdsByBlind.getValue(blindIndex)
            val span = (highest[blindIndex] - lowest[blindIndex]).toLong()
            sumIds.forEach { stillToPlaceAtMost[it] -= span }

            var isWithinLimit = true
            for (count in lowest[blindIndex]..highest[blindIndex]) {
                spawns[blindIndex] = count
                if (count > lowest[blindIndex]) sumIds.forEach { partial[it]++ }
                if (sumIds.any { isOver(it) }) break
                if (sumIds.any { isShort(it) }) continue
                if (!place(orderIndex + 1)) {
                    isWithinLimit = false
                    break
                }
            }

            sumIds.forEach {
                partial[it] -= (spawns[blindIndex] - lowest[blindIndex]).toLong()
                stillToPlaceAtMost[it] += span
            }
            spawns[blindIndex] = lowest[blindIndex]
            return isWithinLimit
        }

        private fun isOver(sumId: Int): Boolean = sums[sumId].bound != Bound.AtLeast && partial[sumId] > sums[sumId].target

        private fun isShort(sumId: Int): Boolean =
            sums[sumId].bound != Bound.AtMost && partial[sumId] + stillToPlaceAtMost[sumId] < sums[sumId].target

        private fun isMet(sumId: Int): Boolean {
            val total = partial[sumId]
            val target = sums[sumId].target.toLong()
            return when (sums[sumId].bound) {
                Bound.Exact -> total == target
                Bound.AtLeast -> total >= target
                Bound.AtMost -> total <= target
            }
        }

        private fun isMet(check: CombinedRemaining): Boolean {
            val timesMutatedByMember = check.members.map { member -> member.fixedSpawns + member.blindIndexes.sumOf { spawns[it] } }
            return MutationCounting.combinedRemainingOf(timesMutatedByMember, check.minimum) == check.target
        }

        private fun noteSolution() {
            solutionCount++
            searchOrder.forEach { index ->
                lowestFound[index] = minOf(lowestFound[index], spawns[index])
                highestFound[index] = maxOf(highestFound[index], spawns[index])
            }
            plantIndexes.forEach { (plant, indexes) ->
                val sum = indexes.sumOf { spawns[it] }
                val range = partialRangeByPlant[plant]
                partialRangeByPlant[plant] = if (range == null) sum..sum else minOf(range.first, sum)..maxOf(range.last, sum)
            }
        }
    }
}
