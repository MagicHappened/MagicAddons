package org.magic.magicaddons.data.handlers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.data.greenhouse.Codecs.MASTER_LAYOUT_CODEC
import org.magic.magicaddons.data.greenhouse.Codecs.MISC_GREENHOUSE_INFO_CODEC
import org.magic.magicaddons.data.greenhouse.MiscGreenhouseInfo
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.OtherProfiles
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** Greenhouse data on disk: one folder a profile, named by the profile's id, each holding its own file. */
object DataHandler {
    val configDir: Path = FabricLoader.getInstance().configDir
    val modDir: Path = configDir.resolve(Common.MOD_ID)
    val dataDir: Path = modDir.resolve("data")

    /** Where the mod wrote before every folder was lowercase. */
    private val legacyModDir: Path = configDir.resolve("MagicAddons")

    private const val FILE_NAME: String = "greenhousepresets.json"
    private const val PROFILE_FILE: String = "profile.json"

    /** Where the data lived before it was split by profile. */
    private val legacyFile: Path = dataDir.resolve(FILE_NAME)

    /** The profile whose data is loaded, null until the game has said which profile this is. */
    var activeProfile: UUID? = null
        private set

    fun init() {
        ensureDirectory(modDir)
        moveLegacyModDir()
        ensureDirectory(dataDir)
    }

    /** Moves anything left in the old MagicAddons folder into the lowercase one, once. */
    private fun moveLegacyModDir() {
        runCatching {
            // the same folder twice on a filesystem that ignores case, so nothing to move
            if (!Files.exists(legacyModDir) || Files.isSameFile(legacyModDir, modDir)) return
            Files.walk(legacyModDir).use { paths ->
                paths.sorted().forEach { source ->
                    val target = modDir.resolve(legacyModDir.relativize(source).toString())
                    if (Files.isDirectory(source)) ensureDirectory(target)
                    else if (!Files.exists(target)) Files.move(source, target)
                }
            }
            legacyModDir.toFile().deleteRecursively()
        }.onFailure { Common.LOGGER.warn("Could not move the old MagicAddons config folder", it) }
    }

    private fun ensureDirectory(path: Path) {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
    }

    fun profileDir(id: UUID): Path = dataDir.resolve(id.toString())
    fun greenhouseFile(id: UUID): Path = profileDir(id).resolve(FILE_NAME)

    /** Every profile folder on disk, the active one included. */
    fun profileIds(): List<UUID> = dataDir.takeIf { it.exists() }
        ?.listDirectoryEntries()
        .orEmpty()
        .filter { it.isDirectory() }
        .mapNotNull { runCatching { UUID.fromString(it.name) }.getOrNull() }

    private val names = mutableMapOf<UUID, String?>()

    /** The fruit name written beside a profile's data, so an offline profile can still be named. */
    fun profileName(id: UUID): String? = names.getOrPut(id) {
        runCatching {
            JsonParser.parseString(profileDir(id).resolve(PROFILE_FILE).readText()).asJsonObject.get("name")?.asString
        }.getOrNull()
    }

    private fun writeProfileName(id: UUID, name: String) {
        ensureDirectory(profileDir(id))
        profileDir(id).resolve(PROFILE_FILE).writeText(JsonObject().apply { addProperty("name", name) }.toString())
        names[id] = name
    }

    /** Puts the current profile's data away and brings [id]'s up, when it is not the one loaded already. */
    fun switchProfile(id: UUID, name: String) {
        if (id == activeProfile) {
            if (profileName(id) != name) writeProfileName(id, name)
            return
        }
        if (activeProfile != null) saveGardenData()

        // data from before the split belongs to whichever profile is seen first
        if (legacyFile.exists() && profileIds().isEmpty()) {
            ensureDirectory(profileDir(id))
            Files.move(legacyFile, greenhouseFile(id))
        }

        activeProfile = id
        writeProfileName(id, name)
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

        // the file names the assigned layout by id, and the presets it points into are loaded by now
        val plots = GreenhouseData.allPlots()
        GreenhouseData.greenhouseGrids.forEach { grid ->
            grid.state.assignedLayout = plots.find { it.id == grid.state.assignedLayoutId }
            grid.state.assignedLayoutId = null
        }
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
