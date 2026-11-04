package org.magic.magicaddons.data.server

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.internal.FollowCoopPlanCommand
import org.magic.magicaddons.data.greenhouse.plot.CoopPlan
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.MAX_FLOW_STEPS
import org.magic.magicaddons.data.greenhouse.plot.MAX_STEP_LABEL_LENGTH
import org.magic.magicaddons.data.greenhouse.plot.PlanOwner
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.StepChange
import org.magic.magicaddons.data.greenhouse.transfer.LayoutTransferResult
import org.magic.magicaddons.data.greenhouse.transfer.MagicAddonsFormat
import org.magic.magicaddons.data.greenhouse.transfer.toShareableName
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.PlannerNeeds
import org.magic.magicaddons.features.farming.greenhousePresets.flows.FlowRunner
import org.magic.magicaddons.features.farming.greenhousePresets.flows.PresetMatcher
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseProfiles
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.SBLocation
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

object CoopPlanSync {

    class SentCoopSteps(val body: JsonObject, val harvestedByPlotId: Map<String, Map<String, Int>>, val stepChangeByPlotId: Map<String, StepChange?>)

    private class PulledPlan(
        val version: Int,
        val code: String?,
        val plotIndex: Int,
        val name: String,
        val stepLabels: List<String>,
        val sharedBy: String?,
        val sharedAt: Long,
        val step: Int,
        val stepStartedAt: Long,
        val stepChangedBy: String?,
        val stepChange: StepChange?,
        val harvested: Map<String, Int>
    )

    private const val PLAN_PATH: String = "/coop/plan"

    private const val HTTP_POST: String = "POST"
    private const val HTTP_DELETE: String = "DELETE"
    private const val HTTP_BAD_REQUEST: Int = 400

    private const val FOLLOW_LABEL: String = "[Follow]"

    private const val MAX_HARVEST_CROPS_SENT: Int = 16

    private val PLOT_ID_PATTERN: Regex = Regex("plot_\\d{1,3}")

    private var arePlansPulledThisSession: Boolean = false

    private val goneGreenhouseAttempts: MutableSet<String> = mutableSetOf()

    private val profileId: String?
        get() = GreenhouseProfiles.activeProfileId?.toString()

    private val ownName: String
        get() = Minecraft.getInstance().user.name

    fun resetForProfile() {
        arePlansPulledThisSession = false
        goneGreenhouseAttempts.clear()
    }

    fun coopStepsForHeartbeat(): SentCoopSteps? {
        val followingGrids = GreenhouseData.greenhouseGrids.filter { it.state.isFollowingCoopPlan }.take(CoopPlan.MAX_COOP_PLANS)
        if (followingGrids.isEmpty()) return null

        val body = JsonObject()
        val harvestedByPlotId = mutableMapOf<String, Map<String, Int>>()
        val stepChangeByPlotId = mutableMapOf<String, StepChange?>()
        followingGrids.forEach { grid ->
            val plan = grid.state.coopPlan ?: return@forEach
            val harvested = CoopPlan.harvestCountsOf(plan.progress.harvestedThisStep).entries.take(MAX_HARVEST_CROPS_SENT).associate { it.key to it.value }
            body.add(grid.layout.id, JsonObject().apply {
                addProperty("planVersion", plan.version)
                addProperty("step", plan.stepIndex + 1)
                addProperty("stepStartedAt", plan.progress.stepStartedAt ?: plan.sharedAt)
                add("harvested", JsonObject().apply { harvested.forEach { (cropName, count) -> addProperty(cropName, count) } })
                plan.unreportedStepChange?.let { addProperty("movedBy", it.storedName) }
            })
            harvestedByPlotId[grid.layout.id] = harvested
            stepChangeByPlotId[grid.layout.id] = plan.unreportedStepChange
        }
        return SentCoopSteps(body, harvestedByPlotId, stepChangeByPlotId)
    }

