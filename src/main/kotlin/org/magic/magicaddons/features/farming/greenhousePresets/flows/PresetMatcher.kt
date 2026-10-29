package org.magic.magicaddons.features.farming.greenhousePresets.flows

import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout

object PresetMatcher {

    class Pairing(val plot: PlotLayout, val grid: GreenhouseGrid, val stepIndex: Int, val turns: Int, val configuration: GreenhouseGrid.PlotConfiguration) {
        fun describe(): String = buildString {
            if (plot.hasSteps) append("step ${stepIndex + 1} · ")
            append(if (turns == 0) "no turn" else "turned ${turns * 90}°")
            append(" · ${configuration.plants} match")
            if (configuration.clashingCropSlots > 0) append(" · ${configuration.clashingCropSlots} clash")
        }
    }

    private class Candidate(val plot: PlotLayout, val grid: GreenhouseGrid, val stepIndex: Int, val turns: Int, val configuration: GreenhouseGrid.PlotConfiguration)

    fun bestPairings(preset: GreenhouseLayout, grids: List<GreenhouseGrid>, noRotate: Boolean): List<Pairing> {
        if (preset.plots.isEmpty() || grids.isEmpty()) return emptyList()

        val candidates = preset.plots.associateWith { plot ->
            grids.map { grid -> bestCandidate(plot, grid, noRotate) }
        }
        var best: List<Candidate> = emptyList()
        var bestScore = Int.MIN_VALUE
        var bestSoil = Int.MIN_VALUE

        fun search(plotIndex: Int, chosen: List<Candidate>, usedGrids: Set<GreenhouseGrid>) {
            if (plotIndex == preset.plots.size || chosen.size == grids.size) {
                val score = chosen.sumOf { it.configuration.score }
                val soil = chosen.sumOf { it.configuration.soil }
                if (score > bestScore || (score == bestScore && soil > bestSoil)) {
                    best = chosen
                    bestScore = score
                    bestSoil = soil
                }
                return
            }
            val plot = preset.plots[plotIndex]
            candidates.getValue(plot).filter { it.grid !in usedGrids }.forEach { candidate ->
                search(plotIndex + 1, chosen + candidate, usedGrids + candidate.grid)
            }
        }
        search(0, emptyList(), emptySet())

        return best.map { it.toPairing() }
    }

    fun pairingOf(plot: PlotLayout, grid: GreenhouseGrid, noRotate: Boolean): Pairing = bestCandidate(plot, grid, noRotate).toPairing()

    private fun Candidate.toPairing(): Pairing = Pairing(plot, grid, stepIndex, turns, configuration)

    private fun bestCandidate(plot: PlotLayout, grid: GreenhouseGrid, noRotate: Boolean): Candidate =
        plot.steps.mapIndexed { stepIndex, step ->
            val turns = if (noRotate) 0 else grid.bestRotationFor(step.layout)
            Candidate(plot, grid, stepIndex, turns, grid.configurationForLayout(step.layout.turnedBy(turns)))
        }.maxWith(compareBy { it.configuration })
}
