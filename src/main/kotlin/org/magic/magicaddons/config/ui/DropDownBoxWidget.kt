package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.text.Text
import org.magic.magicaddons.util.ScreenUtil

class DropDownBoxWidget<T>(
    val value: T,
    val displayText: (T) -> String,
    val onClick: (DropDownBoxWidget<T>) -> Unit,
    val removable: Boolean = false,
    val onRemove: ((DropDownBoxWidget<T>) -> Unit)? = null
) : Element {

    var width: Int = 200
    var height: Int = 40

    var x: Int = 0
    var y: Int = 0

    private val removeButtonWidth = 20

    val bgColor = 0xFF555555.toInt()
    val borderColor = 0xFF000000.toInt()

    private var isFocused = false

    fun render(ctx: DrawContext) {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        ctx.fill(x, y, x + width, y + height, bgColor)

        if (removable) {
            val rx1 = x + width - removeButtonWidth
            val rx2 = x + width

            ctx.fill(rx1, y, rx2, y + height, 0xFF444444.toInt())

            val centerX = rx1 + removeButtonWidth / 2
            val centerY = y + height / 2

            ctx.drawText(
                textRenderer,
                Text.literal("X"),
                centerX - textRenderer.getWidth("X") / 2,
                centerY - textRenderer.fontHeight / 2,
                0xFFFF5555.toInt(),
                false
            )
        }

        val maxTextWidth = if (removable) {
            width - removeButtonWidth - 15
        } else {
            width - 15
        }

        val rawText = displayText(value)
        val trimmedText = textRenderer.trimToWidth(rawText, maxTextWidth)

        ctx.drawText(
            textRenderer,
            Text.literal(trimmedText),
            x + 10,
            y + (height - textRenderer.fontHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, 1, borderColor)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val mx = click.x.toInt()
        val my = click.y.toInt()

        if (mx in x..(x + width) && my in y..(y + height)) {
            if (removable) {
                val rx1 = x + width - removeButtonWidth
                val rx2 = x + width

                if (mx in rx1..rx2) {
                    onRemove?.invoke(this)
                    return true
                }
            }
            onClick(this)
            return true
        }

        return false
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}