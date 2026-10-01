package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.plot.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.data.greenhouse.plot.Codecs.MASTER_LAYOUT_CODEC
import org.magic.magicaddons.data.greenhouse.plot.Codecs.MISC_GREENHOUSE_INFO_CODEC
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.MiscGreenhouseInfo
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.handlers.CodecStorage
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.data.handlers.ModFiles.LoadResult
import org.magic.magicaddons.util.ChatUtils

object GreenhouseProfiles {

    private const val GREENHOUSE_FILE_NAME: String = "greenhousepresets.json"
    private const val PROFILE_FILE_NAME: String = "profile.json"

    private const val MISC_INFO_KEY: String = "misc_info"
    private const val PRESETS_KEY: String = "presets"
    private const val GREENHOUSES_KEY: String = "greenhouses"

    var activeProfileId: UUID? = null
        private set

    private var isSavingBlocked: Boolean = false

    var holdsAlphaData: Boolean = false
        private set

    class GreenhouseFileContents(
        val miscInfo: MiscGreenhouseInfo?,
        val presets: List<GreenhouseLayout>?,
        val greenhouses: List<GreenhouseGrid>?
    )

    fun profileDirOf(profileId: UUID): Path = ModFiles.dataDir.resolve(profileId.toString())
    fun greenhouseFileOf(profileId: UUID): Path = profileDirOf(profileId).resolve(GREENHOUSE_FILE_NAME)

    fun profileIds(): List<UUID> = ModFiles.dataDir.takeIf { it.exists() }
        ?.listDirectoryEntries()
        .orEmpty()
        .filter { it.isDirectory() }
        .mapNotNull { runCatching { UUID.fromString(it.name) }.getOrNull() }

    private val fruitNameByProfileId = mutableMapOf<UUID, String?>()

    fun fruitNameOf(profileId: UUID): String? = fruitNameByProfileId.getOrPut(profileId) {
        runCatching {
            JsonParser.parseString(profileDirOf(profileId).resolve(PROFILE_FILE_NAME).readText()).asJsonObject.get("name")?.asString
        }.getOrNull()
    }

    private fun writeFruitName(profileId: UUID, name: String) {
        ModFiles.writeTextAtomically(profileDirOf(profileId).resolve(PROFILE_FILE_NAME), JsonObject().apply { addProperty("name", name) }.toString())
        fruitNameByProfileId[profileId] = name
    }

    fun readGreenhouseFile(profileId: UUID): LoadResult<GreenhouseFileContents> =
        CodecStorage.load(greenhouseFileOf(profileId), ::decodeGreenhouseFile)

    private fun decodeGreenhouseFile(root: JsonObject): GreenhouseFileContents = GreenhouseFileContents(
        miscInfo = CodecStorage.decodeEntry(root, MISC_GREENHOUSE_INFO_CODEC, MISC_INFO_KEY),
        presets = CodecStorage.decodeEntry(root, MASTER_LAYOUT_CODEC.listOf(), PRESETS_KEY),
        greenhouses = CodecStorage.decodeEntry(root, GREENHOUSE_GRID_CODEC.listOf(), GREENHOUSES_KEY)
    )

    fun switchProfile(profileId: UUID, name: String) {
        if (profileId == activeProfileId) {
            if (!holdsAlphaData && fruitNameOf(profileId) != name) writeFruitName(profileId, name)
            return
        }

        activeProfileId = profileId
        if (!holdsAlphaData) writeFruitName(profileId, name)
        loadGreenhouseData(profileId, name)
        GreenhouseData.resetForProfile()
        OtherProfiles.reload()
    }

    private fun loadGreenhouseData(profileId: UUID, name: String) {
        isSavingBlocked = false
        val contents = when (val result = readGreenhouseFile(profileId)) {
            is LoadResult.NoFile -> null
            is LoadResult.Loaded -> {
                if (result.restoredFromBackup) {
                    Common.LOGGER.warn("Greenhouse data for $name could not be read, restored ${greenhouseFileOf(profileId)} from its backup")
                    val keptAs = result.brokenPath?.let { " The unreadable file was kept as ${it.fileName}." }.orEmpty()
                    ChatUtils.sendWithPrefix("Greenhouse data for $name could not be read, so the last backup was restored.$keptAs")
                }
                result.value
            }
            is LoadResult.Unreadable -> {
                isSavingBlocked = true
                Common.LOGGER.error("Greenhouse data for $name could not be read from ${greenhouseFileOf(profileId)}, saving is blocked", result.cause)
                ChatUtils.sendWithPrefix(
                    "Greenhouse data for $name could not be read and there is no usable backup. " +
                            "It will not be saved until this is fixed, so nothing is overwritten. Check the log for details."
                )
                null
            }
        }

        GreenhouseData.miscInfo = contents?.miscInfo ?: run {
            Common.LOGGER.error("Failed to load greenhouse misc data")
            MiscGreenhouseInfo()
        }

        val greenhouses = contents?.greenhouses
        GreenhouseData.greenhousesInitialized = greenhouses != null
        GreenhouseData.greenhouseGrids = greenhouses?.toMutableList() ?: run {
            Common.LOGGER.error("Failed to load greenhouses data")
            mutableListOf()
        }

        GreenhouseData.resolveAssignedLayoutIds()
    }

    fun enterAlpha() {
        holdsAlphaData = true
    }

    fun leaveAlpha() {
        if (!holdsAlphaData) return
        holdsAlphaData = false

        val profileId = activeProfileId ?: return
        loadGreenhouseData(profileId, fruitNameOf(profileId) ?: profileId.toString())
        GreenhouseData.resetForProfile()
        OtherProfiles.reload()
    }

    fun saveGreenhouseData() {
        val profileId = activeProfileId ?: return
        writeGreenhouseFile(profileId, GreenhouseData.miscInfo, GreenhouseData.greenhouseGrids)
    }

    fun removePresetsFromProfileFile(
        profileId: UUID,
        contents: GreenhouseFileContents,
        renamedPlotIds: Map<String, String>,
        presetPlots: List<PlotLayout>
    ) {
        val greenhouses = contents.greenhouses ?: return
        greenhouses.forEach { grid ->
            val assignedId = grid.state.assignedLayoutId?.let { renamedPlotIds[it] ?: it }
            grid.state.assignedLayoutId = assignedId
            grid.state.assignedLayout = presetPlots.find { it.id == assignedId }
        }
        writeGreenhouseFile(profileId, contents.miscInfo, greenhouses)
    }

    private fun writeGreenhouseFile(profileId: UUID, miscInfo: MiscGreenhouseInfo?, greenhouses: List<GreenhouseGrid>) {
        if (isSavingBlocked && profileId == activeProfileId) return

        val file = greenhouseFileOf(profileId)
        runCatching {
            CodecStorage.save(
                file,
                listOfNotNull(
                    miscInfo?.let { CodecStorage.RootEntry(MISC_INFO_KEY, MISC_GREENHOUSE_INFO_CODEC, it) },
                    CodecStorage.RootEntry(GREENHOUSES_KEY, GREENHOUSE_GRID_CODEC.listOf(), greenhouses)
                ),
                ::decodeGreenhouseFile
            )
        }.onFailure {
            Common.LOGGER.error("Could not save greenhouse data to $file, the previous file is untouched", it)
            ChatUtils.sendWithPrefix("Greenhouse data could not be saved; the previous save is kept. Check the log for details.")
        }
    }
}
