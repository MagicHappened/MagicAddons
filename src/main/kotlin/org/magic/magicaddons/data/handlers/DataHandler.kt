package org.magic.magicaddons.data.handlers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import net.fabricmc.loader.api.FabricLoader
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.plot.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.data.greenhouse.plot.Codecs.MASTER_LAYOUT_CODEC
import org.magic.magicaddons.data.greenhouse.plot.Codecs.MISC_GREENHOUSE_INFO_CODEC
import org.magic.magicaddons.data.greenhouse.plot.MiscGreenhouseInfo
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.OtherProfiles

object DataHandler {

    val configDir: Path = FabricLoader.getInstance().configDir
    val modDir: Path = configDir.resolve(Common.MOD_ID)
    val dataDir: Path = modDir.resolve("data")


    private const val FILE_NAME: String = "greenhousepresets.json"
    private const val PROFILE_FILE: String = "profile.json"

    var activeProfile: UUID? = null
        private set

    fun init() {
        ensureDirectory(modDir)
        ensureDirectory(dataDir)
    }

    private fun ensureDirectory(path: Path) {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
    }

    fun profileDir(profileId: UUID): Path = dataDir.resolve(profileId.toString())
    fun greenhouseFile(profileId: UUID): Path = profileDir(profileId).resolve(FILE_NAME)

    fun profileIds(): List<UUID> = dataDir.takeIf { it.exists() }
        ?.listDirectoryEntries()
        .orEmpty()
        .filter { it.isDirectory() }
        .mapNotNull { runCatching { UUID.fromString(it.name) }.getOrNull() }

    private val uuidToFruitName = mutableMapOf<UUID, String?>()

    fun profileFruitName(id: UUID): String? = uuidToFruitName.getOrPut(id) {
        runCatching {
            JsonParser.parseString(profileDir(id).resolve(PROFILE_FILE).readText()).asJsonObject.get("name")?.asString
        }.getOrNull()
    }

    private fun writeProfileFruitName(id: UUID, name: String) {
        ensureDirectory(profileDir(id))
        profileDir(id).resolve(PROFILE_FILE).writeText(JsonObject().apply { addProperty("name", name) }.toString())
        uuidToFruitName[id] = name
    }

    fun switchProfile(id: UUID, name: String) {
        if (id == activeProfile) {
            if (profileFruitName(id) != name) writeProfileFruitName(id, name)
            return
        }
        if (activeProfile != null) saveGardenData()

        activeProfile = id
        writeProfileFruitName(id, name)
        loadGardenData(greenhouseFile(id))
        GreenhouseData.resetForProfile()
        OtherProfiles.reload()
    }

    private fun loadGardenData(file: Path) {
        GreenhouseData.miscInfo = CodecStorage.load(
            file,
            MISC_GREENHOUSE_INFO_CODEC,
            wrapperKey = "misc_info"
        ) ?: run {
            Common.LOGGER.error("Failed to load greenhouse misc data")
            MiscGreenhouseInfo()
        }

        GreenhouseData.presetGrids = CodecStorage.load(
            file,
            MASTER_LAYOUT_CODEC.listOf(),
            wrapperKey = "presets"
        )?.toMutableList() ?: run {
            Common.LOGGER.error("Failed to load preset data")
            return@run mutableListOf()
        }

        GreenhouseData.presetGrids.forEach { preset ->
            if (preset.repairPlotIds()) Common.LOGGER.warn("Preset ${preset.displayName()} had plots sharing an id, renumbered")
        }

        GreenhouseData.greenhousesInitialized = true
        GreenhouseData.greenhouseGrids = CodecStorage.load(
            file,
            GREENHOUSE_GRID_CODEC.listOf(),
            wrapperKey = "greenhouses"
        )?.toMutableList() ?: run {
            GreenhouseData.greenhousesInitialized = false
            Common.LOGGER.error("Failed to load greenhouses data")
            return@run mutableListOf()
        }

        GreenhouseData.resolveAssignedLayoutIds()
    }

    fun saveGardenData() {
        val file = greenhouseFile(activeProfile ?: return)
        ensureDirectory(file.parent)

        CodecStorage.save(
            file,
            listOf(
                CodecStorage.Entry("misc_info", MISC_GREENHOUSE_INFO_CODEC, GreenhouseData.miscInfo),
                CodecStorage.Entry("presets", MASTER_LAYOUT_CODEC.listOf(), GreenhouseData.presetGrids),
                CodecStorage.Entry("greenhouses", GREENHOUSE_GRID_CODEC.listOf(), GreenhouseData.greenhouseGrids)
            )
        )
    }
}
