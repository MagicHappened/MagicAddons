package org.magic.magicaddons.features.farming.greenhousePresets.flows

import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.plot.ExitCondition
import org.magic.magicaddons.data.greenhouse.plot.FlowStep
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.InputSpawns
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.PlannerNeeds
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings
import org.magic.magicaddons.util.ChatUtils

object FlowRunner {

    class Running(val preset: GreenhouseLayout, val plot: PlotLayout, val stepIndex: Int) {
        val step: FlowStep get() = plot.steps[stepIndex]
        val stepCount: Int get() = plot.steps.size
        val nextIndex: Int? get() = if (stepIndex + 1 < stepCount) stepIndex + 1 else if (plot.flow?.loop == true) 0 else null
        val isLoopingBack: Boolean get() = stepIndex + 1 >= stepCount && nextIndex == 0
        fun title(index: Int): String = plot.steps[index].title(index)
    }

    fun runningOn(grid: GreenhouseGrid): Running? {
        val layout = grid.state.assignedLayout ?: return null
        val preset = GreenhouseData.greenhouseLayoutFor(layout) ?: return null
        val plot = preset.plotOf(layout) ?: return null
        if (!plot.hasSteps) return null
        val stepIndex = plot.stepIndexOf(layout) ?: return null

        return Running(preset, plot, stepIndex)
    }

    fun assign(grid: GreenhouseGrid, plot: PlotLayout, stepIndex: Int, turns: Int, noRotate: Boolean) {
        val state = grid.state
        val keepsStep = runningOn(grid)?.plot === plot
        if (!keepsStep) {
            val step = plot.steps.getOrElse(stepIndex) { plot.steps.first() }
            state.assignedLayout = step.layout
            state.previousStep = null
            startStep(grid, step)
        }
        state.noRotateAssignedLayout = noRotate
        state.planTurns = turns
        state.buildAnnounced = false
        PlannerNeeds.forgetSentLines(grid)
        GreenhouseData.noteUnsavedChange()
        GreenhouseData.regenRender()
    }

    fun clearState(grid: GreenhouseGrid) {
        val state = grid.state
        state.stepStartedAt = null
        state.harvestedThisStep.clear()
        state.previousStep = null
        state.clearMarked.clear()
    }

    private fun startStep(grid: GreenhouseGrid, step: FlowStep) {
        val state = grid.state
        state.stepStartedAt = System.currentTimeMillis()
        state.harvestedThisStep.clear()
        state.clearMarked.clear()
        val clearsEverything = step.policies.fullClear || !step.policies.keepIdentical
        grid.layout.plants.filter { clearsEverything || !isWantedByPlan(grid, it) }.mapTo(state.clearMarked) { clearKeyOf(it) }
    }

    private fun isWantedByPlan(grid: GreenhouseGrid, plant: Plant): Boolean {
        val planned = grid.plannedPlantAt(plant.slot.x, plant.slot.y) ?: return false
        return planned.slot.x == plant.slot.x && planned.slot.y == plant.slot.y && planned.acceptsCrop(plant.cropDef)
    }

    fun onScanSettled(grid: GreenhouseGrid) {
        val state = grid.state
        if (state.clearMarked.isNotEmpty()) {
            val standing = grid.layout.plants.mapTo(HashSet()) { clearKeyOf(it) }
            state.clearMarked.retainAll(standing)
        }
        val running = runningOn(grid) ?: return
        val next = running.nextIndex ?: return
        if (running.step.exits.none { exit -> exit.conditions.all { holds(grid, running, it) } }) return

        advance(grid, running, next)
        if (GreenhousePresets.warningTypeEnabled(PlantWarnings.FLOW_STEP_KEY)) {
            val where = if (next == 0 && running.isLoopingBack) "back on" else "on"
            ChatUtils.sendWithPrefix("${grid.layout.displayName()} finished ${running.title(running.stepIndex)}, now $where ${running.title(next)}")
        }
    }

