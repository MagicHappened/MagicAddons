package org.magic.magicaddons.features

object FeatureManager {
    val features = mutableListOf<Feature>()

    fun register(feature: Feature) {
        features += feature
    }

}