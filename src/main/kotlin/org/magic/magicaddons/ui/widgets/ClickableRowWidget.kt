package org.magic.magicaddons.ui.widgets

import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.resources.Identifier

open class ClickableRowWidget<T>(
    value: T,
    val onClick: (ClickableRowWidget<T>) -> Unit,
) : BaseRowWidget<T>(value) {
    val BUTTON_HOVERED = Identifier.fromNamespaceAndPath("minecraft", "widget/button_highlighted")

    override fun getSprite(): Identifier {
        return if (hovered) BUTTON_HOVERED else super.getSprite()
    }

    open fun mouseClicked(mouseButtonEvent: MouseButtonEvent, double: Boolean): Boolean {
        if (super.isMouseOverRow(mouseButtonEvent.x,mouseButtonEvent.y)){
            onClick.invoke(this)
            return true
        }
        return false
    }
}