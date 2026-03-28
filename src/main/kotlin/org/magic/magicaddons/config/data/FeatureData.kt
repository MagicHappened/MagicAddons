package org.magic.magicaddons.config.data

data class FeatureData(
    var enabled: Boolean = false,
    var settings: MutableMap<String, String>?
)

typealias CategoryMap = MutableMap<String, MutableMap<String, FeatureData>>