    fun readCoopHarvested(reply: JsonObject, sent: SentCoopSteps?) {
        val coopHarvestedByPlotId = reply.get("coopHarvested") as? JsonObject
        GreenhouseData.greenhouseGrids.forEach { grid ->
            val plan = grid.state.coopPlan ?: return@forEach
            val plotId = grid.layout.id
            val sentHarvested = sent?.harvestedByPlotId?.get(plotId)
            if (sentHarvested != null && plan.unreportedStepChange == sent.stepChangeByPlotId[plotId]) plan.unreportedStepChange = null

            val entry = coopHarvestedByPlotId?.get(plotId) as? JsonObject ?: return@forEach
            val isPlanStepUnchanged = entry.intOrNull("version") == plan.version &&
                    entry.intOrNull("step") == plan.stepIndex + 1 &&
                    entry.longOrNull("stepStartedAt") == plan.progress.stepStartedAt
            if (!isPlanStepUnchanged) return@forEach

            plan.coopHarvested = harvestCountsIn(entry.get("harvested") as? JsonObject)
            if (sentHarvested != null) plan.ownHarvestedInCoopTotals = sentHarvested
        }
    }

    fun knownPlanVersions(): JsonObject = JsonObject().apply {
        GreenhouseData.greenhouseGrids.forEach { grid -> grid.state.coopPlan?.let { addProperty(grid.layout.id, it.version) } }
    }

    fun runOwnPlansOutsideCoop() {
        GreenhouseData.greenhouseGrids.filter { it.state.runs == PlanOwner.Coop }.forEach { FlowRunner.switchRunningPlan(it, PlanOwner.Mine) }
    }

    fun applyPulledPlans(pulledPlans: JsonObject) {
        arePlansPulledThisSession = true
        val pulledPlanByPlotId: Map<String, PulledPlan?> = pulledPlans.entrySet()
            .filter { PLOT_ID_PATTERN.matches(it.key) }
            .take(CoopPlan.MAX_COOP_PLANS)
            .associate { (plotId, json) -> plotId to (json as? JsonObject)?.let(::pulledPlanOf) }

        val clearedPlanByGrid = GreenhouseData.greenhouseGrids
            .filter { it.layout.id !in pulledPlanByPlotId }
            .mapNotNull { grid -> grid.state.coopPlan?.let { grid to it } }
            .toMap()
            .toMutableMap()

        pulledPlanByPlotId.forEach { (plotId, pulled) ->
            pulled ?: return@forEach
            val movedFromGrid = clearedPlanByGrid.entries
                .firstOrNull { (_, plan) -> plan.sharedBy == pulled.sharedBy && plan.sharedAt == pulled.sharedAt }
                ?.key

            val grid = GreenhouseData.greenhouseGrids.find { it.layout.id == plotId }
            if (grid == null) {
                movedFromGrid?.let { clearedPlanByGrid.remove(it) }
                return@forEach
            }
            val local = grid.state.coopPlan
            if (local != null && pulled.version < local.version) return@forEach
            if (local != null && local.version == pulled.version) {
                adoptPulledStep(grid, local, pulled)
                return@forEach
            }
            val plan = decodedPulledPlan(grid, pulled) ?: run {
                Common.LOGGER.warn("could not read the coop plan pulled for {}", plotId)
                if (local != null) dropCoopPlan(grid)
                return@forEach
            }
            movedFromGrid?.let { clearedPlanByGrid.remove(it) }
            installPulledPlan(grid, local, plan, movedFromGrid)
        }

        clearedPlanByGrid.keys.forEach { grid ->
            dropCoopPlan(grid)
            announceClearedPlan(grid)
        }
    }

    private fun pulledPlanOf(json: JsonObject): PulledPlan? {
        val latestAcceptedTime = CoopPlan.latestAcceptedTime()
        val version = json.intOrNull("version")?.takeIf { it >= 0 } ?: return null
        val plotIndex = json.intOrNull("plot")?.takeIf { it in 0 until GreenhouseLayout.MAX_PLOTS } ?: return null
        val sharedByJson = json.get("sharedBy")
        val sharedBy = if (sharedByJson == null || sharedByJson.isJsonNull) null else json.stringOrNull("sharedBy")?.takeIf(CoopPlan::isPlayerName) ?: return null
        val stepLabels = (json.get("stepLabels") as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { label -> label.isString }?.asString }
            ?.take(MAX_FLOW_STEPS)
            ?: return null

        return PulledPlan(
            version = version,
            code = json.stringOrNull("code")?.takeIf { it.length <= CoopPlan.MAX_CODE_LENGTH },
            plotIndex = plotIndex,
            name = json.stringOrNull("name") ?: return null,
            stepLabels = stepLabels,
            sharedBy = sharedBy,
            sharedAt = json.longOrNull("sharedAt")?.takeIf { it > 0 }?.coerceAtMost(latestAcceptedTime) ?: return null,
            step = json.intOrNull("step") ?: return null,
            stepStartedAt = json.longOrNull("stepStartedAt")?.takeIf { it > 0 }?.coerceAtMost(latestAcceptedTime) ?: return null,
            stepChangedBy = json.stringOrNull("stepChangedBy")?.takeIf(CoopPlan::isPlayerName),
            stepChange = StepChange.ofStoredName(json.stringOrNull("stepChange")),
            harvested = harvestCountsIn(json.get("harvested") as? JsonObject)
        )
    }

