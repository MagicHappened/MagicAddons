package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.config.ui.ClickableRowWidget
import org.magic.magicaddons.ui.widgets.CheckboxWidget


open class ToggleRowWidget<T>(
    value: T,
    displayText: (T) -> String,
    onClick: (ToggleRowWidget<T>) -> Unit,
    onRemove: ((ToggleRowWidget<T>) -> Unit)? = null,
    val isEnabled: () -> Boolean,
    val onToggle: (Boolean) -> Unit
) : ClickableRowWidget<T>(
    value,
    displayText,
    { onClick(it as ToggleRowWidget<T>) },
    { onRemove?.invoke(it as ToggleRowWidget<T>) }
) {

    private val checkbox = CheckboxWidget()

    override val textLeftPadding: Int = 0

    private val padding = 2

    override fun getLeftReservedWidth(): Int {
        return height + padding
    }

    override fun render(graphics: GuiGraphics) {

        checkbox.size = height - padding * 2
        checkbox.x = x + padding + 1 // idk why but yes
        checkbox.y = y + padding
        checkbox.checked = isEnabled()

        super.render(graphics)

        checkbox.render(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (checkbox.mouseClicked(mouseButtonEvent, doubled)) {
            onToggle(checkbox.checked)
            return true
        }

        return super.mouseClicked(mouseButtonEvent, doubled)
    }
}