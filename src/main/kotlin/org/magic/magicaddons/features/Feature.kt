package org.magic.magicaddons.features

import io.github.notenoughupdates.moulconfig.Config

abstract class Feature : Config() {
    init {
        FeatureManager.register(this)
    }
    abstract val id: String
    abstract val displayName: String
    abstract val tooltipMessage: String
    abstract val category: String
    var enabled: Boolean = false
}
