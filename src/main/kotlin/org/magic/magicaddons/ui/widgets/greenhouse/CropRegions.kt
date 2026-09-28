package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.crops.Footprint
import kotlin.math.abs

object CropRegions {

    fun overlappingRegions(corners: List<Pair<Int, Int>>, footprint: Footprint): List<List<Pair<Int, Int>>> =
        regionsOf(corners) { one, other ->
            one.first < other.first + footprint.width && other.first < one.first + footprint.width &&
                    one.second < other.second + footprint.height && other.second < one.second + footprint.height
        }

    fun touchingRegions(corners: List<Pair<Int, Int>>, footprint: Footprint): List<List<Pair<Int, Int>>> =
        regionsOf(corners) { one, other ->
            cellsTouch(claimedCells(listOf(one), footprint), claimedCells(listOf(other), footprint))
        }

    fun claimedCells(corners: List<Pair<Int, Int>>, footprint: Footprint): Set<Pair<Int, Int>> =
        corners.flatMapTo(mutableSetOf()) { (cornerX, cornerY) -> footprint.cellsFrom(cornerX, cornerY) }

    fun freeCellIn(cells: Set<Pair<Int, Int>>, cellsWithIcon: Set<Pair<Int, Int>>): Pair<Int, Int> {
        val midX = cells.sumOf { it.first }.toDouble() / cells.size
        val midY = cells.sumOf { it.second }.toDouble() / cells.size
        val byDistanceFromMiddle = cells.sortedBy { abs(it.first - midX) + abs(it.second - midY) }

        byDistanceFromMiddle.firstOrNull { it !in cellsWithIcon }?.let { return it }

        return byDistanceFromMiddle.maxBy { cell ->
            cellsWithIcon.minOf { abs(cell.first - it.first) + abs(cell.second - it.second) }
        }
    }

    private fun regionsOf(
        corners: List<Pair<Int, Int>>,
        belongTogether: (Pair<Int, Int>, Pair<Int, Int>) -> Boolean
    ): List<List<Pair<Int, Int>>> {
        val regions = mutableListOf<MutableList<Pair<Int, Int>>>()

        corners.forEach { corner ->
            val joined = regions.filter { region -> region.any { belongTogether(it, corner) } }
            val region = joined.firstOrNull() ?: mutableListOf<Pair<Int, Int>>().also { regions += it }

            joined.drop(1).forEach { absorbed ->
                region += absorbed
                regions.removeAll { it === absorbed }
            }
            region += corner
        }

        return regions
    }

    private fun cellsTouch(one: Set<Pair<Int, Int>>, other: Set<Pair<Int, Int>>): Boolean =
        one.any { (cellX, cellY) ->
            (cellX to cellY) in other || (cellX + 1 to cellY) in other || (cellX - 1 to cellY) in other ||
                    (cellX to cellY + 1) in other || (cellX to cellY - 1) in other
        }
}
