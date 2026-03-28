package org.magic.magicaddons.config.ui

import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.data.SettingNode
import org.magic.magicaddons.config.data.TextSetting

object SettingWidgetFactory {

    fun create(node: SettingNode): SettingWidget<*> {
        return when (node) {
            is BooleanSetting -> BooleanSettingWidget(node)
            is TextSetting -> TextSettingWidget(node)
            is EnumSetting<*> -> EnumSettingWidget(node)
        }
    }
}