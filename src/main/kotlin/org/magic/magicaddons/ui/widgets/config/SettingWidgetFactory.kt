package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.config.data.*

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