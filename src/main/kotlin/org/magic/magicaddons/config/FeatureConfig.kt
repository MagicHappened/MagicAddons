package org.magic.magicaddons.config


data class FeatureConfig(
    var enabled: Boolean = false,
    var extra: MutableMap<String, Any> = mutableMapOf(),
)
