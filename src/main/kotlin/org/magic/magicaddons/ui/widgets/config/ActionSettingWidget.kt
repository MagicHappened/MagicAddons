package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.config.ActionSetting
import org.magic.magicaddons.ui.OverlayContext

/** A setting whose row is a button: pressing it runs the action the setting carries. */
class ActionSettingWidget(
    private val setting: ActionSetting,
    overlays: OverlayContext
) : SettingWidget<String>(setting, overlays) {

    private val button = ClickableButtonWidget(
        BUTTON_WIDTH,
        FIELD_HEIGHT,
        Component.literal(setting.buttonLabel)
    )

    override val controlWidth: Int get() = button.width
    override val controlHeight: Int = FIELD_HEIGHT

    override fun layoutControl() {
        button.x = controlLeft()
        button.y = controlTop()
    }

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        button.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!button.mouseClicked(event, doubled)) return false

        setting.onPressed(setting)
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        button.mouseMoved(mouseX, mouseY)
    }

    private companion object {
        const val BUTTON_WIDTH: Int = 80
    }
}
