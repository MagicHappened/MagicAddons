package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import com.google.gson.JsonObject
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.plot.Codecs.MASTER_LAYOUT_CODEC
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.handlers.CodecStorage
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.data.handlers.ModFiles.LoadResult
import org.magic.magicaddons.util.ChatUtils
import java.nio.file.Path
import kotlin.io.path.exists

object PresetStorage {

    private const val PRESETS_FILE_NAME: String = "presets.json"
    private const val PRESETS_KEY: String = "presets"

    private val presetsFile: Path get() = ModFiles.dataDir.resolve(PRESETS_FILE_NAME)

    private var isSavingBlocked: Boolean = false

    fun loadPresets() {
        val presets = if (presetsFile.exists()) readPresetsFile() else presetsMovedOutOfProfiles()
        GreenhouseData.presetGrids = presets.toMutableList()
        GreenhouseData.presetGrids.forEach { preset ->
            if (preset.repairPlotIds()) Common.LOGGER.warn("Preset ${preset.displayName()} had plots sharing an id, renumbered")
        }
    }

    fun savePresets() {
        if (isSavingBlocked) return

        runCatching {
            CodecStorage.save(presetsFile, listOf(CodecStorage.RootEntry(PRESETS_KEY, MASTER_LAYOUT_CODEC.listOf(), GreenhouseData.presetGrids)), ::decodePresets)
        }.onFailure {
            Common.LOGGER.error("Could not save presets to $presetsFile, the previous file is untouched", it)
            ChatUtils.sendWithPrefix("Presets could not be saved; the previous save is kept. Check the log for details.")
        }
    }

    private fun readPresetsFile(): List<GreenhouseLayout> = when (val result = CodecStorage.load(presetsFile, ::decodePresets)) {
        is LoadResult.Loaded -> result.value
        is LoadResult.NoFile -> emptyList()
        is LoadResult.Unreadable -> {
            isSavingBlocked = true
            Common.LOGGER.error("Presets could not be read from $presetsFile, saving is blocked", result.cause)
            ChatUtils.sendWithPrefix("Presets could not be read and there is no usable backup. They will not be saved until this is fixed.")
            emptyList()
        }
    }

    private fun decodePresets(root: JsonObject): List<GreenhouseLayout> =
        CodecStorage.decodeEntry(root, MASTER_LAYOUT_CODEC.listOf(), PRESETS_KEY).orEmpty()

    private fun presetsMovedOutOfProfiles(): List<GreenhouseLayout> {
        val merged = mutableListOf<GreenhouseLayout>()

        GreenhouseProfiles.profileIds().forEach { profileId ->
            val contents = (GreenhouseProfiles.readGreenhouseFile(profileId) as? LoadResult.Loaded)?.value ?: return@forEach
            val renamedPlotIds = mutableMapOf<String, String>()

            contents.presets.orEmpty().forEach { preset ->
                if (merged.none { it.id == preset.id }) {
                    merged.add(preset)
                    return@forEach
                }
                val renamed = withPresetId(preset, firstFreePresetId(merged))
                preset.plots.zip(renamed.plots).forEach { (before, after) -> renamedPlotIds[before.id] = after.id }
                merged.add(renamed)
            }
            GreenhouseProfiles.removePresetsFromProfileFile(profileId, contents, renamedPlotIds, merged.flatMap { it.plots })
        }

        GreenhouseData.presetGrids = merged
        savePresets()
        Common.LOGGER.info("Moved {} presets out of the profile files into {}", merged.size, presetsFile)
        return merged
    }

    private fun firstFreePresetId(taken: List<GreenhouseLayout>): String =
        generateSequence(1) { it + 1 }.map { PlotLayout.presetId(it) }.first { id -> taken.none { it.id == id } }

    private fun withPresetId(preset: GreenhouseLayout, newId: String): GreenhouseLayout = GreenhouseLayout(
        id = newId,
        name = preset.name,
        plots = preset.plots.mapIndexed { index, plot ->
            PlotLayout(id = GreenhouseLayout.plotId(newId, index), name = plot.name, size = plot.size, slots = plot.slots, plants = plot.plants)
        }.toMutableList()
    )
}
