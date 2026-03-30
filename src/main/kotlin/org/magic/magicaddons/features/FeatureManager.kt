package org.magic.magicaddons.features

import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler.configMap

object FeatureManager {
    val features = mutableListOf<Feature>()

    fun register(feature: Feature) {
        features += feature
    }


    fun syncToConfig() {

        val returnedMap = mutableMapOf<
                String, //category string
                MutableMap<String, //feature id string
                        MutableMap<String, String>>>() // feature setting id, value
        features.groupBy { it.category }.forEach { (category, featureList) ->

            val currentCategoryMap = returnedMap.getOrPut(category) { mutableMapOf() }
            // iterate over features in the current category
            featureList.forEach { feature ->
                // get settings from serialize function and assign to feature id identifier
                currentCategoryMap[feature.id] = feature.serializeSettings()
            }

        }
        configMap = returnedMap
    }

    fun syncFromConfig() {
        features.forEach { feature ->
            val data = configMap[feature.category]?.get(feature.id)
            if (data != null) {
                feature.enabled = data.enabled
                feature.deserializeSettings(data.settings) }
            }
        }
    }


}