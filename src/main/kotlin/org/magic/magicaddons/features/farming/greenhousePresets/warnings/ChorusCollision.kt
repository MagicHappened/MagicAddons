package org.magic.magicaddons.features.farming.greenhousePresets.warnings

import kotlin.math.ceil
import kotlin.math.sqrt
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.ChorusFruit
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.MagicJellybean
import org.magic.magicaddons.data.greenhouse.plot.GREENHOUSE_SIZE
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction

//todo see if warning works correctly.
object ChorusCollision {

    const val PLANNED_FOR_DEVIATIONS: Double = 2.33

    data class Report(
        val movingChorus: Int,
        val freeTiles: Int,
        val openSpawners: Int,
        val ripeningChorus: Int,
        val tilesSpare: Int,
        val tilesNeeded: Int,
        val chorusToBreak: Int,
        val growingJellybeansAtRisk: Int,
        val ticksAway: Int
    ) {
        val needsWarning: Boolean get() = chorusToBreak > 0
    }

    fun reportFor(grid: GreenhouseGrid, ticks: Int, weightMultiplier: Double): Report? =
        reportFor(grid.layout, ticks, weightMultiplier)

    fun reportFor(layout: PlotLayout, ticks: Int, weightMultiplier: Double): Report? {
        if (ticks <= 0) return null

        val chorusPlants = layout.plants.filter { it.cropDef == ChorusFruit.definition }
        if (chorusPlants.isEmpty()) return null

        val maxStage = ChorusFruit.definition.maxStage

        val movingChorus = chorusPlants.filter { (it.lowestStage ?: 1) < maxStage }

        val ripeningChorus = movingChorus.count { (it.lowestStage ?: 1) >= maxStage - ticks }

        val occupied = occupiedTiles(layout)
        val freeTiles = occupied.count { !it }

        val spawnChances = occupied.indices.filter { !occupied[it] }.mapNotNull { tile ->
            PlotPrediction.mutationChancesAtSlot(layout, tile % GREENHOUSE_SIZE, tile / GREENHOUSE_SIZE, weightMultiplier)
                .firstOrNull { it.crop == ChorusFruit.definition }
                ?.chance
        }

        val expectedBirths = spawnChances.sum() * ticks
        val birthDeviation = sqrt(spawnChances.sumOf { it * (1 - it) } * ticks)

        val tilesNeeded = 2 * ceil(expectedBirths + PLANNED_FOR_DEVIATIONS * birthDeviation).toInt()
        val tilesSpare = freeTiles - movingChorus.size
        val spareWithRipening = tilesSpare + ripeningChorus

        val chorusToBreak = if (spareWithRipening < tilesNeeded) ceil((tilesNeeded - spareWithRipening) / 2.0).toInt() else 0

        return Report(
            movingChorus = movingChorus.size,
            freeTiles = freeTiles,
            openSpawners = spawnChances.size,
            ripeningChorus = ripeningChorus,
            tilesSpare = tilesSpare,
            tilesNeeded = tilesNeeded,
            chorusToBreak = chorusToBreak,
            growingJellybeansAtRisk = layout.plants.count {
                it.cropDef == MagicJellybean.definition &&
                        it.grewInPlace &&
                        (it.lowestStage ?: 1) < it.cropDef.maxStage
            },
            ticksAway = ticks
        )
    }

    private fun occupiedTiles(layout: PlotLayout): BooleanArray {
        val occupied = BooleanArray(GREENHOUSE_SIZE * GREENHOUSE_SIZE)

        layout.plants.forEach { plant ->
            val footprint = plant.cropDef.footprint

            for (dy in 0 until footprint.height) {
                for (dx in 0 until footprint.width) {
                    val x = plant.slot.x + dx
                    val y = plant.slot.y + dy

                    if (x in 0 until GREENHOUSE_SIZE && y in 0 until GREENHOUSE_SIZE) {
                        occupied[y * GREENHOUSE_SIZE + x] = true
                    }
                }
            }
        }

        return occupied
    }
}
