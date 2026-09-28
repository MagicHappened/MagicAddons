package org.magic.magicaddons.features


import com.google.gson.JsonObject
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.SettingNode

abstract class Feature {

    abstract val id: String
    abstract val displayName: String
    abstract val description: String
    abstract val category: String
    abstract val baseSetting: BooleanSetting

    val isAvailable: Boolean get() = baseSetting.isAvailable

    fun settingsJson(): JsonObject = JsonObject().also { baseSetting.writeAsFeatureRoot(it) }

    fun readSettingsJson(settingsJson: JsonObject) {
        baseSetting.readAsFeatureRoot(settingsJson)
    }


    fun settingPaths(): Map<String, String> {
        val paths = mutableMapOf<String, String>()

        fun collect(node: SettingNode<*>, parentPath: String) {
            val path = node.settingKey(parentPath)
            paths[node.key] = path
            node.children?.forEach { child -> collect(child, path) }
        }

        baseSetting.children?.forEach { child -> collect(child, "") }
        return paths
    }
}
