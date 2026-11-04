package org.magic.magicaddons.data.server

import com.google.gson.annotations.SerializedName
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.crops.CropTableExport
import org.magic.magicaddons.data.greenhouse.crops.DecayOutlook
import org.magic.magicaddons.data.greenhouse.crops.PlantStage
import org.magic.magicaddons.data.greenhouse.plot.ExitCondition
import org.magic.magicaddons.data.greenhouse.plot.GREENHOUSE_SIZE
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.MAX_STEP_LABEL_LENGTH
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.data.greenhouse.transfer.toShareableName
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.flows.FlowRunner
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseProfiles
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.BioanalysisAccessory
import tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileAPI
import org.magic.magicaddons.data.greenhouse.crops.Plant as GreenhousePlant

data class ServerGreenhouseData(
    val cropTableVersion: Int,
    val profile: String,
    val profileId: String?,
    val name: String?,
    val isStillOnline: Boolean? = null,
    val visitedGreenhouse: Boolean? = null,
    val inGarden: Boolean? = null,
    val activity: Activity? = null,
    val nextTickInMs: Long,
    val tickMs: Long,
    val gardenDayTime: Long?,
    val mutationWeightMultiplier: Double,
    val settings: Settings,
    val plots: List<Plot>
) {

    data class Settings(
        val aloeHarvestStage: Int,
        val jellybeanHarvestStage: Int,
        val enabledWarnings: List<String>,
        val onlyPresetTargets: Boolean,
        val harvestableBaseCrops: Boolean,
        val harvestableIngredients: Boolean,
        val assumeFlatWater: Boolean,
        val warnOnNegativeWater: Boolean,
        val chorusLossTolerance: Double,
        val spawnConfidence: Double,
        val spawnCount: Int,
        val greenhouseCoop: String
    )

    data class Activity(val harvested: Int, val placed: Int, val watered: Int, val plots: List<String>)

    data class Plot(
        val id: String,
        val name: String,
        val scannedAt: Long?,
        val lastChangedAt: Long?,
        val grid: String,
        val ticksSinceLastScan: Int,
        val slotsBySoil: Map<String, List<Int>>,
        val plants: List<Plant>,
        val plan: List<PlannedSlot>?,
        val chorusLossChanceByTick: List<Double>?,
        val spawnChanceByTick: List<Double>?,
        val spawnCountByTick: List<Int>?,
        val spawnCrops: List<String>?,
        val flow: Flow?
    )

    data class Flow(
        val name: String,
        val step: Int,
        val stepCount: Int,
        val loops: Boolean,
        val stepLabel: String,
        val nextStepLabel: String?,
        val stepStartedAt: Long?,
        val isCoopPlan: Boolean,
        val exits: List<List<FlowCondition>>,
        val progress: List<List<Int?>>,
        val spawnCountByTickOf: Map<String, List<Int>>?
    )

    data class FlowCondition(val kind: FlowConditionKind, val crop: String, val amount: Int)

    enum class FlowConditionKind {
        @SerializedName("spawned") Spawned,
        @SerializedName("stage") Stage,
        @SerializedName("harvested") Harvested
    }

    data class Plant(
        @SerializedName("c") val crop: String,
        @SerializedName("x") val x: Int,
        @SerializedName("y") val y: Int,
        @SerializedName("s") val stage: Int?,
        @SerializedName("sm") val stageMax: Int?,
        @SerializedName("w") val water: Double?,
        @SerializedName("d") val decayInMinutes: Long?,
        @SerializedName("du") val isDecayUncertain: Boolean?,
        @SerializedName("p") val placed: Boolean?,
        @SerializedName("ch") val charge: Int?,
        @SerializedName("r") val readings: Map<String, Int>?
    )

    data class PlannedSlot(val x: Int, val y: Int, val mark: String, val crops: List<String>)

    enum class MissingData { CropGrowth, CropSpeedUpgrade, TickTime, ProfileName, ScannedGreenhouse }

    companion object {

        private val DEFAULT_SOIL = Blocks.FARMLAND

        private const val NO_SOIL: String = "none"

        private const val MS_PER_MINUTE: Long = 60_000

        private const val MAX_FLOW_NAME_LENGTH: Int = 64

        private const val MAX_FLOW_PROGRESS: Int = 9999

        fun missingDataOfActiveProfile(): List<MissingData> = buildList {
            if (GreenhouseData.miscInfo.cropGrowthValue == null) add(MissingData.CropGrowth)
            if (GreenhouseData.miscInfo.cropSpeedUpgradeValue == null) add(MissingData.CropSpeedUpgrade)
            if (GreenhouseData.miscInfo.nextTickTime == null) add(MissingData.TickTime)
            if (activeProfileName() == null) add(MissingData.ProfileName)
            if (scannedGrids().isEmpty()) add(MissingData.ScannedGreenhouse)
        }

        private fun activeProfileName(): String? =
            ProfileAPI.profileName ?: GreenhouseProfiles.activeProfileId?.let { GreenhouseProfiles.fruitNameOf(it) }

        private fun scannedGrids(): List<GreenhouseGrid> = GreenhouseData.greenhouseGrids.filter { it.state.lastScanTime != null }

        fun ofActiveProfile(): ServerGreenhouseData? {
            val profile = activeProfileName() ?: return null
            val nextTickInMs = GreenhouseTickTime.remainingTickMs() ?: return null
            val tickMs = GreenhouseTickTime.tickMs ?: return null
            val plots = scannedGrids().map { plotOf(it) }
            if (plots.isEmpty()) return null

            return ServerGreenhouseData(
                cropTableVersion = CropTableExport.TABLE_VERSION,
                profile = profile,
                profileId = (ProfileAPI.profileId ?: GreenhouseProfiles.activeProfileId)?.toString(),
                name = Minecraft.getInstance().user.name,
                nextTickInMs = nextTickInMs,
                tickMs = tickMs,
                gardenDayTime = Minecraft.getInstance().level?.overworldClockTime,
                mutationWeightMultiplier = BioanalysisAccessory.mutationWeightMultiplier(),
                settings = Settings(
                    aloeHarvestStage = GreenhousePresets.aloeHarvestStage(),
                    jellybeanHarvestStage = GreenhousePresets.jellybeanHarvestStage(),
                    enabledWarnings = GreenhousePresets.enabledWarningTypes(),
                    onlyPresetTargets = GreenhousePresets.harvestHighlightOnlyTargets(),
                    harvestableBaseCrops = GreenhousePresets.countsBaseCropsAsHarvestable(),
                    harvestableIngredients = GreenhousePresets.countsIngredientsAsHarvestable(),
                    assumeFlatWater = GreenhousePresets.assumeFlatWater(),
                    warnOnNegativeWater = GreenhousePresets.negativeWaterWarningEnabled(),
                    chorusLossTolerance = GreenhousePresets.chorusLossTolerance(),
                    spawnConfidence = GreenhousePresets.spawnConfidence(),
                    spawnCount = GreenhousePresets.spawnCountThreshold(),
                    greenhouseCoop = GreenhousePresets.greenhouseCoop().name
                ),
                plots = plots
            )
        }

        private fun spawnChancesOf(grid: GreenhouseGrid): PlotPrediction.SpawnChances? =
            grid.state.spawnChances ?: grid.state.spawnChanceCalculation?.chances?.takeUnless { it.isCompletedExceptionally }?.getNow(null)

        private fun plotOf(grid: GreenhouseGrid): Plot = Plot(
            id = grid.layout.id,
            name = grid.layout.displayName(),
            scannedAt = grid.state.lastScanTime?.toEpochMilli(),
            lastChangedAt = grid.state.lastChangedAt,
            grid = GridBlob.encode(grid),
            ticksSinceLastScan = grid.state.ticksSinceLastScan,
            slotsBySoil = grid.layout.slots
                .filter { it.soil != DEFAULT_SOIL }
                .groupBy({ slot -> slot.soil?.let { BuiltInRegistries.BLOCK.getKey(it).path } ?: NO_SOIL }, { it.y * GREENHOUSE_SIZE + it.x }),
            plants = grid.layout.plants.map { plantOf(grid.layout, it) },
            plan = grid.assignedPlanAfterTurn()?.let { plan ->
                plan.plants
                    .filter { it.slot.mark != null }
                    .map { planned -> PlannedSlot(planned.slot.x, planned.slot.y, planned.slot.mark!!.name, planned.acceptedCrops.map { it.name }) }
            },
            chorusLossChanceByTick = (grid.state.chorusLossChanceByTick
                ?: grid.state.chorusRiskCalculation?.takeIf { grid.state.isChorusRiskCalculating }?.immediateLossChanceByTick())?.toList(),
            spawnChanceByTick = spawnChancesOf(grid)?.chancesOfAtLeast(GreenhousePresets.spawnCountThreshold()),
            spawnCountByTick = spawnChancesOf(grid)?.countsReachedWith(GreenhousePresets.spawnConfidence()),
            spawnCrops = spawnChancesOf(grid)?.crops,
            flow = flowOf(grid)
        )

        private fun flowOf(grid: GreenhouseGrid): Flow? {
            val running = FlowRunner.runningOn(grid) ?: return null
            val plotFlow = running.plot.flow ?: return null
            if (!plotFlow.isWithinSharedBounds) return null
            val exits = running.step.exits

            return Flow(
                name = GreenhouseData.fullPlotName(running.plot).toShareableName(MAX_FLOW_NAME_LENGTH),
                step = running.stepIndex + 1,
                stepCount = running.stepCount,
                loops = plotFlow.loop,
                stepLabel = running.title(running.stepIndex).toShareableName(MAX_STEP_LABEL_LENGTH),
                nextStepLabel = running.nextIndex?.let { running.title(it).toShareableName(MAX_STEP_LABEL_LENGTH) },
                stepStartedAt = grid.state.stepStartedAt,
                isCoopPlan = grid.state.isFollowingCoopPlan,
                exits = exits.map { exit -> exit.conditions.map(::flowConditionOf) },
                progress = exits.map { exit -> exit.conditions.map { FlowRunner.progressOf(grid, it)?.coerceIn(0, MAX_FLOW_PROGRESS) } },
                spawnCountByTickOf = spawnCountByTickOfExitCrops(grid)
            )
        }

        private fun flowConditionOf(condition: ExitCondition): FlowCondition = when (condition) {
            is ExitCondition.MutationSpawned -> FlowCondition(FlowConditionKind.Spawned, condition.crop.name, condition.count)
            is ExitCondition.HighestStageAtLeast -> FlowCondition(FlowConditionKind.Stage, condition.crop.name, condition.stage)
            is ExitCondition.MutationHarvested -> FlowCondition(FlowConditionKind.Harvested, condition.crop.name, condition.count)
        }

        private fun spawnCountByTickOfExitCrops(grid: GreenhouseGrid): Map<String, List<Int>>? {
            val chancesByCrop = grid.state.exitCropSpawnChances
                ?: grid.state.exitCropSpawnCalculation?.chances?.takeUnless { it.isCompletedExceptionally }?.getNow(null)
                ?: return null
            val cropNamesSpawnedToExit = FlowRunner.cropsSpawnedToExit(grid).mapTo(HashSet()) { it.name }
            val confidence = GreenhousePresets.spawnConfidence()

            return chancesByCrop
                .filterKeys { it in cropNamesSpawnedToExit }
                .mapValues { (_, chances) -> chances.countsReachedWith(confidence) }
                .takeIf { it.isNotEmpty() }
        }

        private fun isDecayReachable(outlook: DecayOutlook): Boolean = outlook.isReachableFromCurrentSpots

        private fun plantOf(layout: PlotLayout, plant: GreenhousePlant): Plant {
            val stage = plant.growthStage
            val decayOutlook = layout.decayOutlookOf(plant)

            return Plant(
                crop = plant.cropDef.name,
                x = plant.slot.x,
                y = plant.slot.y,
                stage = when (stage) {
                    is PlantStage.Known -> stage.stage
                    is PlantStage.Estimated -> stage.range.first
                    null -> null
                },
                stageMax = (stage as? PlantStage.Estimated)?.range?.last,
                water = plant.waterLevel,
                decayInMinutes = plant.decayRemainingMs?.takeIf { decayOutlook.canDecay && isDecayReachable(decayOutlook) }?.let { it / MS_PER_MINUTE },
                isDecayUncertain = (!decayOutlook.isCertain).takeIf { it },
                placed = plant.placed.takeIf { it },
                charge = plant.charge.takeIf { it != 0 },
                readings = plant.readings.takeIf { it.isNotEmpty() }
            )
        }
    }
}
