package org.magic.magicaddons.ui.widgets

import net.minecraft.client.input.MouseButtonEvent

open class ClickableRowWidget<T>(
    value: T,
    val onClick: ((ClickableRowWidget<T>) -> Unit)? = null,
) : BaseRowWidget<T>(value) {

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, double: Boolean): Boolean {
        if (super.isMouseOverRow(mouseButtonEvent.x, mouseButtonEvent.y)) {
            onClick?.invoke(this)
            return true
        }
        return false
    }
}
