package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.SwitchWidget

/** An on/off setting: a switch on the right of the row. */
class BooleanSettingWidget(
    private val setting: BooleanSetting,
    overlays: OverlayContext
) : SettingWidget<Boolean>(setting, overlays) {

    private val switch = SwitchWidget(setting.value)

    override val controlWidth: Int = SwitchWidget.WIDTH
    override val controlHeight: Int = SwitchWidget.HEIGHT

    override fun layoutControl() {
        switch.x = controlLeft()
        switch.y = controlTop()
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        switch.set(setting.value)
        switch.render(graphics)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() != 0 || !switch.isMouseOver(event.x, event.y)) return false
        setting.value = !setting.value
        switch.set(setting.value)
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        switch.mouseMoved(mouseX, mouseY)
    }
}
