package org.magic.magicaddons.features

import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler.configMap
import org.magic.magicaddons.features.combat.HighlightMobs
import org.magic.magicaddons.features.debug.MobHitSkin
import org.magic.magicaddons.features.mining.HidePowderCoatingParticles

object FeatureManager {
    val features = mutableListOf(
        HidePowderCoatingParticles,
        HighlightMobs,
        MobHitSkin
    ) // need to call objects somehow for initialization


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
            val categoryMap = configMap[feature.category] ?: return@forEach
            val settingsMap = categoryMap[feature.id] ?: return@forEach
            feature.deserializeSettings(settingsMap.toMutableMap())
        }
    }


}