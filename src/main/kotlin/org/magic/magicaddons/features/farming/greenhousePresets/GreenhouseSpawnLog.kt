package org.magic.magicaddons.features.farming.greenhousePresets

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.Plant
import org.magic.magicaddons.data.greenhouse.SpawnOdds
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.util.ChatUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.pow
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.BioanalysisAccessory

object GreenhouseSpawnLog {

    private class RecordedPlant(val x: Int, val y: Int, val cropName: String, var stages: String, var water: Double?) {
        override fun toString(): String = "$x,$y:$cropName@$stages" + (water?.let { "/${"%.1f".format(it)}" } ?: "")
    }

    private class EmptyTargetSpot(val x: Int, val y: Int, val plannedPlant: Plant, val targetChance: Double, val otherChance: Double) {
        fun expectedSpawns(chance: Double, ticks: Int): Double {
            val anyChance = targetChance + otherChance
            if (anyChance <= 0.0) return 0.0
            return chance / anyChance * (1.0 - (1.0 - anyChance).pow(ticks))
        }
    }

    private class Record(
        val plotId: String,
        val ticks: Int,
        val leftGarden: Boolean,
        val weightMultiplier: Double,
        val emptyTargetSpots: List<EmptyTargetSpot>,
        val plantsBefore: List<RecordedPlant>
    ) {
        val time: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val spawns = mutableListOf<RecordedPlant>()
        var plantsAfter: List<RecordedPlant> = emptyList()
    }

    private val SETTINGS_FILE: Path = DataHandler.modDir.resolve("spawn-log.json")
    private val LOG_DIR: Path = DataHandler.modDir.resolve("collected")
    private val FILE_NAME_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")

    private const val HEADER: String = "time,plot,ticks,left_garden,weight_multiplier,empty_target_spots," +
            "expected_target_spawns,target_spawns,expected_other_spawns,other_spawns_on_targets,spawns_elsewhere," +
            "spawns,plants_before,plants_after"
    private const val TICKS_COLUMN: Int = 2
    private const val EXPECTED_TARGET_COLUMN: Int = 6
    private const val TARGET_SPAWNS_COLUMN: Int = 7

    var lostPlantsMessages: Boolean = readLostPlantsMessages()
        private set

    private var activeFileName: String? = readActiveFileName()
    private val openRecordByGrid = mutableMapOf<GreenhouseGrid, Record>()

    private val isEnabled: Boolean get() = activeFileName != null

    fun toggleLostPlantsMessages(): Boolean {
        lostPlantsMessages = !lostPlantsMessages
        writeSettings()
        return lostPlantsMessages
    }

    fun toggle() {
        if (activeFileName == null) {
            startFile()
            return
        }

        val fileName = activeFileName
        submitEveryRecord()
        activeFileName = null
        writeSettings()

        val rows = fileName?.let { LOG_DIR.resolve(it) }?.takeIf { it.exists() }?.readLines().orEmpty().drop(1).filter { it.isNotBlank() }
        val columnsByRow = rows.map { splitCsvRow(it) }
        val ticks = columnsByRow.sumOf { it.getOrNull(TICKS_COLUMN)?.toIntOrNull() ?: 0 }
        val targetSpawns = columnsByRow.sumOf { it.getOrNull(TARGET_SPAWNS_COLUMN)?.toIntOrNull() ?: 0 }
        val expectedTargetSpawns = columnsByRow.sumOf { it.getOrNull(EXPECTED_TARGET_COLUMN)?.toDoubleOrNull() ?: 0.0 }
        ChatUtils.sendWithPrefix(
            "Spawn log off. ${rows.size} rows, $ticks ticks: $targetSpawns target spawns, ${"%.1f".format(expectedTargetSpawns)} expected."
        )
    }

