package org.magic.magicaddons.features

import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler.configMap
import org.magic.magicaddons.features.combat.HighlightMobs
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.features.debug.MobHitDebugInfo
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.foraging.safarihelper.SafariHelper
import org.magic.magicaddons.features.kuudra.CustomRendSound
import org.magic.magicaddons.features.mining.HidePowderCoatingParticles
import org.magic.magicaddons.features.misc.HighlightMarkers
import org.magic.magicaddons.features.misc.SmolPeople

object FeatureManager {
    // listing the objects here is what makes them initialise
    val features = listOf(
        HidePowderCoatingParticles,
        GreenhousePresets,
        HighlightMobs,
        SafariHelper,
        CustomRendSound,
        SmolPeople,
        HighlightMarkers,
        Customization,
        MobHitDebugInfo
    )

    /** a config category, its key, the name displayed, and a list of features. */
    data class Category(val key: String, val name: String, val features: List<Feature>, val isUnrelatedToGame: Boolean)

    /** hardcoded panel order, ones not listed come after alphabetically */
    private val CATEGORY_ORDER = listOf("farming", "mining", "foraging", "combat", "kuudra")

    /** categories that are unrelated to game features. */
    private val BELOW_DIVIDER = setOf(Customization.CATEGORY, "debug")

    fun categories(): List<Category> = features
        .groupBy { it.category }
        .map { (key, list) -> Category(key, key.replaceFirstChar { it.uppercase() }, list, key in BELOW_DIVIDER) }
        .sortedWith(
            compareBy<Category> { it.isUnrelatedToGame }
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