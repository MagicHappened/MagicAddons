package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.ParentSetting
import org.magic.magicaddons.ui.OverlayContext

/** A heading with settings under it: the same row as any other, with nothing on its right. */
class ParentSettingWidget(
    setting: ParentSetting,
    overlays: OverlayContext
) : SettingWidget<Unit>(setting, overlays) {

    override val controlWidth: Int = 0
    override val controlHeight: Int = 0

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = Unit

    /** Nothing to set, so a click on the row only opens and shuts the group under it. */
    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean = false
}