    fun noteGrowthTicks(grid: GreenhouseGrid, ticks: Int, leftGarden: Boolean) {
        if (!isEnabled) return

        openRecordByGrid.remove(grid)?.let { submit(grid, it) }

        val weightMultiplier = BioanalysisAccessory.mutationWeightMultiplier()
        openRecordByGrid[grid] = Record(
            plotId = grid.layout.id,
            ticks = ticks,
            leftGarden = leftGarden,
            weightMultiplier = weightMultiplier,
            emptyTargetSpots = emptyTargetSpots(grid, weightMultiplier),
            plantsBefore = recordedPlants(grid.layout)
        )
    }

    fun recordSpawn(spawn: Plant, layout: GreenhouseLayout) {
        if (!isEnabled) return
        openRecordByGrid.entries.firstOrNull { it.key.layout === layout }?.value?.spawns?.add(recordedPlant(spawn))
    }

    fun noteScan(grid: GreenhouseGrid) {
        if (!isEnabled) return
        val record = openRecordByGrid[grid] ?: return
        if (record.plantsAfter.isEmpty()) record.plantsAfter = recordedPlants(grid.layout)
    }

    fun onGameClosing() {
        if (!isEnabled) return
        submitEveryRecord()
    }

    private fun submitEveryRecord() {
        openRecordByGrid.entries.toList().forEach { (grid, record) -> submit(grid, record) }
        openRecordByGrid.clear()
    }