    private fun advance(grid: GreenhouseGrid, running: Running, toIndex: Int) {
        val state = grid.state
        state.previousStep = GreenhouseGrid.GridState.StepSnapshot(running.step.layout.id, state.stepStartedAt, state.harvestedThisStep.toMap())
            .also { it.layout = running.step.layout }
        state.assignedLayout = running.plot.steps[toIndex].layout
        startStep(grid, running.plot.steps[toIndex])
        state.buildAnnounced = false
        PlannerNeeds.forgetSentLines(grid)
        GreenhouseData.noteUnsavedChange()
        GreenhouseData.regenRender()
    }

    fun stepBack(grid: GreenhouseGrid): Boolean {
        val state = grid.state
        val previous = state.previousStep ?: return false
        val layout = previous.layout ?: return false
        state.assignedLayout = layout
        state.stepStartedAt = previous.startedAt
        state.harvestedThisStep.clear()
        state.harvestedThisStep.putAll(previous.harvested)
        state.clearMarked.clear()
        state.previousStep = null
        state.buildAnnounced = false
        PlannerNeeds.forgetSentLines(grid)
        GreenhouseData.noteUnsavedChange()
        GreenhouseData.regenRender()
        runningOn(grid)?.let { ChatUtils.sendWithPrefix("${grid.layout.displayName()} moved back to ${it.title(it.stepIndex)}") }
        return true
    }

    fun moveTo(grid: GreenhouseGrid, stepIndex: Int) {
        val running = runningOn(grid) ?: return
        if (stepIndex == running.stepIndex || stepIndex !in running.plot.steps.indices) return
        advance(grid, running, stepIndex)
        ChatUtils.sendWithPrefix("${grid.layout.displayName()} moved to ${running.title(stepIndex)}")
    }

    fun noteHarvested(grid: GreenhouseGrid, crop: CropDefinition) {
        if (runningOn(grid) == null) return
        grid.state.harvestedThisStep.merge(crop.name, 1, Int::plus)
    }

    fun isMarkedForClear(grid: GreenhouseGrid, plant: Plant): Boolean = clearKeyOf(plant) in grid.state.clearMarked

    fun harvestsSpawnedIngredients(grid: GreenhouseGrid): Boolean = runningOn(grid)?.step?.policies?.inputSpawns == InputSpawns.Harvest

    private fun clearKeyOf(plant: Plant): String = "${plant.slot.x},${plant.slot.y},${plant.elementId}"

    private fun holds(grid: GreenhouseGrid, running: Running, condition: ExitCondition): Boolean = when (condition) {
        is ExitCondition.MutationSpawned -> spawnedCount(grid, condition.crop) >= condition.count
        is ExitCondition.HighestStageAtLeast -> (highestStageOf(grid, condition.crop) ?: 0) >= condition.stage
        is ExitCondition.MutationHarvested -> harvestedCount(grid, condition.crop) >= condition.count
    }

    fun progressOf(grid: GreenhouseGrid, condition: ExitCondition): Int? = when (condition) {
        is ExitCondition.MutationSpawned -> spawnedCount(grid, condition.crop)
        is ExitCondition.HighestStageAtLeast -> highestStageOf(grid, condition.crop)
        is ExitCondition.MutationHarvested -> harvestedCount(grid, condition.crop)
    }

    private fun spawnedCount(grid: GreenhouseGrid, crop: CropDefinition): Int {
        val startedAt = grid.state.stepStartedAt ?: 0L
        val standing = grid.layout.plants.count { plant ->
            plant.cropDef == crop && !plant.placed && (plant.appearedAt?.let { it >= startedAt } ?: true)
        }
        return standing + harvestedCount(grid, crop)
    }

    private fun highestStageOf(grid: GreenhouseGrid, crop: CropDefinition): Int? =
        grid.layout.plants.filter { it.cropDef == crop }.mapNotNull { it.highestStage }.maxOrNull()

    private fun harvestedCount(grid: GreenhouseGrid, crop: CropDefinition): Int = grid.state.harvestedThisStep[crop.name] ?: 0

    fun needLine(grid: GreenhouseGrid, running: Running): String? {
        val exit = running.step.exits.firstOrNull() ?: return null
        val needs = exit.conditions.joinToString(" and ") { it.describeNeed(progressOf(grid, it)) }
        val next = running.nextIndex ?: return "Need $needs to finish"
        val reach = if (running.isLoopingBack) "to loop back to" else "to reach"

        return "Need $needs $reach ${running.title(next)}"
    }
}
