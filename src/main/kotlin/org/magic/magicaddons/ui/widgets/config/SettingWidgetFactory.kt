package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.data.config.ToggleListSetting

object SettingWidgetFactory {

    fun create(node: SettingNode<*>): SettingWidget<*> {
        return when (node) {
            is BooleanSetting -> BooleanSettingWidget(node)
            is TextSetting -> TextSettingWidget(node)
            is EnumSetting<*> -> EnumSettingWidget(node)
            is ToggleListSetting -> TextListSettingWidget(node)
        }
    }
}