    private fun decodedPulledPlan(grid: GreenhouseGrid, pulled: PulledPlan): CoopPlan? {
        val code = pulled.code ?: return null
        val plan = CoopPlan.decodedOrNull(
            plotId = grid.layout.id,
            code = code,
            plotIndex = pulled.plotIndex,
            name = pulled.name,
            stepLabels = pulled.stepLabels,
            sharedBy = pulled.sharedBy,
            sharedAt = pulled.sharedAt,
            version = pulled.version,
            stepNumber = pulled.step,
            stepStartedAt = pulled.stepStartedAt,
            stepChangedBy = pulled.stepChangedBy,
            stepChange = pulled.stepChange,
            fallbackStepIndexOf = { plot -> PresetMatcher.pairingOf(plot, grid, noRotate = false).stepIndex }
        ) ?: return null
        plan.coopHarvested = pulled.harvested
        return plan
    }

    private fun adoptPulledStep(grid: GreenhouseGrid, plan: CoopPlan, pulled: PulledPlan) {
        plan.sharedAt = pulled.sharedAt
        val pulledStepIndex = (pulled.step - 1).takeIf { it in plan.plot.steps.indices } ?: run {
            resetCoopPlanToBestStep(grid, plan)
            return
        }
        val localStartedAt = plan.progress.stepStartedAt ?: 0L
        val leftStepIndex = plan.stepIndex
        if (pulledStepIndex != leftStepIndex && pulled.stepStartedAt < localStartedAt) return

        if (pulledStepIndex != leftStepIndex) {
            FlowRunner.moveCoopPlanToStep(grid, plan, pulledStepIndex, pulled.stepStartedAt)
            plan.stepChangedBy = pulled.stepChangedBy
            plan.stepChange = pulled.stepChange
            announceStepChange(grid, plan, leftStepIndex)
        } else if (pulled.stepStartedAt != localStartedAt) {
            plan.progress.stepStartedAt = pulled.stepStartedAt
            GreenhouseData.noteUnsavedChange()
        }
        plan.unreportedStepChange = null
        plan.coopHarvested = pulled.harvested
    }

    private fun resetCoopPlanToBestStep(grid: GreenhouseGrid, plan: CoopPlan) {
        val bestStepIndex = PresetMatcher.pairingOf(plan.plot, grid, noRotate = false).stepIndex
        if (bestStepIndex != plan.stepIndex) FlowRunner.moveCoopPlanToStep(grid, plan, bestStepIndex, System.currentTimeMillis())
        plan.forgetCoopHarvests()
        plan.unreportedStepChange = null
    }

    private fun installPulledPlan(grid: GreenhouseGrid, replaced: CoopPlan?, plan: CoopPlan, movedFromGrid: GreenhouseGrid?) {
        installCoopPlan(grid, plan, followsPlan = replaced == null)
        movedFromGrid?.let { oldGrid ->
            dropCoopPlan(oldGrid)
            announceMovedPlan(oldGrid, grid.layout.displayName(), plan.stepIndex + 1, plan.stepCount)
            return
        }
        val plotNumber = grid.layout.number
        if (grid.state.runs == PlanOwner.Coop || plotNumber == null) {
            ChatUtils.sendWithPrefix(sharedLineOf(plan, grid))
            return
        }
        ChatUtils.sendWithPrefix(
            Component.literal("${CoopPlan.memberNameOpeningSentence(plan.sharedBy)} shared ${plan.name} as ${grid.layout.displayName()}'s coop plan. You're on your own plan. ")
                .append(
                    ChatUtils.buildStyled(
                        FOLLOW_LABEL,
                        ChatFormatting.GOLD,
                        click = ClickEvent.RunCommand("${FollowCoopPlanCommand.command} $plotNumber"),
                        underlined = true
                    )
                )
        )
    }

    private fun installCoopPlan(grid: GreenhouseGrid, plan: CoopPlan, followsPlan: Boolean) {
        grid.state.coopPlan = plan
        FlowRunner.startCoopPlan(grid, plan)
        if (followsPlan) grid.state.runs = PlanOwner.Coop
        restartPlannerOn(grid)
    }

