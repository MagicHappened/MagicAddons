package org.magic.magicaddons.data.greenhouse.transfer

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.plot.ExitCondition
import org.magic.magicaddons.data.greenhouse.plot.FlowStep
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.InputSpawns
import org.magic.magicaddons.data.greenhouse.plot.PlotFlow
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.StepExit
import org.magic.magicaddons.data.greenhouse.plot.StepPolicies

object SkyShardsFlowsFormat : LayoutFormat {

    override val displayName: String = "SkyShards flows"

    override val importsAsNewPreset: Boolean = true

    override val carriesSteps: Boolean = true

    private const val KIND: String = "skyshards-greenhouse-flows"
    private const val VERSION: Int = 2

    private const val MUTATION_SPAWNED: String = "mutationSpawned"
    private const val HIGHEST_STAGE_AT_LEAST: String = "highestStageAtLeast"
    private const val MUTATION_HARVESTED: String = "mutationHarvested"
    private const val HARVEST_INPUT_SPAWNS: String = "harvest"
    private const val KEEP_INPUT_SPAWNS: String = "keep"

    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun canImport(text: String): Boolean = parse(text)?.get("kind")?.asStringOrNull() == KIND

    private class FilePlot(val number: Int, val plot: PlotLayout)

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val root = parse(text)?.takeIf { it.get("kind")?.asStringOrNull() == KIND }
            ?: return LayoutTransferResult.Failure("Your clipboard does not hold a SkyShards flows file.")

        val keepIdentical = root.objectAt("settings")?.objectAt("config")?.get("keepIdenticalOnStepChange")?.asBooleanOrNull() ?: true
        val filePlots = root.arrayAt("plots")?.mapIndexedNotNull { index, element ->
            val plotJson = element as? JsonObject ?: return@mapIndexedNotNull null
            val number = plotJson.get("id")?.asIntOrNull() ?: (index + 1)
            readPlot(plotJson, GreenhouseLayout.plotId(layoutId, index), keepIdentical)?.let { FilePlot(number, it) }
        }.orEmpty()
        if (filePlots.isEmpty()) return LayoutTransferResult.Failure("That SkyShards flows file has no plot with a readable layout.")

