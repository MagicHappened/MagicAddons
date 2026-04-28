package org.magic.magicaddons.config

import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import org.magic.magicaddons.Common
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils
import java.io.File

object MagicAddonsConfigJsonHandler {

    private const val CONFIG_VERSION_NUM = "1.0.1"

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("config/magicaddons/magicaddons.json")

    var configMap = mutableMapOf<
            String, //category string
            MutableMap<String, //feature id string
                    MutableMap<String, Any>>>() //each feature setting id mapped to value

    fun load(): Boolean {
        if (!file.exists()) return false

        val rawType = object : TypeToken<MutableMap<String, Any>>() {}.type
        var raw: MutableMap<String, Any> =
            gson.fromJson(file.readText(), rawType) ?: mutableMapOf()

        val version = extractVersion(raw)

        if (version == null || version != CONFIG_VERSION_NUM) {
            raw = OldConfigHandler.updateConfig(raw, CONFIG_VERSION_NUM)

            file.writeText(gson.toJson(raw))
        }


        val configSection = raw["config"] as? MutableMap<String, Any> ?: mutableMapOf()

        val type = object : TypeToken<
                MutableMap<String,
                        MutableMap<String,
                                MutableMap<String, Any>>>>() {}.type

        configMap = gson.fromJson(gson.toJson(configSection), type) ?: mutableMapOf()

        FeatureManager.syncFromConfigJson()
        Common.LOGGER.info("Successfully loaded config")
        return true
    }

    fun save(): Boolean {
        FeatureManager.syncToConfigJson()

        val wrapped = mutableMapOf<String, Any>(
            "info" to mapOf("version" to CONFIG_VERSION_NUM),
            "config" to configMap
        )

        file.parentFile.mkdirs()
        file.writeText(gson.toJson(wrapped))

        EventBus.post(ConfigChangedEvent())
        Common.LOGGER.info("Successfully saved config")
        return true
    }

    private fun extractVersion(raw: Map<String, Any>): String? {
        val info = raw["info"] as? Map<*, *> ?: return null
        return info["version"] as? String
    }
}