    private fun submit(grid: GreenhouseGrid, record: Record) {
        val fileName = activeFileName ?: return

        narrowStageRanges(grid, record)

        val spotsByPosition = record.emptyTargetSpots.associateBy { it.x to it.y }
        val (spawnsOnTargets, spawnsElsewhere) = record.spawns.partition { (it.x to it.y) in spotsByPosition }
        val targetSpawns = spawnsOnTargets.count { spawn ->
            spotsByPosition.getValue(spawn.x to spawn.y).plannedPlant.acceptedCrops.any { it.name == spawn.cropName }
        }

        val row = listOf(
            record.time.toString(),
            record.plotId,
            record.ticks.toString(),
            if (record.leftGarden) "yes" else "no",
            "%.2f".format(record.weightMultiplier),
            record.emptyTargetSpots.size.toString(),
            "%.2f".format(record.emptyTargetSpots.sumOf { it.expectedSpawns(it.targetChance, record.ticks) }),
            targetSpawns.toString(),
            "%.2f".format(record.emptyTargetSpots.sumOf { it.expectedSpawns(it.otherChance, record.ticks) }),
            (spawnsOnTargets.size - targetSpawns).toString(),
            spawnsElsewhere.size.toString(),
            csvField(record.spawns.joinToString(";")),
            csvField(record.plantsBefore.joinToString(";")),
            csvField(record.plantsAfter.joinToString(";"))
        )
        Files.write(LOG_DIR.resolve(fileName), listOf(row.joinToString(",")), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    private fun narrowStageRanges(grid: GreenhouseGrid, record: Record) {
        val plantsBySlot = grid.layout.plants.associateBy { it.slot.x to it.slot.y }

        (record.plantsAfter + record.spawns).forEach { recorded ->
            val recordedRange = stageRangeOf(recorded.stages) ?: return@forEach
            val plant = plantsBySlot[recorded.x to recorded.y] ?: return@forEach
            if (plant.cropDef.name != recorded.cropName) return@forEach

            val narrowed = when (val stage = plant.growthStage) {
                is GrowthStageInfo.Known -> stage.stage..stage.stage
                is GrowthStageInfo.Estimated -> stage.range
                null -> return@forEach
            }
            if (narrowed.first < recordedRange.first || narrowed.last > recordedRange.last) return@forEach
            if (narrowed == recordedRange) return@forEach

            recorded.stages = if (narrowed.first == narrowed.last) "${narrowed.first}" else "${narrowed.first}-${narrowed.last}"
        }
    }

    private fun stageRangeOf(stages: String): IntRange? {
        val first = stages.substringBefore("-").toIntOrNull() ?: return null
        val last = stages.substringAfter("-").toIntOrNull() ?: return null
        return first..last
    }

    private fun emptyTargetSpots(grid: GreenhouseGrid, weightMultiplier: Double): List<EmptyTargetSpot> {
        val plan = grid.state.assignedLayout?.turned(grid.state.planTurns) ?: return emptyList()

        return plan.plants
            .filter { it.slot.mark == LayoutSlot.Marking.Target }
            .filter { planned -> grid.layout.getSlot(planned.slot.x, planned.slot.y)?.let { grid.layout.plantCovering(it) } == null }
            .map { planned ->
                val chances = SpawnOdds.mutationChancesAtSlot(grid.layout, planned.slot.x, planned.slot.y, weightMultiplier)
                val (targetChances, otherChances) = chances.partition { planned.acceptsCrop(it.crop) }
                EmptyTargetSpot(planned.slot.x, planned.slot.y, planned, targetChances.sumOf { it.chance }, otherChances.sumOf { it.chance })
            }
    }

    private fun recordedPlants(layout: GreenhouseLayout): List<RecordedPlant> =
        layout.plants.sortedWith(compareBy({ it.slot.y }, { it.slot.x })).map { recordedPlant(it) }

    private fun recordedPlant(plant: Plant): RecordedPlant =
        RecordedPlant(plant.slot.x, plant.slot.y, plant.cropDef.name, stagesOf(plant), plant.waterLevel)

    private fun stagesOf(plant: Plant): String = when (val stage = plant.growthStage) {
        is GrowthStageInfo.Known -> stage.stage.toString()
        is GrowthStageInfo.Estimated -> "${stage.range.first}-${stage.range.last}"
        null -> "?"
    }

    private fun startFile() {
        val newFileName = "spawn-log-${LocalDateTime.now().format(FILE_NAME_TIME)}.csv"
        Files.createDirectories(LOG_DIR)
        LOG_DIR.resolve(newFileName).writeText(HEADER + "\n")
        activeFileName = newFileName
        writeSettings()
        ChatUtils.sendWithPrefix("Spawn log on, writing to collected/$newFileName")
    }

    private fun readActiveFileName(): String? = runCatching {
        if (!SETTINGS_FILE.exists()) return null
        val fileName = JsonParser.parseString(SETTINGS_FILE.readText()).asJsonObject.get("activeFile")?.asString ?: return null
        val file = LOG_DIR.resolve(fileName)
        if (!file.exists()) {
            Files.createDirectories(LOG_DIR)
            file.writeText(HEADER + "\n")
            return fileName
        }
        if (file.readLines().firstOrNull() == HEADER) return fileName

        val replacementFileName = "spawn-log-${LocalDateTime.now().format(FILE_NAME_TIME)}.csv"
        LOG_DIR.resolve(replacementFileName).writeText(HEADER + "\n")
        writeSettings(replacementFileName)
        replacementFileName
    }.getOrNull()

    private fun readLostPlantsMessages(): Boolean = runCatching {
        SETTINGS_FILE.exists() &&
                JsonParser.parseString(SETTINGS_FILE.readText()).asJsonObject.get("lostPlantsMessages")?.asBoolean == true
    }.getOrDefault(false)

    private fun writeSettings(fileName: String? = activeFileName) {
        SETTINGS_FILE.writeText(
            JsonObject().apply {
                fileName?.let { addProperty("activeFile", it) }
                addProperty("lostPlantsMessages", lostPlantsMessages)
            }.toString()
        )
    }

    private fun csvField(text: String): String =
        if (text.any { it == ',' || it == '"' }) "\"" + text.replace("\"", "\"\"") + "\"" else text

    private fun splitCsvRow(row: String): List<String> {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        row.forEach { character ->
            when {
                character == '"' -> inQuotes = !inQuotes
                character == ',' && !inQuotes -> {
                    fields += field.toString()
                    field.clear()
                }
                else -> field.append(character)
            }
        }
        fields += field.toString()
        return fields
    }
}
