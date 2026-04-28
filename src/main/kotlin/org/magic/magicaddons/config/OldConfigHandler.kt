package org.magic.magicaddons.config

object OldConfigHandler {

    private const val INFO_KEY = "info"
    private const val VERSION_KEY = "version"
    private const val CONFIG_KEY = "config"

    fun updateConfig(
        raw: MutableMap<String, Any>,
        targetVersion: String
    ): MutableMap<String, Any> {

        val version = extractVersion(raw)

        if (version == null) {
            return handleNoVersion(raw, targetVersion)
        }

        return migrate(raw, version, targetVersion)
    }


    private fun handleNoVersion(
        oldConfig: MutableMap<String, Any>,
        targetVersion: String
    ): MutableMap<String, Any> {

        return mutableMapOf(
            INFO_KEY to mapOf(VERSION_KEY to targetVersion),
            CONFIG_KEY to oldConfig
        )
    }


    private fun migrate(
        raw: MutableMap<String, Any>,
        oldVersion: String,
        targetVersion: String
    ): MutableMap<String, Any> {

        var updated = raw

        if (oldVersion == "1.0.0") {
            updated = update_to_1_0_1(updated)
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



}