package org.magic.magicaddons.config

import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.features.FeatureManager
import java.io.File

object MagicAddonsConfigJsonHandler {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("config/magicaddons.json")

    var configMap = mutableMapOf<
            String, //category string
            MutableMap<String, //feature id string
                    MutableMap<String, String>>>() //each feature setting id mapped to value

    fun load() : Boolean {
        if (!file.exists()) return false
        val type = object : TypeToken<
                MutableMap<String,
                        MutableMap<String,
                                MutableMap<String, String>
                        >
                >>() {}.type
        configMap = gson.fromJson(file.readText(), type) ?: mutableMapOf()
        FeatureManager.syncFromConfigJson()
        return true
    }

    fun save() : Boolean {
        FeatureManager.syncToConfigJson()
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(configMap))
        EventBus.post(ConfigChangedEvent())
        return true
    }
}