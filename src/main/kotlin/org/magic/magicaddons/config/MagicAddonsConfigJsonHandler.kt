package org.magic.magicaddons.config

import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import org.magic.magicaddons.config.data.CategoryMap
import org.magic.magicaddons.features.FeatureManager
import java.io.File

object MagicAddonsConfigJsonHandler {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("config/magicaddons.json")

    var configMap: CategoryMap = mutableMapOf()

    fun load() {
        if (!file.exists()) return
        val type = object : TypeToken<CategoryMap>() {}.type
        configMap = gson.fromJson(file.readText(), type) ?: mutableMapOf()
        FeatureManager.syncFromConfig(FeatureManager.features, configMap)
    }

    fun save() {
        configMap = mutableMapOf()
        FeatureManager.syncToConfig(FeatureManager.features, configMap)
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(configMap))
    }
}