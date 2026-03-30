package org.magic.magicaddons.config

import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import org.magic.magicaddons.config.data.CategoryMap
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import java.io.File

object MagicAddonsConfigJsonHandler {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("config/magicaddons.json")

    var configMap = mutableMapOf<
            String, //category string
            MutableMap<String, //feature id string
                    MutableMap<String, String>>>()

    fun load() {
        if (!file.exists()) return
        val type = object : TypeToken<MutableMap<String, String>>() {}.type
        configMap = gson.fromJson(file.readText(), type) ?: mutableMapOf()
        FeatureManager.syncFromConfig()
    }

    fun save() {
        configMap = mutableMapOf()
        FeatureManager.syncToConfig()
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(configMap))
    }
}