        val merged = mutableListOf<MutableList<FilePlot>>()
        filePlots.forEach { filePlot ->
            val twin = merged.firstOrNull { group -> isSamePlot(group.first().plot, filePlot.plot) }
            if (twin != null) twin.add(filePlot) else merged.add(mutableListOf(filePlot))
        }
        val plots = merged.take(GreenhouseLayout.MAX_PLOTS).mapIndexed { index, group ->
            group.first().plot.copyWithSteps(GreenhouseLayout.plotId(layoutId, index), nameOf(group.map { it.number }))
        }
        return LayoutTransferResult.Imported(plots.first(), plots.drop(1))
    }

    private fun nameOf(numbers: List<Int>): String = when (numbers.size) {
        1 -> "Plot ${numbers.first()}"
        else -> "Plots ${numbers.dropLast(1).joinToString(", ")} and ${numbers.last()}"
    }

    private fun isSamePlot(left: PlotLayout, right: PlotLayout): Boolean {
        val leftSteps = left.steps
        val rightSteps = right.steps
        if (leftSteps.size != rightSteps.size || (left.flow?.loop ?: false) != (right.flow?.loop ?: false)) return false

        return leftSteps.zip(rightSteps).all { (a, b) ->
            isSameLayout(a.layout, b.layout) && exitsText(a) == exitsText(b) && policiesText(a) == policiesText(b)
        }
    }

    private fun isSameLayout(left: PlotLayout, right: PlotLayout): Boolean {
        fun plantsOf(layout: PlotLayout) = layout.plants.map { listOf(it.elementId, it.slot.x, it.slot.y, it.slot.mark, it.presetAlternatives.map { alt -> alt.name }) }.toSet()
        if (plantsOf(left) != plantsOf(right)) return false

        return left.slots.all { slot -> right.getSlot(slot.x, slot.y)?.soil == slot.soil }
    }

    private fun exitsText(step: FlowStep): String = step.exits.joinToString("|") { exit -> exit.conditions.joinToString("&") { it.describe() } }

    private fun policiesText(step: FlowStep): String = "${step.policies.fullClear}${step.policies.keepIdentical}${step.policies.inputSpawns}"

    private fun readPlot(plotJson: JsonObject, plotId: String, keepIdentical: Boolean): PlotLayout? {
        val flowJson = plotJson.objectAt("flow") ?: return null
        val stepsJson = flowJson.arrayAt("steps")?.mapNotNull { it as? JsonObject }.orEmpty()
        if (stepsJson.isEmpty()) return null

        var plot: PlotLayout? = null
        val steps = mutableListOf<FlowStep>()
        stepsJson.forEach { stepJson ->
            val code = stepJson.objectAt("layout")?.get("code")?.asStringOrNull()
            val layoutId = PlotLayout.stepId(plotId, steps.size)
            val layout = code?.let { SkyShardsFormat.import(it, layoutId) } as? LayoutTransferResult.Imported ?: return@forEach
            val label = stepJson.get("label")?.asStringOrNull()?.takeIf { it.isNotBlank() }
            val stepLayout = layout.layout.also { it.name = label }
            if (plot == null) plot = stepLayout

            val exits = stepJson.arrayAt("exits")?.mapNotNull { exitElement ->
                val conditions = (exitElement as? JsonObject)?.arrayAt("when")?.mapNotNull { readCondition(it) }.orEmpty()
                if (conditions.isEmpty()) null else StepExit(conditions)
            }.orEmpty()
            val policies = StepPolicies(
                fullClear = stepJson.get("fullClear")?.asBooleanOrNull() ?: false,
                keepIdentical = keepIdentical,
                inputSpawns = if (stepJson.objectAt("policies")?.get("layoutInputSpawns")?.asStringOrNull() == HARVEST_INPUT_SPAWNS) InputSpawns.Harvest else InputSpawns.Keep
            )
            steps.add(FlowStep(label, stepLayout, exits, policies))
        }
        val first = plot ?: return null
        first.name = null
        if (steps.size > 1) first.flow = PlotFlow(flowJson.get("loop")?.asBooleanOrNull() ?: false, steps)
        return first
    }

    private fun readCondition(element: JsonElement): ExitCondition? {
        val json = element as? JsonObject ?: return null
        val kind = json.get("kind")?.asStringOrNull() ?: return null
        val cropId = json.get("mutationId")?.asStringOrNull() ?: return null
        val crop = CropRegistry.findByLooseName(cropId) ?: return null
        return when (kind) {
            MUTATION_SPAWNED -> ExitCondition.MutationSpawned(crop, json.get("count")?.asIntOrNull() ?: 1)
            HIGHEST_STAGE_AT_LEAST -> ExitCondition.HighestStageAtLeast(crop, json.get("stage")?.asIntOrNull() ?: crop.maxStage)
            MUTATION_HARVESTED -> ExitCondition.MutationHarvested(crop, json.get("count")?.asIntOrNull() ?: 1)
            else -> null
        }
    }

    override fun export(layout: PlotLayout): LayoutTransferResult = writeFile(listOf(layout))

    override fun exportAll(master: GreenhouseLayout): LayoutTransferResult = writeFile(master.plots)

    private fun writeFile(plots: List<PlotLayout>): LayoutTransferResult {
        val root = JsonObject()
        root.addProperty("kind", KIND)
        root.addProperty("version", VERSION)
        val plotsJson = JsonArray()
        plots.forEachIndexed { index, plot ->
            val stepsJson = JsonArray()
            plot.steps.forEachIndexed { stepIndex, step ->
                val code = (SkyShardsFormat.export(step.layout) as? LayoutTransferResult.Exported)?.text?.substringAfter("layout=")
                    ?: return LayoutTransferResult.Failure("Could not write ${step.title(stepIndex)} of ${plot.displayName()} as a SkyShards layout.")
                stepsJson.add(stepJson(step, stepIndex, code))
            }
            val flowJson = JsonObject()
            flowJson.add("steps", stepsJson)
            flowJson.addProperty("loop", plot.flow?.loop ?: false)
            flowJson.addProperty("startIndex", 0)
            val plotJson = JsonObject()
            plotJson.addProperty("id", index + 1)
            plotJson.add("flow", flowJson)
            plotsJson.add(plotJson)
        }
        root.add("plots", plotsJson)
        val config = JsonObject()
        config.addProperty("keepIdenticalOnStepChange", plots.all { plot -> plot.steps.all { step -> step.policies.keepIdentical } })
        val settings = JsonObject()
        settings.add("config", config)
        root.add("settings", settings)
        return LayoutTransferResult.Exported(gson.toJson(root))
    }

    private fun stepJson(step: FlowStep, stepIndex: Int, code: String): JsonObject {
        val stepJson = JsonObject()
        stepJson.addProperty("id", "step-${stepIndex + 1}")
        stepJson.addProperty("label", step.title(stepIndex))
        val layoutJson = JsonObject()
        layoutJson.addProperty("code", code)
        stepJson.add("layout", layoutJson)
        val exitsJson = JsonArray()
        step.exits.forEach { exit ->
            val whenJson = JsonArray()
            exit.conditions.forEach { condition -> writeCondition(condition)?.let(whenJson::add) }
            val exitJson = JsonObject()
            exitJson.add("when", whenJson)
            exitsJson.add(exitJson)
        }
        stepJson.add("exits", exitsJson)
        if (step.policies.fullClear) stepJson.addProperty("fullClear", true)
        val policiesJson = JsonObject()
        policiesJson.addProperty("layoutInputSpawns", if (step.policies.inputSpawns == InputSpawns.Harvest) HARVEST_INPUT_SPAWNS else KEEP_INPUT_SPAWNS)
        stepJson.add("policies", policiesJson)
        return stepJson
    }

    private fun writeCondition(condition: ExitCondition): JsonObject? {
        val mutationId = SkyShardsFormat.siteIdOf(condition.crop) ?: return null
        val json = JsonObject()
        json.addProperty("mutationId", mutationId)
        when (condition) {
            is ExitCondition.MutationSpawned -> {
                json.addProperty("kind", MUTATION_SPAWNED)
                json.addProperty("count", condition.count)
            }
            is ExitCondition.HighestStageAtLeast -> {
                json.addProperty("kind", HIGHEST_STAGE_AT_LEAST)
                json.addProperty("stage", condition.stage)
            }
            is ExitCondition.MutationHarvested -> {
                json.addProperty("kind", MUTATION_HARVESTED)
                json.addProperty("count", condition.count)
            }
        }
        return json
    }

    private fun parse(text: String): JsonObject? = runCatching { JsonParser.parseString(text.trim()).asJsonObject }.getOrNull()

    private fun JsonObject.objectAt(name: String): JsonObject? = get(name) as? JsonObject

    private fun JsonObject.arrayAt(name: String): JsonArray? = get(name) as? JsonArray

    private fun JsonElement.asStringOrNull(): String? = runCatching { asString }.getOrNull()

    private fun JsonElement.asIntOrNull(): Int? = runCatching { asInt }.getOrNull()

    private fun JsonElement.asBooleanOrNull(): Boolean? = runCatching { asBoolean }.getOrNull()
}
