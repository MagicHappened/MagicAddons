package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent

/** A row with a checkbox at its right edge. Clicking anywhere on the row flips it. */
open class ToggleRowWidget<T>(
    value: T,
    val isEnabled: () -> Boolean,
    val onToggle: (Boolean) -> Unit
) : ClickableRowWidget<T>(value) {

    private val checkbox = CheckboxWidget(checked = isEnabled())

    /** Keeps the box off the row's own frame on every side. */
    private val padding = 3

    /** The box stays this size however tall the wrapped text makes the row. */
    private val boxSize = 14

    override fun getRightReservedWidth(): Int = boxSize + padding * 2

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        checkbox.checked = isEnabled()
        checkbox.size = boxSize
        checkbox.x = x + width - boxSize - padding
        checkbox.y = y + (height - boxSize) / 2

        super.extractRenderState(graphics, mouseX, mouseY)
        checkbox.render(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, double: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        val flipped = !isEnabled()
        checkbox.checked = flipped
        onToggle(flipped)
        return true
    }
}
