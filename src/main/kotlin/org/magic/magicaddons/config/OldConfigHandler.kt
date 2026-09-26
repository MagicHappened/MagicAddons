package org.magic.magicaddons.config

import org.magic.magicaddons.features.FeatureManager


@Suppress("UNCHECKED_CAST")
object OldConfigHandler {

    private const val INFO_KEY = "info"
    private const val VERSION_KEY = "version"
    private const val CONFIG_KEY = "config"

    fun updateConfig(
        raw: MutableMap<String, Any>,
        targetVersion: String
    ): MutableMap<String, Any> {

        val version = extractVersion(raw) ?: return handleNoVersion(raw, targetVersion)

        return migrateVersion(raw, version, targetVersion)
    }


    private fun handleNoVersion(
        oldConfig: MutableMap<String, Any>,
        targetVersion: String
    ): MutableMap<String, Any> {

        val wrapped = mutableMapOf<String, Any>(
            INFO_KEY to mutableMapOf<String, Any>(VERSION_KEY to "1.0.0"),
            CONFIG_KEY to oldConfig
        )

        return migrateVersion(wrapped, "1.0.0", targetVersion)
    }


    private fun migrateVersion(
        raw: MutableMap<String, Any>,
        oldVersion: String,
        targetVersion: String
    ): MutableMap<String, Any> {

        var updated = raw
        var version = oldVersion

        if (version == "1.0.0" || version == "1.0.1") {
            updated = update_to_1_0_2(updated)
            version = "1.0.2"
        }

        if (version == "1.0.2") {
            updated = update_to_1_0_3(updated)
            version = "1.0.3"
        }

        val info = mutableMapOf<String, Any>(
            VERSION_KEY to targetVersion
        )

        updated[INFO_KEY] = info

        return updated
    }

    private fun extractVersion(raw: Map<String, Any>): String? {
        val info = raw[INFO_KEY] as? Map<*, *> ?: return null
        return info[VERSION_KEY] as? String
    }

    // change 1_0_1 -> 1_0_2 the safari mob preset and the safari restricted treasure highlight
    // moved to foraging/SafariHelper "Mob Highlight"
    fun update_to_1_0_2(raw: MutableMap<String, Any>): MutableMap<String, Any> {
        val configMap = raw[CONFIG_KEY] as? MutableMap<String, Any> ?: return raw
        val combat = configMap["combat"] as? MutableMap<String, Any> ?: return raw
        val highlightMobs = combat["HighlightMobs"] as? MutableMap<String, Any> ?: return raw

        val usedSafariPreset = highlightMobs.remove("SafariPreset") == true
        val usedSafariTreasure = highlightMobs.remove("ForagingTreasureSafariCondition") == true

        if (!usedSafariPreset && !usedSafariTreasure) return raw

        val foraging = configMap.getOrPut("foraging") { mutableMapOf<String, Any>() }
                as? MutableMap<String, Any> ?: return raw

        foraging["SafariHelper"] = mutableMapOf<String, Any>(
            "enabled" to true,
            "MobHighlight" to true
        )

        return raw
    }

    // change 1_0_2 -> 1_0_3 settings are stored under their nested path "Parent.Child"
    fun update_to_1_0_3(raw: MutableMap<String, Any>): MutableMap<String, Any> {
        val configMap = raw[CONFIG_KEY] as? MutableMap<String, Any> ?: return raw

        FeatureManager.features.forEach { feature ->
            val category = configMap[feature.category] as? MutableMap<String, Any> ?: return@forEach
            val stored = category[feature.id] as? MutableMap<String, Any> ?: return@forEach

            feature.settingPaths().forEach { (key, path) ->
                if (key == path) return@forEach

                val storedValue = stored.remove(key) ?: return@forEach
                stored[path] = storedValue
            }
        }

        return raw
    }
}
