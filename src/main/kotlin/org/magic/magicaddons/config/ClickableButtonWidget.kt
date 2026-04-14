package org.magic.magicaddons.config

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.magic.magicaddons.util.ScreenUtil

class ClickableButtonWidget(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    private val message: Text,
    private val onClick: () -> Unit
) {
    val BUTTON = Identifier.of("minecraft", "widget/button")
    val BUTTON_HOVERED = Identifier.of("minecraft", "widget/button_highlighted")

    private var hovered = false

    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        hovered = isHovered(mouseX, mouseY)

        val sprite = if (hovered)
            BUTTON_HOVERED
        else
            BUTTON


        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            x,
            y,
            width,
            height
        )


        // centered text
        val textX = x + (width - textRenderer.getWidth(message)) / 2
        val textY = y + (height - 8) / 2
        val color = (message.style.color?.rgb ?: 0xFFFFFF) or 0xFF000000.toInt()

        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            message,
            textX,
            textY,
            color,
            false
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