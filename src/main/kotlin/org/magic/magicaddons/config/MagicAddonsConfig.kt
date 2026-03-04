package org.magic.magicaddons.config

import org.magic.magicaddons.features.FeatureManager

object MagicAddonsConfig {

    val categories = mutableMapOf<String, MutableMap<String, FeatureConfig>>()

    fun save() {
        categories.clear() // reset


        FeatureManager.features.forEach { feature ->
            val cat = categories.getOrPut(feature.category) { mutableMapOf() }

            cat[feature.id] = FeatureConfig(
                enabled = feature.enabled,
                extra = mutableMapOf() // TODO: handle extra if needed later
            )
        }

        // TODO: serialize `categories` to disk as JSON
    }
}