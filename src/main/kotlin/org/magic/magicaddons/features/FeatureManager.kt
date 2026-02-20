package org.magic.magicaddons.features

import org.magic.magicaddons.features.api.SlotRenderable

object FeatureManager {
    private val slotRenderableFeatures = mutableListOf<SlotRenderable>()
    private val features = mutableListOf<Feature>()

    fun register(feature: Feature) {
        features += feature
        feature.register()
        if (feature is SlotRenderable)
            slotRenderableFeatures += feature

    }
    fun onTick(){
        features.forEach { it.onTick() }
    }
    fun onRender() {
        features.forEach { it.onRender() }
    }
    fun getFeaturesByCategory(category: String) =
        features.filter { it.category == category }
    fun getSlotRenderables() = slotRenderableFeatures;
}