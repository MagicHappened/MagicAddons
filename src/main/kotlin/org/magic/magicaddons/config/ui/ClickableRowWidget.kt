package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

open class ClickableRowWidget<T>(
    value: T,
    displayText: (T) -> String,
    val onClick: (ClickableRowWidget<T>) -> Unit,
    val onRemove: ((ClickableRowWidget<T>) -> Unit)? = null
) : BaseRowWidget<T>(value, displayText) {

    private val removeWidth = 18

    override fun getRightReservedWidth(): Int {
        return if (onRemove != null) removeWidth else 0
    }

    override fun render(ctx: DrawContext) {
        super.render(ctx)

        if (onRemove != null) {
            val rx = x + width - removeWidth

            ctx.fill(rx, y, x + width, y + height, 0xFF333333.toInt())

            val tr = MinecraftClient.getInstance().textRenderer
            ctx.drawText(
                tr,
                Text.literal("X"),
                rx + 6,
                y + (height - tr.fontHeight) / 2,
                0xFFFF5555.toInt(),
                false
            )
        }
    }

    open fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val mx = click.x.toInt()
        val my = click.y.toInt()

        if (mx !in x..x + width || my !in y..y + height) return false

        if (onRemove != null) {
            val rx = x + width - removeWidth
            if (mx in rx..x + width) {
                onRemove.invoke(this)
                return true
            }
        }

        onClick(this)
        return true
    }
}