    private fun dropCoopPlan(grid: GreenhouseGrid) {
        grid.state.coopPlan = null
        grid.state.runs = PlanOwner.Mine
        restartPlannerOn(grid)
    }

    private fun restartPlannerOn(grid: GreenhouseGrid) {
        grid.state.buildAnnounced = false
        PlannerNeeds.forgetSentLines(grid)
        GreenhouseData.noteUnsavedChange()
        GreenhouseData.regenRender()
        GreenhouseData.calculateSpawnForecasts(grid)
    }

    private fun sharedLineOf(plan: CoopPlan, grid: GreenhouseGrid): String =
        "${CoopPlan.memberNameOpeningSentence(plan.sharedBy)} shared ${plan.name} as ${grid.layout.displayName()}'s coop plan, step ${plan.stepIndex + 1} of ${plan.stepCount}"

    private fun announceStepChange(grid: GreenhouseGrid, plan: CoopPlan, leftStepIndex: Int) {
        if (!grid.state.isFollowingCoopPlan || !GreenhousePresets.warningTypeEnabled(PlantWarnings.FLOW_STEP_KEY)) return
        if (plan.stepChangedBy == ownName) return
        val changedBy = CoopPlan.memberNameOpeningSentence(plan.stepChangedBy)
        val seenBy = CoopPlan.memberNameInSentence(plan.stepChangedBy)

        val plotName = grid.layout.displayName()
        val stepNumber = plan.stepIndex + 1
        val stepTitle = plan.stepTitle(plan.stepIndex)
        val line = when (plan.stepChange) {
            StepChange.Scan -> "$plotName finished step ${leftStepIndex + 1}, now on step $stepNumber · $stepTitle (seen by $seenBy)"
            StepChange.Back -> "$changedBy moved $plotName back to step $stepNumber · $stepTitle"
            StepChange.Pick, null -> "$changedBy moved $plotName to step $stepNumber · $stepTitle"
        }
        ChatUtils.sendWithPrefix(line)
    }

    private fun announceMovedPlan(oldGrid: GreenhouseGrid, newPlotName: String, stepNumber: Int, stepCount: Int) {
        ChatUtils.sendWithPrefix(
            "${oldGrid.layout.displayName()}'s greenhouse is gone; its coop plan moved to the new greenhouse on $newPlotName, step $stepNumber of $stepCount"
        )
    }

    private fun announceClearedPlan(grid: GreenhouseGrid) {
        val plotName = grid.layout.displayName()
        if (isGreenhouseGone(grid.layout.id)) {
            ChatUtils.sendWithPrefix("$plotName's greenhouse is gone; its coop plan was cleared")
        } else {
            ChatUtils.sendWithPrefix("$plotName's coop plan was cleared")
        }
    }

    private fun isGreenhouseGone(plotId: String): Boolean =
        PlotAPI.plots.find { PlotLayout.plotId(it.id) == plotId }?.data?.isGreenhouse == false

    fun followCoopPlanOn(plotNumber: Int) {
        val grid = GreenhouseData.greenhouseGrids.find { it.layout.id == PlotLayout.plotId(plotNumber) } ?: return
        if (grid.state.coopPlan == null) return
        FlowRunner.switchRunningPlan(grid, PlanOwner.Coop)
    }

