package org.magic.magicaddons.features.farming.greenhousePresets.flows

import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.Plant
import net.minecraft.client.Minecraft
import org.magic.magicaddons.data.greenhouse.plot.CoopPlan
import org.magic.magicaddons.data.greenhouse.plot.ExitCondition
import org.magic.magicaddons.data.greenhouse.plot.FlowStep
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.InputSpawns
import org.magic.magicaddons.data.greenhouse.plot.PlanOwner
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.StepChange
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

    fun runningOn(grid: GreenhouseGrid): Running? = runningOf(grid.state.assignedLayout)

    private fun runningOf(layout: PlotLayout?): Running? {
        layout ?: return null
        val preset = GreenhouseData.greenhouseLayoutFor(layout) ?: return null
        val plot = preset.plotOf(layout) ?: return null
        if (!plot.hasSteps) return null
        val stepIndex = plot.stepIndexOf(layout) ?: return null

        return Running(preset, plot, stepIndex)
    }

    fun assign(grid: GreenhouseGrid, plot: PlotLayout, stepIndex: Int, turns: Int, noRotate: Boolean) {
        val ownPlan = grid.state.ownPlan
        val keepsStep = runningOf(ownPlan.assignedLayout)?.plot === plot
        if (!keepsStep) {
            val step = plot.steps.getOrElse(stepIndex) { plot.steps.first() }
            ownPlan.assignedLayout = step.layout
            ownPlan.previousStep = null
            startStep(grid, ownPlan, step, System.currentTimeMillis())
        }
        ownPlan.noRotateAssignedLayout = noRotate
        ownPlan.planTurns = turns
        ownPlan.buildAnnounced = false
        PlannerNeeds.forgetSentLines(grid)
        GreenhouseData.noteUnsavedChange()
        GreenhouseData.regenRender()
        GreenhouseData.calculateSpawnForecasts(grid)
    }

    fun switchRunningPlan(grid: GreenhouseGrid, owner: PlanOwner) {
        if (grid.state.runs == owner) return
        grid.state.runs = owner
        grid.state.buildAnnounced = false
        PlannerNeeds.forgetSentLines(grid)
        GreenhouseData.noteUnsavedChange()
        GreenhouseData.regenRender()
        GreenhouseData.calculateSpawnForecasts(grid)
    }

    fun startCoopPlan(grid: GreenhouseGrid, plan: CoopPlan) {
        val progress = plan.progress
        val layout = progress.assignedLayout ?: return
        progress.planTurns = grid.bestRotationFor(layout)
        startStep(grid, progress, plan.plot.steps[plan.stepIndex], progress.stepStartedAt ?: System.currentTimeMillis())
    }

    fun moveCoopPlanToStep(grid: GreenhouseGrid, plan: CoopPlan, stepIndex: Int, startedAt: Long) {
        val progress = plan.progress
        val leftLayout = progress.assignedLayout
        progress.previousStep = leftLayout?.let { layout ->
            GreenhouseGrid.GridState.StepSnapshot(layout.id, progress.stepStartedAt, progress.harvestedThisStep.toMap()).also { it.layout = layout }
        }
        val step = plan.plot.steps[stepIndex]
        progress.assignedLayout = step.layout
        startStep(grid, progress, step, startedAt)
        progress.buildAnnounced = false
        plan.forgetCoopHarvests()
        GreenhouseData.noteUnsavedChange()
        if (grid.state.coopPlan !== plan || !grid.state.isFollowingCoopPlan) return
        PlannerNeeds.forgetSentLines(grid)
        GreenhouseData.regenRender()
        GreenhouseData.calculateSpawnForecasts(grid)
    }

    fun clearState(progress: GreenhouseGrid.PlanProgress) {
        progress.stepStartedAt = null
        progress.harvestedThisStep.clear()
        progress.previousStep = null
        progress.clearMarked.clear()
    }

    private fun startStep(grid: GreenhouseGrid, progress: GreenhouseGrid.PlanProgress, step: FlowStep, startedAt: Long) {
        progress.stepStartedAt = startedAt
        progress.harvestedThisStep.clear()
        progress.clearMarked.clear()
        val clearsEverything = step.policies.fullClear || !step.policies.keepIdentical
        val plan = progress.assignedLayout?.turnedBy(progress.planTurns)
        grid.layout.plants.filter { clearsEverything || !isWantedBy(plan, it) }.mapTo(progress.clearMarked) { clearKeyOf(it) }
    }

    private fun isWantedBy(plan: PlotLayout?, plant: Plant): Boolean {
        val planned = plan?.getSlot(plant.slot.x, plant.slot.y)?.let { plan.plantCovering(it) } ?: return false
        return planned.slot.x == plant.slot.x && planned.slot.y == plant.slot.y && planned.acceptsCrop(plant.cropDef)
    }

    private fun followedCoopPlanOf(grid: GreenhouseGrid): CoopPlan? = grid.state.coopPlan?.takeIf { grid.state.isFollowingCoopPlan }

    private fun noteCoopStepChange(grid: GreenhouseGrid, change: StepChange) {
        val plan = followedCoopPlanOf(grid) ?: return
        plan.unreportedStepChange = change
        plan.stepChangedBy = Minecraft.getInstance().user.name
        plan.stepChange = change
        plan.forgetCoopHarvests()
    }

    fun onScanSettled(grid: GreenhouseGrid) {
        val state = grid.state
        val standing = grid.layout.plants.mapTo(HashSet()) { clearKeyOf(it) }
        listOfNotNull(state.ownPlan, state.coopPlan?.progress)
            .filter { it.clearMarked.isNotEmpty() }
            .forEach { it.clearMarked.retainAll(standing) }
        val running = runningOn(grid) ?: return
        val next = running.nextIndex ?: return
        if (running.step.exits.none { exit -> exit.conditions.all { holds(grid, running, it) } }) return

        advance(grid, running, next)
        noteCoopStepChange(grid, StepChange.Scan)
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
        startStep(grid, state.runningPlan, running.plot.steps[toIndex], System.currentTimeMillis())
        state.buildAnnounced = false
        PlannerNeeds.forgetSentLines(grid)
        GreenhouseData.noteUnsavedChange()
        GreenhouseData.regenRender()
    }

    fun stepBack(grid: GreenhouseGrid): Boolean {
        val state = grid.state
        val previous = state.previousStep ?: return false
        val layout = previous.layout ?: return false
        if (state.isFollowingCoopPlan) return stepCoopPlanBack(grid, layout)
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
        GreenhouseData.calculateSpawnForecasts(grid)
        runningOn(grid)?.let { ChatUtils.sendWithPrefix("${grid.layout.displayName()} moved back to ${it.title(it.stepIndex)}") }
        return true
    }

    private fun stepCoopPlanBack(grid: GreenhouseGrid, layout: PlotLayout): Boolean {
        val running = runningOn(grid) ?: return false
        val backIndex = running.plot.stepIndexOf(layout) ?: return false
        advance(grid, running, backIndex)
        grid.state.previousStep = null
        noteCoopStepChange(grid, StepChange.Back)
        GreenhouseData.calculateSpawnForecasts(grid)
        ChatUtils.sendWithPrefix("${grid.layout.displayName()} moved back to ${running.title(backIndex)}")
        return true
    }

    fun moveTo(grid: GreenhouseGrid, stepIndex: Int) {
        val running = runningOn(grid) ?: return
        if (stepIndex == running.stepIndex || stepIndex !in running.plot.steps.indices) return
        advance(grid, running, stepIndex)
        noteCoopStepChange(grid, StepChange.Pick)
        GreenhouseData.calculateSpawnForecasts(grid)
        ChatUtils.sendWithPrefix("${grid.layout.displayName()} moved to ${running.title(stepIndex)}")
    }

    fun noteHarvested(grid: GreenhouseGrid, crop: CropDefinition) {
        if (runningOn(grid) == null) return
        grid.state.harvestedThisStep.merge(crop.name, 1, Int::plus)
    }

    fun isMarkedForClear(grid: GreenhouseGrid, plant: Plant): Boolean = clearKeyOf(plant) in grid.state.clearMarked

    fun cropsHarvestedToExit(grid: GreenhouseGrid): Set<CropDefinition> =
        runningOn(grid)?.step?.exits.orEmpty()
            .flatMap { it.conditions }
            .filterIsInstance<ExitCondition.MutationHarvested>()
            .mapTo(HashSet()) { it.crop }

    fun cropsSpawnedToExit(grid: GreenhouseGrid): Set<CropDefinition> =
        runningOn(grid)?.step?.exits.orEmpty()
            .flatMap { it.conditions }
            .filterIsInstance<ExitCondition.MutationSpawned>()
            .mapTo(LinkedHashSet()) { it.crop }

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

    private fun harvestedCount(grid: GreenhouseGrid, crop: CropDefinition): Int =
        followedCoopPlanOf(grid)?.harvestedByCoop(crop.name) ?: grid.state.harvestedThisStep[crop.name] ?: 0

    fun needLine(grid: GreenhouseGrid, running: Running): String? {
        val exit = running.step.exits.firstOrNull() ?: return null
        val needs = exit.conditions.joinToString(" and ") { it.describeNeed(progressOf(grid, it)) }
        val next = running.nextIndex ?: return "Need $needs to finish"
        val reach = if (running.isLoopingBack) "to loop back to" else "to reach"

        return "Need $needs $reach ${running.title(next)}"
    }
}
