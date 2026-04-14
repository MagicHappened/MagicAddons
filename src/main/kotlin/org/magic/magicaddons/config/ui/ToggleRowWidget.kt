package org.magic.magicaddons.config.ui

import net.minecraft.client.gui.DrawContext

open class ToggleRowWidget<T>(
    value: T,
    displayText: (T) -> String,
    onClick: (ToggleRowWidget<T>) -> Unit,
    onRemove: ((ToggleRowWidget<T>) -> Unit)? = null,
    val isEnabled: () -> Boolean
) : ClickableRowWidget<T>(
    value,
    displayText,
    { onClick(it as ToggleRowWidget<T>) },
    { onRemove?.invoke(it as ToggleRowWidget<T>) } ) {

    private val toggleWidth = 4

    override fun render(ctx: DrawContext) {
        super.render(ctx)

        val color = if (isEnabled()) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()

        ctx.fill(x, y, x + toggleWidth, y + height, color)
    }
}