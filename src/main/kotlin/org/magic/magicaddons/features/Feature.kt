package org.magic.magicaddons.features


import org.magic.magicaddons.config.data.BooleanSetting

abstract class Feature {

    abstract val id: String
    abstract val displayName: String
    abstract val tooltipMessage: String
    abstract val category: String
    abstract val baseSetting: BooleanSetting


    fun serializeSettings(): MutableMap<String, Any> = baseSetting.serializeSettings()

    fun deserializeSettings(settings: MutableMap<String, Any>) {
        baseSetting.updateSettings(settings)
    }
}
