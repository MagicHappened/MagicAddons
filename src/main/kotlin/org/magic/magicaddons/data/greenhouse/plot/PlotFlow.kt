package org.magic.magicaddons.data.greenhouse.plot

import org.magic.magicaddons.data.greenhouse.crops.CropDefinition

class PlotFlow(var loop: Boolean, val steps: MutableList<FlowStep>)

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

    abstract fun describe(): String

    abstract fun describeNeed(progress: Int?): String

    class MutationSpawned(override val crop: CropDefinition, val count: Int) : ExitCondition() {
        override fun describe(): String = "$count ${crop.name} spawned"
        override fun describeNeed(progress: Int?): String = "$count ${crop.name} spawned" + progressText(progress)
    }

    class HighestStageAtLeast(override val crop: CropDefinition, val stage: Int) : ExitCondition() {
        override fun describe(): String = "${crop.name} at stage $stage"
        override fun describeNeed(progress: Int?): String = "${crop.name} at stage $stage" + (progress?.let { " ($it now)" } ?: "")
    }

    class MutationHarvested(override val crop: CropDefinition, val count: Int) : ExitCondition() {
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
