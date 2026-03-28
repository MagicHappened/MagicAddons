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
    var enabled: Boolean = false
    open val baseSetting: BooleanSetting = BooleanSetting(
    key = id,
    displayName = displayName,
    tooltip = tooltipMessage,
    value = configMap.getValue(category).getValue(id).enabled
    )

    init {
        FeatureManager.register(this)
    }
}
