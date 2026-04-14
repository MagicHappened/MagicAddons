package org.magic.magicaddons.config

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

class ClickableButtonWidget(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    private val message: Text,
    private val onClick: () -> Unit
) {

    private var hovered = false

    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        hovered = isHovered(mouseX, mouseY)

        val bgColor = if (hovered) 0xFF555555.toInt() else 0xFF333333.toInt()
        context.fill(x, y, x + width, y + height, bgColor)

        context.fill(x, y, x + width, y + 1, 0xFF000000.toInt())
        context.fill(x, y + height - 1, x + width, y + height, 0xFF000000.toInt())

        // centered text
        val textX = x + width / 2
        val textY = y + (height - 8) / 2

        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            message,
            textX,
            textY,
            0xFFFFFF
        )
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (isHovered(mouseX.toInt(), mouseY.toInt())) {
            onClick()
            return true
        }
        return false
    }

    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in x until (x + width) &&
                mouseY in y until (y + height)
    }
}