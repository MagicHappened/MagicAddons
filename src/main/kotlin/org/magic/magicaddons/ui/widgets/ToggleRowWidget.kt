package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent

/** A row with a small switch at its right edge. Clicking anywhere on the row flips it. */
open class ToggleRowWidget<T>(
    value: T,
    val isEnabled: () -> Boolean,
    val onToggle: (Boolean) -> Unit
) : ClickableRowWidget<T>(value) {

    private val switch = SwitchWidget(isEnabled(), SWITCH_WIDTH, SWITCH_HEIGHT)

    /** Keeps the switch off the row's own edge on every side. */
    private val padding = 3

    override fun getRightReservedWidth(): Int = switch.width + padding * 2

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        switch.set(isEnabled())
        switch.x = x + width - switch.width - padding
        switch.y = y + (height - switch.height) / 2

        super.extractRenderState(graphics, mouseX, mouseY)
        switch.render(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, double: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        val flipped = !isEnabled()
        switch.set(flipped)
        onToggle(flipped)
        return true
    }

    private companion object {
        /** A step smaller than the switch on a setting row. */
        const val SWITCH_WIDTH: Int = 18
        const val SWITCH_HEIGHT: Int = 10
    }
}
