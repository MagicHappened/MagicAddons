package org.magic.magicaddons.features.farming.greenhousePresets.warnings

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.random.Random
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.flows.FlowRunner
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.BioanalysisAccessory

object LikelySpawns {

    private const val MAX_SIMULATED_EXIT_CROPS: Int = 4

    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "MagicAddons likely spawns").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    class Calculation<T>(
        private val simulation: PlotPrediction.SpawnSimulation,
        chancesOf: PlotPrediction.SpawnSimulation.(Random, () -> Boolean) -> T?
    ) {
        val fingerprint: Int get() = simulation.fingerprint

        @Volatile
        private var isCancelled: Boolean = false

        val chances: CompletableFuture<T?> = CompletableFuture.supplyAsync({
            simulation.chancesOf(Random(simulation.fingerprint.toLong())) { isCancelled }
        }, worker)

        val isRunning: Boolean get() = !chances.isDone

        fun cancel() {
            isCancelled = true
        }
    }

    fun simulationOf(grid: GreenhouseGrid): PlotPrediction.SpawnSimulation {
        val plan = grid.assignedPlanAfterTurn()

        return PlotPrediction.spawnSimulationOf(
            grid.layout,
            BioanalysisAccessory.mutationWeightMultiplier(),
            GreenhouseTickTime.remainingTickMs(),
            GreenhouseTickTime.tickMs
        ) { crop, x, y -> countsSpawn(plan, crop, x, y) }
    }

    fun exitCropSimulationOf(grid: GreenhouseGrid): PlotPrediction.SpawnSimulation? {
        val cropsSpawnedToExit = FlowRunner.cropsSpawnedToExit(grid).take(MAX_SIMULATED_EXIT_CROPS).toSet()
        if (cropsSpawnedToExit.isEmpty()) return null

        return PlotPrediction.spawnSimulationOf(
            grid.layout,
            BioanalysisAccessory.mutationWeightMultiplier(),
            GreenhouseTickTime.remainingTickMs(),
            GreenhouseTickTime.tickMs
        ) { crop, _, _ -> crop in cropsSpawnedToExit }
    }

    private fun countsSpawn(plan: PlotLayout?, crop: CropDefinition, x: Int, y: Int): Boolean {
        if (!PlotPrediction.countsAsLikelySpawn(crop)) return false
        val planned = plan?.getSlot(x, y)?.let { plan.plantCovering(it) }
        if (planned != null && planned.slot.mark == LayoutSlot.Marking.Ingredient && planned.acceptsCrop(crop)) {
            if (!GreenhousePresets.countsIngredientsAsHarvestable()) return false
        }
        if (!GreenhousePresets.harvestHighlightOnlyTargets()) return true
        plan ?: return false

        return plan.plants.any { target ->
            target.slot.mark == LayoutSlot.Marking.Target &&
                    target.acceptsCrop(crop) &&
                    target.slot.x in x until x + crop.footprint.width &&
                    target.slot.y in y until y + crop.footprint.height
        }
    }
}
