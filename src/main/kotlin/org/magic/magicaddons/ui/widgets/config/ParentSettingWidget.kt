package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.ParentSetting
import org.magic.magicaddons.ui.OverlayContext

class ParentSettingWidget(
    setting: ParentSetting,
    overlays: OverlayContext
) : SettingWidget<Unit>(setting, overlays) {

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean = false
}
