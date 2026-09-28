package org.magic.magicaddons.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.data.handlers.ModFiles.LoadResult
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils

object MagicAddonsConfigJsonHandler {

    private const val CONFIG_VERSION_NUM = "1.0.4"

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath = ModFiles.modDir.resolve("magicaddons.json")

    private var isSavingBlocked: Boolean = false

    var settingsByCategory: JsonObject = JsonObject()

    fun load(): Boolean {
        isSavingBlocked = false
        settingsByCategory = when (val result = ModFiles.loadTextWithBackup(configPath, ::readSettingsByCategory)) {
            is LoadResult.NoFile -> return false
            is LoadResult.Loaded -> {
                if (result.restoredFromBackup) {
                    Common.LOGGER.warn("The config could not be read, restored $configPath from its backup")
                    val keptAs = result.brokenPath?.let { " The unreadable file was kept as ${it.fileName}." }.orEmpty()
                    ChatUtils.sendWithPrefix("The config could not be read, so the last backup was restored.$keptAs")
                }
                result.value
            }
            is LoadResult.Unreadable -> {
                isSavingBlocked = true
                Common.LOGGER.error("The config could not be read from $configPath, saving is blocked", result.cause)
                ChatUtils.sendWithPrefix(
                    "The config could not be read and there is no usable backup. " +
                            "It will not be saved until this is fixed, so nothing is overwritten. Check the log for details."
                )
                return true
            }
        }

        FeatureManager.syncFromConfigJson()

        writeToDisk()

        Common.LOGGER.info("Successfully loaded config")
        return true
    }

    fun save(): Boolean {
        writeToDisk()

        EventBus.post(ConfigChangedEvent())
        Common.LOGGER.info("Successfully saved config")
        return true
    }

    private fun readSettingsByCategory(text: String): JsonObject {
        var root = JsonParser.parseString(text).asJsonObject

        if (versionOf(root) != CONFIG_VERSION_NUM) {
            root = OldConfigHandler.updateConfig(root, CONFIG_VERSION_NUM)
        }

        return root.get("config") as? JsonObject ?: JsonObject()
    }

    private fun writeToDisk() {
        if (isSavingBlocked) return
        FeatureManager.syncToConfigJson()

        val root = JsonObject().apply {
            add("info", JsonObject().apply { addProperty("version", CONFIG_VERSION_NUM) })
            add("config", settingsByCategory)
        }

        runCatching {
            ModFiles.saveTextWithBackup(configPath, gson.toJson(root)) { JsonParser.parseString(it).asJsonObject }
        }.onFailure {
            Common.LOGGER.error("Could not save the config to $configPath, the previous file is untouched", it)
            ChatUtils.sendWithPrefix("The config could not be saved; the previous save is kept. Check the log for details.")
        }
    }

    private fun versionOf(root: JsonObject): String? = (root.get("info") as? JsonObject)?.get("version")?.asString
}