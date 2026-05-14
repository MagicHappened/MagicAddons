package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.resources.Identifier
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.widgets.RemovableRowWidget

open class ToggleRowWidget<T>(
    value: T,
    onClick: (ToggleRowWidget<T>) -> Unit,
    onRemove: ((ToggleRowWidget<T>) -> Unit)? = null,
    val isEnabled: () -> Boolean,
    val onToggle: (Boolean) -> Unit
) : RemovableRowWidget<T>(
    value,
    { onClick(it as ToggleRowWidget<T>) },
    { onRemove?.invoke(it as ToggleRowWidget<T>) }
) {

    private val checkbox = CheckboxWidget(checked = isEnabled())

    private val padding = 1

    override fun getLeftReservedWidth(): Int {
        return height + padding
    }
    override fun getSprite(): Identifier {
        return if (isFocused) BUTTON_HOVERED else super.getSprite()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {

        checkbox.size = height - padding
        checkbox.x = x + padding
        checkbox.y = y + padding

        super.render(graphics, mouseX, mouseY)
        checkbox.render(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, double: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x,mouseButtonEvent.y)) return false
        if (isMouseOverRow(mouseButtonEvent.x,mouseButtonEvent.y) ||
            isMouseOverCheckbox(mouseButtonEvent.x,mouseButtonEvent.y)) {
            checkbox.checked = !checkbox.checked
            onToggle(checkbox.checked)
            return true
        }

        return super.mouseClicked(mouseButtonEvent, double)
    }

    fun isMouseOverCheckbox(mouseX: Double, mouseY: Double): Boolean {
        return checkbox.isMouseOver(mouseX,mouseY)
    }

}