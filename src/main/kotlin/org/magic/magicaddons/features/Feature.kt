package org.magic.magicaddons.features


import net.minecraft.text.Text
import org.magic.magicaddons.config.data.SettingNode

abstract class Feature {

    abstract val id: String
    abstract val displayName: String
    abstract val tooltipMessage: String
    abstract val category: String
    var enabled: Boolean = false


    init {
        FeatureManager.register(this)
    }
    open fun getSettings(): List<SettingNode> = emptyList()
}
