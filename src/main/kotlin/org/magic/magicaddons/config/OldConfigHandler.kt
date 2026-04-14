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

    // =========================
    // NO VERSION → wrap config
    // =========================
    private fun handleNoVersion(
        oldConfig: MutableMap<String, Any>,
        targetVersion: String
    ): MutableMap<String, Any> {

        return mutableMapOf(
            INFO_KEY to mapOf(VERSION_KEY to targetVersion),
            CONFIG_KEY to oldConfig
        )
    }

    // =========================
    // VERSIONED MIGRATIONS
    // =========================
    private fun migrate(
        raw: MutableMap<String, Any>,
        oldVersion: String,
        targetVersion: String
    ): MutableMap<String, Any> {

        var updated = raw

        // if (oldVersion == "1.0.0")
        // updated = migrate_1_0_0_to_1_0_1(updated)
        //

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
}