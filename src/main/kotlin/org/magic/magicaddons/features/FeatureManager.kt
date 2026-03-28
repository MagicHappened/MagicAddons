package org.magic.magicaddons.features

import org.magic.magicaddons.config.data.CategoryMap
import org.magic.magicaddons.config.data.FeatureData

object FeatureManager {
    val features = mutableListOf<Feature>()

    fun register(feature: Feature) {
        features += feature
    }


    fun syncToConfig(features: List<Feature>, categories: CategoryMap) {
        // Group features by their category
        features.groupBy { it.category }.forEach { (category, featureList) ->
            // Get or create the category map
            val catMap = categories.getOrPut(category) { mutableMapOf() }

            featureList.forEach { feature ->
                // Get or create the feature data
                val data = catMap.getOrPut(feature.id) { FeatureData() }
                data.enabled = feature.enabled
                data.settings = feature.serializeSettings().toMutableMap()
            }
        }
    }

    fun syncFromConfig(features: List<Feature>, categories: CategoryMap) {
        features.forEach { feature ->
            val data = categories[feature.category]?.get(feature.id)
            if (data != null) {
                feature.enabled = data.enabled
                feature.deserializeSettings(data.settings)
            }
        }
    }


}