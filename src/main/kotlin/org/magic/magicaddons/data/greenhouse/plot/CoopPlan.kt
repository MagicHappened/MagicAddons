package org.magic.magicaddons.data.greenhouse.plot

import java.time.Duration
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.transfer.MagicAddonsFormat
import org.magic.magicaddons.data.greenhouse.transfer.toShareableName

enum class PlanOwner(val storedName: String) {
    Coop("coop"),
    Mine("mine");

    companion object {
        fun ofStoredName(storedName: String?): PlanOwner = entries.firstOrNull { it.storedName == storedName } ?: Mine
    }
}

enum class StepChange(val storedName: String) {
    Scan("scan"),
    Back("back"),
    Pick("pick");

    companion object {
        fun ofStoredName(storedName: String?): StepChange? = entries.firstOrNull { it.storedName == storedName }
    }
}

class CoopPlan(
    val code: String,
    val plotIndex: Int,
    val name: String,
    val stepLabels: List<String>,
    val sharedBy: String?,
    sharedAt: Long,
    val version: Int,
    val preset: GreenhouseLayout,
    val progress: GreenhouseGrid.PlanProgress,
    var stepChangedBy: String?,
    var stepChange: StepChange?
) {
    var sharedAt: Long = sharedAt

    val plot: PlotLayout get() = preset.plots[plotIndex]

    val stepIndex: Int get() = progress.assignedLayout?.let { plot.stepIndexOf(it) } ?: 0

    val stepCount: Int get() = plot.steps.size

    fun stepTitle(index: Int): String = plot.steps[index].title(index)

    var coopHarvested: Map<String, Int>? = null

    var ownHarvestedInCoopTotals: Map<String, Int>? = emptyMap()

    var unreportedStepChange: StepChange? = null

    fun harvestedByCoop(cropName: String): Int {
        val ownCount = progress.harvestedThisStep[cropName] ?: 0
        val coopTotal = coopHarvested?.get(cropName) ?: return ownCount
        val ownCountInCoopTotal = ownHarvestedInCoopTotals ?: return maxOf(ownCount, coopTotal)

        return maxOf(ownCount, coopTotal - (ownCountInCoopTotal[cropName] ?: 0) + ownCount)
    }

    fun forgetCoopHarvests() {
        coopHarvested = null
        ownHarvestedInCoopTotals = emptyMap()
    }

    fun isCurrentStepKeptBy(newPlot: PlotLayout, newStepIndex: Int): Boolean {
        val newStep = newPlot.steps.getOrNull(newStepIndex) ?: return false
        return newStepIndex == stepIndex && hasSameExits(plot.steps[stepIndex].exits, newStep.exits)
    }

    fun takeHarvestsFrom(replaced: CoopPlan) {
        progress.harvestedThisStep.putAll(replaced.progress.harvestedThisStep)
        coopHarvested = replaced.coopHarvested
        ownHarvestedInCoopTotals = replaced.ownHarvestedInCoopTotals
    }

    fun hasSameStepsAs(other: PlotLayout): Boolean {
        val coopSteps = plot.steps
        val otherSteps = other.steps
        if (coopSteps.size != otherSteps.size || (plot.flow?.loop == true) != (other.flow?.loop == true)) return false

        return coopSteps.zip(otherSteps).all { (coopStep, otherStep) ->
            hasSameCells(coopStep.layout, otherStep.layout) &&
                    hasSamePolicies(coopStep.policies, otherStep.policies) &&
                    hasSameExits(coopStep.exits, otherStep.exits)
        }
    }

    private fun hasSameCells(coopLayout: PlotLayout, otherLayout: PlotLayout): Boolean {
        if (coopLayout.plants.size != otherLayout.plants.size) return false
        val hasUnmatchedPlant = coopLayout.plants.any { coopPlant ->
            otherLayout.plants.none { it.cropDef == coopPlant.cropDef && it.placementEquals(coopPlant) }
        }
        if (hasUnmatchedPlant) return false

        return coopLayout.slots.all { coopSlot ->
            val otherSlot = otherLayout.getSlot(coopSlot.x, coopSlot.y) ?: return@all false
            otherSlot.mark == coopSlot.mark && soilUnder(coopLayout, coopSlot) == soilUnder(otherLayout, otherSlot)
        }
    }

    private fun soilUnder(layout: PlotLayout, slot: LayoutSlot) =
        slot.soil ?: layout.plantCovering(slot)?.cropDef?.requiredSoil?.firstOrNull()

    private fun hasSamePolicies(coopPolicies: StepPolicies, otherPolicies: StepPolicies): Boolean =
        coopPolicies.fullClear == otherPolicies.fullClear &&
                coopPolicies.keepIdentical == otherPolicies.keepIdentical &&
                coopPolicies.inputSpawns == otherPolicies.inputSpawns

    private fun hasSameExits(coopExits: List<StepExit>, otherExits: List<StepExit>): Boolean =
        coopExits.size == otherExits.size && coopExits.zip(otherExits).all { (coopExit, otherExit) ->
            coopExit.conditions.map(::conditionKeyOf) == otherExit.conditions.map(::conditionKeyOf)
        }

    private fun conditionKeyOf(condition: ExitCondition): Triple<String, String, Int> = when (condition) {
        is ExitCondition.MutationSpawned -> Triple(KIND_SPAWNED, condition.crop.name, condition.count)
        is ExitCondition.HighestStageAtLeast -> Triple(KIND_STAGE, condition.crop.name, condition.stage)
        is ExitCondition.MutationHarvested -> Triple(KIND_HARVESTED, condition.crop.name, condition.count)
    }

    companion object {
        const val MAX_CODE_LENGTH: Int = 16384
        const val MAX_NAME_LENGTH: Int = 64
        const val MAX_HARVEST_COUNT: Int = 9999
        const val MAX_COOP_PLANS: Int = 3

        private const val LAYOUT_ID_PREFIX: String = "coop_"

        private const val UNLINKED_MEMBER_NAME: String = "a coop member"

        private const val KIND_SPAWNED: String = "spawned"
        private const val KIND_STAGE: String = "stage"
        private const val KIND_HARVESTED: String = "harvested"

        private val PLAYER_NAME_PATTERN: Regex = Regex("[A-Za-z0-9_]{1,16}")

        private val ALLOWED_SERVER_CLOCK_LEAD: Duration = Duration.ofMinutes(10)

        fun latestAcceptedTime(): Long = System.currentTimeMillis() + ALLOWED_SERVER_CLOCK_LEAD.toMillis()

        fun isPlayerName(text: String?): Boolean = text != null && PLAYER_NAME_PATTERN.matches(text)

        fun memberNameInSentence(name: String?): String = name ?: UNLINKED_MEMBER_NAME

        fun memberNameOpeningSentence(name: String?): String = name ?: UNLINKED_MEMBER_NAME.replaceFirstChar(Char::uppercaseChar)

        fun decodedOrNull(
            plotId: String,
            code: String,
            plotIndex: Int,
            name: String,
            stepLabels: List<String>,
            sharedBy: String?,
            sharedAt: Long,
            version: Int,
            stepNumber: Int,
            stepStartedAt: Long,
            stepChangedBy: String?,
            stepChange: StepChange?,
            fallbackStepIndexOf: ((PlotLayout) -> Int)?
        ): CoopPlan? {
            if (code.length > MAX_CODE_LENGTH || (sharedBy != null && !isPlayerName(sharedBy)) || version < 0 || sharedAt <= 0 || stepStartedAt <= 0) return null
            val layoutId = LAYOUT_ID_PREFIX + plotId.removePrefix(PlotLayout.PLOT_PREFIX)
            val plots = MagicAddonsFormat.importCoopPlanPlots(code, layoutId) ?: return null
            val plot = plots.getOrNull(plotIndex) ?: return null

            val now = System.currentTimeMillis()
            val latestAcceptedTime = latestAcceptedTime()
            val isStepWithinPlan = stepNumber - 1 in plot.steps.indices
            val stepIndex = if (isStepWithinPlan) stepNumber - 1 else fallbackStepIndexOf?.invoke(plot) ?: return null
            val shownName = name.toShareableName(MAX_NAME_LENGTH).ifEmpty { plot.displayName() }
            val progress = GreenhouseGrid.PlanProgress().apply {
                assignedLayout = plot.steps[stepIndex].layout
                this.stepStartedAt = if (isStepWithinPlan) stepStartedAt.coerceAtMost(latestAcceptedTime) else now
            }

            return CoopPlan(
                code = code,
                plotIndex = plotIndex,
                name = shownName,
                stepLabels = stepLabels.take(MAX_FLOW_STEPS).map { it.toShareableName(MAX_STEP_LABEL_LENGTH) },
                sharedBy = sharedBy,
                sharedAt = sharedAt.coerceAtMost(latestAcceptedTime),
                version = version,
                preset = GreenhouseLayout(id = layoutId, name = shownName, plots = plots.toMutableList()),
                progress = progress,
                stepChangedBy = stepChangedBy?.takeIf(::isPlayerName),
                stepChange = stepChange
            )
        }

        fun harvestCountsOf(counts: Map<String, Int>): Map<String, Int> = counts
            .filterKeys { cropName -> CropRegistry.allCrops.any { it.name == cropName } }
            .mapValues { (_, count) -> count.coerceIn(0, MAX_HARVEST_COUNT) }
            .filterValues { it > 0 }
    }
}
