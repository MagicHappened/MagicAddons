package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.data.config.ActionSetting
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.ChoiceSetting
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.data.config.ParentSetting
import org.magic.magicaddons.data.config.PresetLibrarySetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.ui.OverlayContext

object SettingWidgetFactory {

    fun create(node: SettingNode<*>, overlays: OverlayContext): SettingWidget<*> {
        return when (node) {
            is BooleanSetting -> BooleanSettingWidget(node, overlays)
            is TextSetting -> TextSettingWidget(node, overlays)
            is IntSetting -> IntSettingWidget(node, overlays)
            is EnumSetting<*> -> EnumSettingWidget(node, overlays)
            is ToggleListSetting -> ChoiceListSettingWidget(node, overlays)
            is ActionSetting -> ActionSettingWidget(node, overlays)
            is ChoiceSetting -> ChoiceSettingWidget(node, overlays)
            is ParentSetting -> ParentSettingWidget(node, overlays)
            is PresetLibrarySetting -> PresetLibraryWidget(node, overlays)
        }
    }
}
