package org.magic.magicaddons.features.farming.greenhousePresets

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText
import net.minecraft.client.Minecraft
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.PlantStage
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseProfiles
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.BioanalysisAccessory
import org.magic.magicaddons.util.ChatUtils
// for tracking how hypixel mutations spawns work.
object GreenhouseSpawnLog {

    private class RecordedPlant(val x: Int, val y: Int, val cropName: String, val stages: String, val water: Double?) {
        override fun toString(): String = "$x,$y:$cropName@$stages" + (water?.let { "/${"%.1f".format(it)}" } ?: "")
    }

    private class Record(
        val plotId: String,
        var ticks: Int,
        var leftGarden: Boolean,
        val weightMultiplier: Double,
        val layoutBefore: PlotLayout,
        val emptyTargets: List<Plant>
    ) {
        val time: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val plantsBefore: List<RecordedPlant> = recordedPlants(layoutBefore)
    }

    private val rowWriter = Executors.newSingleThreadExecutor { Thread(it, "MagicAddons spawn log").apply { isDaemon = true } }

    private const val CLOSING_WRITE_TIMEOUT_SECONDS: Long = 10

    private val SETTINGS_FILE: Path = ModFiles.modDir.resolve("spawn-log.json")
    private val LOG_DIR: Path = ModFiles.modDir.resolve("collected")
    private val FILE_NAME_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")

    private const val HEADER: String = "time,plot,ticks,left_garden,weight_multiplier,empty_target_spots," +
            "expected_target_spawns,target_spawns,expected_other_spawns,other_spawns_on_targets,spawns_elsewhere," +
            "spawns,plants_before,plants_after"
    private const val TICKS_COLUMN: Int = 2
    private const val EXPECTED_TARGET_COLUMN: Int = 6
    private const val TARGET_SPAWNS_COLUMN: Int = 7

    private var activeFileName: String? = readActiveFileName()
    private val openRecordByGrid = mutableMapOf<GreenhouseGrid, Record>()

    private val isEnabled: Boolean get() = activeFileName != null

    fun toggle() {
        if (activeFileName == null) {
            startFile()
            return
        }

        val fileName = activeFileName
        writeEveryOpenRecord()
        activeFileName = null
        writeSettings()

        rowWriter.execute { announceLogClosed(fileName) }
    }

    private fun announceLogClosed(fileName: String?) {
        val rows = fileName?.let { LOG_DIR.resolve(it) }?.takeIf { it.exists() }?.readLines().orEmpty().drop(1).filter { it.isNotBlank() }
        val columnsByRow = rows.map { splitCsvRow(it) }
        val ticks = columnsByRow.sumOf { it.getOrNull(TICKS_COLUMN)?.toIntOrNull() ?: 0 }
        val targetSpawns = columnsByRow.sumOf { it.getOrNull(TARGET_SPAWNS_COLUMN)?.toIntOrNull() ?: 0 }
        val expectedTargetSpawns = columnsByRow.sumOf { it.getOrNull(EXPECTED_TARGET_COLUMN)?.toDoubleOrNull() ?: 0.0 }
        val summary = "Spawn log off. ${rows.size} rows, $ticks ticks: $targetSpawns target spawns, ${"%.1f".format(expectedTargetSpawns)} expected."
        Minecraft.getInstance().execute { ChatUtils.sendWithPrefix(summary) }
    }

    fun discardOpenRecords() {
        openRecordByGrid.clear()
    }

    fun noteGrowthTicks(grid: GreenhouseGrid, ticks: Int, leftGarden: Boolean) {
        if (!isEnabled || GreenhouseProfiles.holdsAlphaData) return

        val openRecord = openRecordByGrid[grid]
        if (openRecord != null) {
            openRecord.ticks += ticks
            openRecord.leftGarden = openRecord.leftGarden && leftGarden
            return
        }

        openRecordByGrid[grid] = Record(
            plotId = grid.layout.id,
            ticks = ticks,
            leftGarden = leftGarden,
            weightMultiplier = BioanalysisAccessory.mutationWeightMultiplier(),
            layoutBefore = grid.layout.freshCopy(),
            emptyTargets = emptyTargets(grid)
        )
    }

    fun noteScan(grid: GreenhouseGrid) {
        if (!isEnabled || GreenhouseProfiles.holdsAlphaData) return
        val record = openRecordByGrid.remove(grid) ?: return
        writeRow(record, grid.layout.freshCopy())
    }

