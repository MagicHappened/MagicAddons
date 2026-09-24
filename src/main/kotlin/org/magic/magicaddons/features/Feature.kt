package org.magic.magicaddons.features


import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.SettingNode

abstract class Feature {

    abstract val id: String
    abstract val displayName: String
    abstract val description: String
    abstract val category: String
    abstract val baseSetting: BooleanSetting

    val isAvailable: Boolean get() = baseSetting.isAvailable

    fun serializeSettings(): MutableMap<String, Any> = baseSetting.serializeAsFeatureRoot()

    fun deserializeSettings(settings: Map<String, Any>) {
        baseSetting.updateAsFeatureRoot(settings)
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
