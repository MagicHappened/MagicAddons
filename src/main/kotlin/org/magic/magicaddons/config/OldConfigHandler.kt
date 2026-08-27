package org.magic.magicaddons.config

import org.magic.magicaddons.features.FeatureManager

object OldConfigHandler {

    private const val INFO_KEY = "info"
    private const val VERSION_KEY = "version"
    private const val CONFIG_KEY = "config"

    fun updateConfig(
        raw: MutableMap<String, Any>,
        targetVersion: String
    ): MutableMap<String, Any> {

        val version = extractVersion(raw) ?: return handleNoVersion(raw, targetVersion)

        return migrate(raw, version, targetVersion)
    }


    private fun handleNoVersion(
        oldConfig: MutableMap<String, Any>,
        targetVersion: String
    ): MutableMap<String, Any> {

        // a file from before versioning existed is a 1.0.0 file, so it still has to walk the chain
        val wrapped = mutableMapOf<String, Any>(
            INFO_KEY to mutableMapOf<String, Any>(VERSION_KEY to "1.0.0"),
            CONFIG_KEY to oldConfig
        )

        return migrate(wrapped, "1.0.0", targetVersion)
    }


    private fun migrate(
        raw: MutableMap<String, Any>,
        oldVersion: String,
        targetVersion: String
    ): MutableMap<String, Any> {

        var updated = raw
        var version = oldVersion

        // each step upgrades one version, so an old config walks the whole chain
        if (version == "1.0.0") {
            updated = update_to_1_0_1(updated)
            version = "1.0.1"
        }

        if (version == "1.0.1") {
            updated = update_to_1_0_2(updated)
            version = "1.0.2"
        }

        if (version == "1.0.2") {
            updated = update_to_1_0_3(updated)
            version = "1.0.3"
        }

        // always update version at the end
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

    // change 1_0_0 -> 1_0_1 EntityTypePlayer or Other no longer enum
    fun update_to_1_0_1(raw: MutableMap<String, Any>): MutableMap<String, Any> {
        val mapToUpdate = raw.toMutableMap()
        val configMap = raw["config"] as? MutableMap<String, Any> ?: return raw
        val combat = configMap["combat"] as? MutableMap<String, Any> ?: return raw
        val highlightMobs = combat["HighlightMobs"] as? MutableMap<String, Any> ?: return raw
        val trueValue = highlightMobs["EntityTypePlayerOtherEnum"]
        when (trueValue) {
            is String -> {
                if (trueValue == "Player"){
                    highlightMobs["EntityTypePlayerEnabled"] = true
                }
                else if (trueValue == "Other"){
                    highlightMobs["EntityTypeOtherEnabled"] = true
                }
            }
            else -> return raw
        }
        highlightMobs.remove("EntityTypePlayerOtherEnum")
        mapToUpdate["config"] = configMap
        return mapToUpdate
    }

    // change 1_0_1 -> 1_0_2 the safari mob preset and the safari restricted treasure highlight
    // left combat/HighlightMobs and became foraging/SafariHelper "Mob Highlight"
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

    // change 1_0_2 -> 1_0_3 settings are stored under their nested path ("Parent.Child") instead of
    // a bare key, so the value of every setting below the top level has to be moved over
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