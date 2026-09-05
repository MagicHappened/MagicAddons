package org.magic.magicaddons.features

import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler.configMap
import org.magic.magicaddons.features.combat.HighlightMobs
import org.magic.magicaddons.features.debug.MobHitDebugInfo
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.foraging.safarihelper.SafariHelper
import org.magic.magicaddons.features.kuudra.CustomRendSound
import org.magic.magicaddons.features.mining.HidePowderCoatingParticles

object FeatureManager {
    val features = mutableListOf(
        HidePowderCoatingParticles,
        GreenhousePresets,
        HighlightMobs,
        SafariHelper,
        CustomRendSound,
        MobHitDebugInfo
    ) // need to call objects somehow for initialization

    /** A category of the config screen: its key, the name shown, and its features. */
    data class Category(val key: String, val name: String, val features: List<Feature>, val dev: Boolean)

    /** The side panel's order; a category not listed here comes after these, alphabetically. */
    private val CATEGORY_ORDER = listOf("farming", "mining", "foraging", "combat", "kuudra")

    /** Categories shown under the thick divider, for developers rather than players. */
    private val DEV_CATEGORIES = setOf("debug")

    fun categories(): List<Category> = features
        .groupBy { it.category }
        .map { (key, list) -> Category(key, key.replaceFirstChar { it.uppercase() }, list, key in DEV_CATEGORIES) }
        .sortedWith(
            compareBy<Category> { it.dev }
                .thenBy { CATEGORY_ORDER.indexOf(it.key).let { index -> if (index < 0) CATEGORY_ORDER.size else index } }
                .thenBy { it.key }
        )


    fun syncToConfigJson() {

        val returnedMap = mutableMapOf<
                String, //category string
                MutableMap<String, //feature id string
                        MutableMap<String, Any>>>() // feature setting id, value
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

    fun syncFromConfigJson() {
        features.forEach { feature ->
            val categoryMap = configMap[feature.category] ?: return@forEach
            val settingsMap = categoryMap[feature.id] ?: return@forEach
            feature.deserializeSettings(settingsMap)
        }
    }


}