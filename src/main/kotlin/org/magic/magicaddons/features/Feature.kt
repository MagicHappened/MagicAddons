package org.magic.magicaddons.features


import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.SettingNode

abstract class Feature {

    abstract val id: String
    abstract val displayName: String
    abstract val description: String
    abstract val category: String
    abstract val baseSetting: BooleanSetting


    fun serializeSettings(): MutableMap<String, Any> = baseSetting.serializeAsFeatureRoot()

    fun deserializeSettings(settings: Map<String, Any>) {
        baseSetting.updateAsFeatureRoot(settings)
    }

    /**
     * Every setting key of this feature mapped to the path it is stored under. Used by config
     * migrations that have to find a value written under an older key layout.
     */
    fun settingPaths(): Map<String, String> {
        val paths = mutableMapOf<String, String>()

        fun collect(node: SettingNode<*>, parentPath: String) {
            val path = node.pathIn(parentPath)
            paths[node.key] = path
            node.children?.forEach { child -> collect(child, path) }
        }

        baseSetting.children?.forEach { child -> collect(child, "") }
        return paths
    }
}