    fun sharePlotAsCoopPlan(grid: GreenhouseGrid, plot: PlotLayout) {
        val profileId = profileId ?: return
        if (GreenhouseData.presetGrids.none { preset -> preset.plots.any { it === plot } }) return
        val planName = GreenhouseData.fullPlotName(plot).toShareableName(CoopPlan.MAX_NAME_LENGTH).ifEmpty { plot.displayName() }
        val code = when (val exported = MagicAddonsFormat.export(plot)) {
            is LayoutTransferResult.Exported -> exported.text
            is LayoutTransferResult.Failure -> {
                ChatUtils.sendWithPrefix(exported.reason)
                return
            }
            is LayoutTransferResult.Imported -> return
        }
        if (code.length > CoopPlan.MAX_CODE_LENGTH) {
            ChatUtils.sendWithPrefix("$planName is too big to share (max 16 KB)")
            return
        }
        val plotIndex = 0
        val stepLabels = stepLabelsOf(plot)
        val (stepIndex, ownStepStartedAt) = sharedStepOf(grid, plot)
        val replaced = grid.state.coopPlan
        val keepsHarvests = replaced?.isCurrentStepKeptBy(plot, stepIndex) == true
        val stepStartedAt = replaced?.progress?.stepStartedAt?.takeIf { keepsHarvests } ?: ownStepStartedAt
        val body = planBodyOf(profileId, grid.layout.id, code, plotIndex, planName, stepLabels, stepIndex, stepStartedAt).apply {
            if (keepsHarvests) addProperty("keepHarvests", true)
        }

        sendPlanRequest(HTTP_POST, body).thenAccept { response ->
            Minecraft.getInstance().execute {
                val version = versionIn(response) ?: run {
                    ChatUtils.sendWithPrefix("Could not share $planName to the coop (${statusTextOf(response)})")
                    return@execute
                }
                val plan = CoopPlan.decodedOrNull(
                    plotId = grid.layout.id,
                    code = code,
                    plotIndex = plotIndex,
                    name = planName,
                    stepLabels = stepLabels,
                    sharedBy = ownName,
                    sharedAt = System.currentTimeMillis(),
                    version = version,
                    stepNumber = stepIndex + 1,
                    stepStartedAt = stepStartedAt,
                    stepChangedBy = null,
                    stepChange = null,
                    fallbackStepIndexOf = null
                ) ?: return@execute
                installCoopPlan(grid, plan, followsPlan = true)
                if (keepsHarvests && replaced != null) plan.takeHarvestsFrom(replaced)
                ChatUtils.sendWithPrefix(sharedLineOf(plan, grid))
            }
        }
    }

    private fun sharedStepOf(grid: GreenhouseGrid, plot: PlotLayout): Pair<Int, Long> {
        val now = System.currentTimeMillis()
        val ownPlan = grid.state.ownPlan
        val runningStepIndex = ownPlan.assignedLayout?.takeIf { GreenhouseData.presetPlotOf(it) === plot }?.let { plot.stepIndexOf(it) }
        if (runningStepIndex != null) return runningStepIndex to (ownPlan.stepStartedAt?.coerceAtMost(now) ?: now)

        return PresetMatcher.pairingOf(plot, grid, noRotate = false).stepIndex to now
    }

    private fun stepLabelsOf(plot: PlotLayout): List<String> =
        plot.steps.mapIndexed { index, step -> step.title(index).toShareableName(MAX_STEP_LABEL_LENGTH).ifEmpty { "Step ${index + 1}" } }

    fun clearCoopPlan(grid: GreenhouseGrid) {
        val profileId = profileId ?: return
        val body = JsonObject().apply {
            addProperty("profileId", profileId)
            addProperty("plotId", grid.layout.id)
        }
        sendPlanRequest(HTTP_DELETE, body).thenAccept { response ->
            Minecraft.getInstance().execute {
                val plotName = grid.layout.displayName()
                if (response?.statusCode() != ServerSession.HTTP_OK) {
                    ChatUtils.sendWithPrefix("Could not clear $plotName's coop plan (${statusTextOf(response)})")
                    return@execute
                }
                dropCoopPlan(grid)
                ChatUtils.sendWithPrefix("$ownName cleared $plotName's coop plan")
            }
        }
    }

    fun moveOrClearPlansOfGoneGreenhouses() {
        if (!arePlansPulledThisSession || CoopSync.state?.isCoop != true || !SBLocation.OwnGarden.inside()) return
        val plots = PlotAPI.plots
        if (plots.isEmpty()) return

        val greenhousePlotIds = plots.filter { it.data?.isGreenhouse == true }.mapTo(HashSet()) { PlotLayout.plotId(it.id) }
        val goneGreenhousePlotIds = plots.filter { it.data?.isGreenhouse == false }.mapTo(HashSet()) { PlotLayout.plotId(it.id) }
        val gridsWithPlan = GreenhouseData.greenhouseGrids.filter { it.state.coopPlan != null }
        val freeGreenhousePlotIds = greenhousePlotIds - gridsWithPlan.mapTo(HashSet()) { it.layout.id }

        gridsWithPlan.filter { it.layout.id in goneGreenhousePlotIds }.forEach { grid ->
            val plan = grid.state.coopPlan ?: return@forEach
            val attemptKey = "${grid.layout.id}@${plan.version}"
            if (attemptKey in goneGreenhouseAttempts) return@forEach
            when {
                freeGreenhousePlotIds.size == 1 && plots.none { it.data == null } -> {
                    goneGreenhouseAttempts += attemptKey
                    movePlanToNewGreenhouse(grid, plan, freeGreenhousePlotIds.single())
                }
                freeGreenhousePlotIds.size >= 2 -> {
                    goneGreenhouseAttempts += attemptKey
                    clearPlanOfGoneGreenhouse(grid)
                }
            }
        }
    }

