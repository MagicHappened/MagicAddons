package org.magic.magicaddons.features


import net.minecraft.text.Text
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler.configMap
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.data.SettingNode

abstract class Feature {

    abstract val id: String
    abstract val displayName: String
    abstract val tooltipMessage: String
    abstract val category: String
    open val baseSetting: BooleanSetting = BooleanSetting(
    key = "enabled",
    displayName = displayName,
    tooltip = tooltipMessage,
    value = false
    )

    init {
        FeatureManager.register(this)
    }

    fun serializeSettings(): MutableMap<String, String> = baseSetting.serializeSettings()

    fun deserializeSettings(settings: MutableMap<String, String>) {
        baseSetting.updateSettings(settings)
    }
}
