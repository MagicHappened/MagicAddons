package org.magic.magicaddons.config

import com.google.gson.JsonObject
import org.magic.magicaddons.features.FeatureManager


object OldConfigHandler {

    private const val INFO_KEY = "info"
    private const val VERSION_KEY = "version"
    private const val CONFIG_KEY = "config"

    fun updateConfig(
        raw: JsonObject,
        targetVersion: String
    ): JsonObject {

        val version = extractVersion(raw) ?: return handleNoVersion(raw, targetVersion)

        return migrateVersion(raw, version, targetVersion)
    }


    private fun handleNoVersion(
        oldConfig: JsonObject,
        targetVersion: String
    ): JsonObject {

        val wrapped = JsonObject().apply {
            add(INFO_KEY, JsonObject().apply { addProperty(VERSION_KEY, "1.0.0") })
            add(CONFIG_KEY, oldConfig)
        }

        return migrateVersion(wrapped, "1.0.0", targetVersion)
    }


    private fun migrateVersion(
        raw: JsonObject,
        oldVersion: String,
        targetVersion: String
    ): JsonObject {

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

        if (version == "1.0.3" || version == "1.0.4") {
            updated = update_to_1_0_5(updated)
            version = "1.0.5"
        }

        updated.add(INFO_KEY, JsonObject().apply { addProperty(VERSION_KEY, targetVersion) })

        return updated
    }

    private fun extractVersion(raw: JsonObject): String? =
        (raw.get(INFO_KEY) as? JsonObject)?.get(VERSION_KEY)?.asString

    // change 1_0_1 -> 1_0_2 the safari mob preset and the safari restricted treasure highlight
    // moved to foraging/SafariHelper "Mob Highlight"
    fun update_to_1_0_2(raw: JsonObject): JsonObject {
        val configMap = raw.get(CONFIG_KEY) as? JsonObject ?: return raw
        val combat = configMap.get("combat") as? JsonObject ?: return raw
        val highlightMobs = combat.get("HighlightMobs") as? JsonObject ?: return raw

        val usedSafariPreset = highlightMobs.remove("SafariPreset")?.let { it.isJsonPrimitive && it.asBoolean } == true
        val usedSafariTreasure = highlightMobs.remove("ForagingTreasureSafariCondition")?.let { it.isJsonPrimitive && it.asBoolean } == true

        if (!usedSafariPreset && !usedSafariTreasure) return raw

        val foraging = configMap.get("foraging") as? JsonObject ?: JsonObject().also { configMap.add("foraging", it) }

        foraging.add("SafariHelper", JsonObject().apply {
            addProperty("enabled", true)
            addProperty("MobHighlight", true)
        })

        return raw
    }

    // change 1_0_4 -> 1_0_5 the greenhouse presets "Warnings.DiscordIntegration" key moved out from
    // under Warnings to "DiscordIntegration"
    fun update_to_1_0_5(raw: JsonObject): JsonObject {
        val configMap = raw.get(CONFIG_KEY) as? JsonObject ?: return raw
        val farming = configMap.get("farming") as? JsonObject ?: return raw
        val greenhousePresets = farming.get("GreenhousePresets") as? JsonObject ?: return raw

        val discordIntegration = greenhousePresets.remove("Warnings.DiscordIntegration") ?: return raw
        greenhousePresets.add("DiscordIntegration", discordIntegration)

        return raw
    }

    // change 1_0_2 -> 1_0_3 settings are stored under their nested path "Parent.Child"
    fun update_to_1_0_3(raw: JsonObject): JsonObject {
        val configMap = raw.get(CONFIG_KEY) as? JsonObject ?: return raw

        FeatureManager.features.forEach { feature ->
            val category = configMap.get(feature.category) as? JsonObject ?: return@forEach
            val stored = category.get(feature.id) as? JsonObject ?: return@forEach

            feature.settingPaths().forEach { (key, path) ->
                if (key == path) return@forEach

                val storedValue = stored.remove(key) ?: return@forEach
                stored.add(path, storedValue)
            }
        }

        return raw
    }
}
