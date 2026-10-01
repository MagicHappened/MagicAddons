package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.SwitchWidget

class BooleanSettingWidget(
    private val setting: BooleanSetting,
    overlays: OverlayContext
) : SettingWidget<Boolean>(setting, overlays) {

    private val valueSwitch = SwitchWidget(setting.value)

    override val controlWidth: Int = SwitchWidget.WIDTH
    override val controlHeight: Int = SwitchWidget.HEIGHT

    override fun layoutControl() {
        valueSwitch.x = controlLeft()
        valueSwitch.y = controlTop()
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        valueSwitch.set(setting.value)
        valueSwitch.render(graphics)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() != 0 || !isMouseOver(event.x, event.y)) return false
        setting.value = !setting.value
        valueSwitch.set(setting.value)
        setting.valueChanged?.invoke(setting.value)
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        valueSwitch.mouseMoved(mouseX, mouseY)
    }
}