    private fun movePlanToNewGreenhouse(oldGrid: GreenhouseGrid, plan: CoopPlan, newPlotId: String) {
        val profileId = profileId ?: return
        val newGrid = GreenhouseData.greenhouseGrids.find { it.layout.id == newPlotId }
        val stepIndex = newGrid?.let { PresetMatcher.pairingOf(plan.plot, it, noRotate = false).stepIndex } ?: 0
        val body = JsonObject().apply {
            addProperty("profileId", profileId)
            addProperty("plotId", newPlotId)
            addProperty("movedFrom", oldGrid.layout.id)
            addProperty("step", stepIndex + 1)
            addProperty("stepStartedAt", System.currentTimeMillis())
        }

        sendPlanRequest(HTTP_POST, body).thenAccept { response ->
            Minecraft.getInstance().execute {
                val status = response?.statusCode()
                if (status == ServerSession.HTTP_OK || status == HTTP_BAD_REQUEST) {
                    CoopSync.requestCoopPlanPull()
                    return@execute
                }
                Common.LOGGER.info("coop plan move from {} refused: {}", oldGrid.layout.id, status ?: "unreachable")
            }
        }
    }

    private fun clearPlanOfGoneGreenhouse(grid: GreenhouseGrid) {
        val profileId = profileId ?: return
        val body = JsonObject().apply {
            addProperty("profileId", profileId)
            addProperty("plotId", grid.layout.id)
            addProperty("greenhouseGone", true)
        }
        sendPlanRequest(HTTP_DELETE, body).thenAccept { response ->
            Minecraft.getInstance().execute {
                if (response?.statusCode() != ServerSession.HTTP_OK) {
                    Common.LOGGER.info("coop plan clear of the gone greenhouse {} refused: {}", grid.layout.id, response?.statusCode() ?: "unreachable")
                    return@execute
                }
                dropCoopPlan(grid)
                ChatUtils.sendWithPrefix("${grid.layout.displayName()}'s greenhouse is gone; its coop plan was cleared")
            }
        }
    }

    private fun planBodyOf(
        profileId: String,
        plotId: String,
        code: String,
        plotIndex: Int,
        planName: String,
        stepLabels: List<String>,
        stepIndex: Int,
        stepStartedAt: Long
    ): JsonObject = JsonObject().apply {
        addProperty("profileId", profileId)
        addProperty("plotId", plotId)
        addProperty("code", code)
        addProperty("plot", plotIndex)
        addProperty("name", planName)
        add("stepLabels", JsonArray().apply { stepLabels.forEach { add(it) } })
        addProperty("step", stepIndex + 1)
        addProperty("stepStartedAt", stepStartedAt)
    }

    private fun sendPlanRequest(httpMethod: String, body: JsonObject): CompletableFuture<HttpResponse<String>?> =
        ServerSession.sendAuthorized(PLAN_PATH) {
            header("Content-Type", "application/json")
            method(httpMethod, HttpRequest.BodyPublishers.ofString(body.toString()))
        }

    private fun versionIn(response: HttpResponse<String>?): Int? {
        if (response?.statusCode() != ServerSession.HTTP_OK) return null
        return runCatching { JsonParser.parseString(response.body()).asJsonObject.intOrNull("version") }.getOrNull()
    }

    private fun statusTextOf(response: HttpResponse<String>?): String = response?.statusCode()?.toString() ?: "server unreachable"

    private fun harvestCountsIn(json: JsonObject?): Map<String, Int> {
        json ?: return emptyMap()
        val counts = json.entrySet().mapNotNull { (cropName, count) ->
            (count as? JsonPrimitive)?.takeIf { it.isNumber }?.let { cropName to it.asInt }
        }.toMap()
        return CoopPlan.harvestCountsOf(counts)
    }

    private fun JsonObject.intOrNull(key: String): Int? = (get(key) as? JsonPrimitive)?.takeIf { it.isNumber }?.asInt

    private fun JsonObject.longOrNull(key: String): Long? = (get(key) as? JsonPrimitive)?.takeIf { it.isNumber }?.asLong

    private fun JsonObject.stringOrNull(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.asString
}
