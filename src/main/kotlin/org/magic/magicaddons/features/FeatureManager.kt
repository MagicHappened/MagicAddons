package org.magic.magicaddons.features

import com.google.gson.JsonObject
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.features.account.ServerConnection
import org.magic.magicaddons.features.combat.HighlightMobs
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.features.debug.MobHitDebugInfo
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.foraging.safarihelper.SafariHelper
import org.magic.magicaddons.features.kuudra.CustomRendSound
import org.magic.magicaddons.features.mining.HidePowderCoatingParticles
import org.magic.magicaddons.features.mining.PickaxeAbilityCooldown
import org.magic.magicaddons.features.mining.XpOrbHider
import org.magic.magicaddons.features.misc.HighlightMarkers
import org.magic.magicaddons.features.misc.SmolPeople

object FeatureManager {
    val features = listOf(
        HidePowderCoatingParticles,
        PickaxeAbilityCooldown,
        XpOrbHider,
        GreenhousePresets,
        HighlightMobs,
        SafariHelper,
        CustomRendSound,
        SmolPeople,
        HighlightMarkers,
        ServerConnection,
        Customization,
        MobHitDebugInfo
    )

    data class Category(val key: String, val name: String, val features: List<Feature>, val isUnrelatedToGame: Boolean)

    private val CATEGORY_ORDER = listOf("farming", "mining", "foraging", "combat", "kuudra")

    private val BELOW_DIVIDER = setOf(ServerConnection.CATEGORY, Customization.CATEGORY, "debug")

    fun categories(): List<Category> = availableFeatures
        .groupBy { it.category }
        .map { (key, list) -> Category(key, key.replaceFirstChar { it.uppercase() }, list, key in BELOW_DIVIDER) }
        .sortedWith(
            compareBy<Category> { it.isUnrelatedToGame }
                .thenBy { CATEGORY_ORDER.indexOf(it.key).let { index -> if (index < 0) CATEGORY_ORDER.size else index } }
                .thenBy { it.key }
        )

    val availableFeatures: List<Feature> get() = features.filter { it.isAvailable }

    fun syncToConfigJson() {
        val settingsByCategory = JsonObject()
        features.forEach { feature ->
            val categoryJson = settingsByCategory.get(feature.category) as? JsonObject
                ?: JsonObject().also { settingsByCategory.add(feature.category, it) }
            categoryJson.add(feature.id, feature.settingsJson())
        }
        MagicAddonsConfigJsonHandler.settingsByCategory = settingsByCategory
    }

    fun syncFromConfigJson() {
        features.forEach { feature ->
            val categoryJson = MagicAddonsConfigJsonHandler.settingsByCategory.get(feature.category) as? JsonObject ?: return@forEach
            val settingsJson = categoryJson.get(feature.id) as? JsonObject ?: return@forEach
            feature.readSettingsJson(settingsJson)
        }
    }

}