    fun onGameClosing() {
        if (!isEnabled) return
        writeEveryOpenRecord()
        rowWriter.shutdown()
        rowWriter.awaitTermination(CLOSING_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun writeEveryOpenRecord() {
        openRecordByGrid.values.forEach { writeRow(it, layoutAfter = null) }
        openRecordByGrid.clear()
    }

    private fun writeRow(record: Record, layoutAfter: PlotLayout?) {
        val fileName = activeFileName ?: return
        rowWriter.execute {
            runCatching { appendRow(fileName, record, layoutAfter) }
                .onFailure { Common.LOGGER.warn("Could not write a spawn log row for ${record.plotId}", it) }
        }
    }

    private fun appendRow(fileName: String, record: Record, layoutAfter: PlotLayout?) {
        val spawns = layoutAfter?.let { spawnsBetween(record.layoutBefore, it) }.orEmpty()
        val targetsByOrigin = record.emptyTargets.associateBy { it.slot.x to it.slot.y }
        val (spawnsOnTargets, spawnsElsewhere) = spawns.partition { (it.slot.x to it.slot.y) in targetsByOrigin }
        val targetSpawns = spawnsOnTargets.count { targetsByOrigin.getValue(it.slot.x to it.slot.y).acceptsCrop(it.cropDef) }
        val expected = PlotPrediction.expectedSpawnsDuringAbsence(record.layoutBefore, record.emptyTargets, record.ticks, record.weightMultiplier)

        val row = listOf(
            record.time.toString(),
            record.plotId,
            record.ticks.toString(),
            if (record.leftGarden) "yes" else "no",
            "%.2f".format(record.weightMultiplier),
            record.emptyTargets.size.toString(),
            "%.2f".format(expected.onTargets),
            targetSpawns.toString(),
            "%.2f".format(expected.otherOnTargets),
            (spawnsOnTargets.size - targetSpawns).toString(),
            spawnsElsewhere.size.toString(),
            csvField(spawns.map { recordedPlant(it) }.joinToString(";")),
            csvField(record.plantsBefore.joinToString(";")),
            csvField(layoutAfter?.let { recordedPlants(it) }.orEmpty().joinToString(";"))
        )
        Files.write(LOG_DIR.resolve(fileName), listOf(row.joinToString(",")), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    private fun spawnsBetween(before: PlotLayout, after: PlotLayout): List<Plant> {
        val cellsCoveredBefore = before.plants.flatMapTo(mutableSetOf()) { it.coveredCells }
        return after.plants
            .filter { it.cropDef.isMutation && !it.placed && it.coveredCells.none { cell -> cell in cellsCoveredBefore } }
            .sortedWith(compareBy({ it.slot.y }, { it.slot.x }))
    }

    private fun emptyTargets(grid: GreenhouseGrid): List<Plant> {
        val plan = grid.state.assignedLayout?.turnedBy(grid.state.planTurns) ?: return emptyList()

        return plan.plants
            .filter { it.slot.mark == LayoutSlot.Marking.Target }
            .filter { planned -> grid.layout.plantCovering(planned.slot.x, planned.slot.y) == null }
    }

    private fun recordedPlants(layout: PlotLayout): List<RecordedPlant> =
        layout.plants.sortedWith(compareBy({ it.slot.y }, { it.slot.x })).map { recordedPlant(it) }

    private fun recordedPlant(plant: Plant): RecordedPlant =
        RecordedPlant(plant.slot.x, plant.slot.y, plant.cropDef.name, stagesOf(plant), plant.waterLevel)

    private fun stagesOf(plant: Plant): String = when (val stage = plant.growthStage) {
        is PlantStage.Known -> stage.stage.toString()
        is PlantStage.Estimated -> "${stage.range.first}-${stage.range.last}"
        null -> "?"
    }

    private fun startFile() {
        val newFileName = "spawn-log-${LocalDateTime.now().format(FILE_NAME_TIME)}.csv"
        ModFiles.writeTextAtomically(LOG_DIR.resolve(newFileName), HEADER + "\n")
        activeFileName = newFileName
        writeSettings()
        ChatUtils.sendWithPrefix("Spawn log on, writing to collected/$newFileName")
    }

    private fun readActiveFileName(): String? = runCatching {
        val settings = ModFiles.loadTextWithBackup(SETTINGS_FILE) { JsonParser.parseString(it).asJsonObject.get("activeFile")?.asString }
        val fileName = (settings as? ModFiles.LoadResult.Loaded)?.value ?: return null
        val file = LOG_DIR.resolve(fileName)
        if (!file.exists()) {
            ModFiles.writeTextAtomically(file, HEADER + "\n")
            return fileName
        }
        if (file.readLines().firstOrNull() == HEADER) return fileName

        val replacementFileName = "spawn-log-${LocalDateTime.now().format(FILE_NAME_TIME)}.csv"
        ModFiles.writeTextAtomically(LOG_DIR.resolve(replacementFileName), HEADER + "\n")
        writeSettings(replacementFileName)
        replacementFileName
    }.getOrNull()

    private fun writeSettings(fileName: String? = activeFileName) {
        val settingsJson = JsonObject().apply { fileName?.let { addProperty("activeFile", it) } }.toString()
        runCatching { ModFiles.saveTextWithBackup(SETTINGS_FILE, settingsJson) { JsonParser.parseString(it).asJsonObject } }
            .onFailure { Common.LOGGER.warn("Could not save the spawn log settings", it) }
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
