package org.magic.magicaddons.config

import org.magic.magicaddons.config.option.Position


data class FeatureConfig(
    var enabled: Boolean = false,
    var extra: MutableMap<String, Any> = mutableMapOf(),
)
