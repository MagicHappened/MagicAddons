package org.magic.magicaddons.data.greenhouse.plot

import org.magic.magicaddons.data.greenhouse.crops.CropDefinition

const val MAX_FLOW_STEPS: Int = 10
const val MAX_EXITS_PER_STEP: Int = 8
const val MAX_CONDITIONS_PER_EXIT: Int = 8
const val MAX_CONDITION_COUNT: Int = 999
const val MAX_STEP_LABEL_LENGTH: Int = 48

class PlotFlow(var loop: Boolean, val steps: MutableList<FlowStep>) {

    val isWithinSharedBounds: Boolean
        get() = steps.size <= MAX_FLOW_STEPS && steps.all { step ->
            step.exits.size <= MAX_EXITS_PER_STEP && step.exits.all { exit ->
                exit.conditions.size in 1..MAX_CONDITIONS_PER_EXIT && exit.conditions.all { it.isAmountWithinBounds }
            }
        }
}

class FlowStep(var label: String?, val layout: PlotLayout, val exits: List<StepExit>, val policies: StepPolicies) {

    fun title(index: Int): String = label ?: "Step ${index + 1}"

    fun exitText(): String? = exits.firstOrNull()?.conditions?.joinToString(" and ") { it.describe() }

    fun policyText(): String? = listOfNotNull(
        "full clear".takeIf { policies.fullClear },
        "replaces identical plants".takeIf { !policies.keepIdentical },
        "harvests spawns on ingredient cells".takeIf { policies.inputSpawns == InputSpawns.Harvest }
    ).joinToString(", ").takeIf { it.isNotEmpty() }
}

class StepExit(val conditions: List<ExitCondition>)

sealed class ExitCondition {
    abstract val crop: CropDefinition

    abstract val isAmountWithinBounds: Boolean

    abstract fun describe(): String

    abstract fun describeNeed(progress: Int?): String

    class MutationSpawned(override val crop: CropDefinition, val count: Int) : ExitCondition() {
        override val isAmountWithinBounds: Boolean get() = count in 1..MAX_CONDITION_COUNT
        override fun describe(): String = "$count ${crop.name} spawned"
        override fun describeNeed(progress: Int?): String = "$count ${crop.name} spawned" + progressText(progress)
    }

    class HighestStageAtLeast(override val crop: CropDefinition, val stage: Int) : ExitCondition() {
        override val isAmountWithinBounds: Boolean get() = stage in 1..crop.maxStage
        override fun describe(): String = "${crop.name} at stage $stage"
        override fun describeNeed(progress: Int?): String = "${crop.name} at stage $stage" + (progress?.let { " ($it now)" } ?: "")
    }

    class MutationHarvested(override val crop: CropDefinition, val count: Int) : ExitCondition() {
        override val isAmountWithinBounds: Boolean get() = count in 1..MAX_CONDITION_COUNT
        override fun describe(): String = "$count ${crop.name} harvested"
        override fun describeNeed(progress: Int?): String = "$count ${crop.name} harvested" + progressText(progress)
    }

    protected fun progressText(progress: Int?): String = progress?.let { " ($it so far)" } ?: ""
}

enum class InputSpawns { Keep, Harvest }

class StepPolicies(
    val fullClear: Boolean = false,
    val keepIdentical: Boolean = true,
    val inputSpawns: InputSpawns = InputSpawns.Keep
)
