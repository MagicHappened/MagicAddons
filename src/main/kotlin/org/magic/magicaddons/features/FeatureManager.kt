package org.magic.magicaddons.features

import org.magic.magicaddons.features.api.SlotRenderable

object FeatureManager {
    val features = mutableListOf<Feature>()

    fun register(feature: Feature) {
        features += feature
    }
    fun onTick(){
        features.forEach { it.onTick() }
    }
    fun onRender() {
        features.forEach { it.onRender() }
    }
    fun getFeaturesByCategory(category: String) =
        features.filter { it.category == category }
}