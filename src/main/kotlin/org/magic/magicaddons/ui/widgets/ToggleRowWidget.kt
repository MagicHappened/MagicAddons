package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent

open class ToggleRowWidget<T>(
    value: T,
    val isEnabled: () -> Boolean,
    val onToggle: (Boolean) -> Unit
) : RowWidget<T>(value) {

    private val enabledSwitch = SwitchWidget(isEnabled(), SWITCH_WIDTH, SWITCH_HEIGHT)

    override fun rightReservedWidth(): Int = enabledSwitch.width + SWITCH_PADDING * 2

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        enabledSwitch.set(isEnabled())
        enabledSwitch.x = x + width - enabledSwitch.width - SWITCH_PADDING
        enabledSwitch.y = y + (height - enabledSwitch.height) / 2

        super.extractRenderState(graphics, mouseX, mouseY)
        enabledSwitch.render(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        val isEnabledNow = !isEnabled()
        enabledSwitch.set(isEnabledNow)
        onToggle(isEnabledNow)
        return true
    }

    private companion object {
        const val SWITCH_WIDTH: Int = 18
        const val SWITCH_HEIGHT: Int = 10
        const val SWITCH_PADDING: Int = 3
    }
}
