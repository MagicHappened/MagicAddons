package org.magic.magicaddons.features

abstract class Feature {
    init {
        FeatureManager.register(this)
    }
    abstract val id: String
    abstract val displayName: String
    abstract val tooltipMessage: String
    abstract val category: String
    // TODO implement the feature settings widget
    abstract var enabled: Boolean

}
