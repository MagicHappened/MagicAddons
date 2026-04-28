package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class ClickableButtonWidget(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    private val message: Component,
) {
    val BUTTON = Identifier.fromNamespaceAndPath("minecraft", "widget/button")
    val BUTTON_HOVERED = Identifier.fromNamespaceAndPath("minecraft", "widget/button_highlighted")

    private var hovered = false

    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val font = Minecraft.getInstance().font
        hovered = isHovered(mouseX, mouseY)

        val sprite = if (hovered)
            BUTTON_HOVERED
        else
            BUTTON

        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            x,
            y,
            width,
            height
        )

        val textX = x + (width - font.width(message)) / 2
        val textY = y + (height - 8) / 2
        val color = (message.style.color?.value ?: 0xFFFFFFFF.toInt()) or 0xFF000000.toInt()

        graphics.drawString(
            font,
            message,
            textX,
            textY,
            color,
            false
        )
    }

    fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        return isHovered(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())
    }

    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in x..(x + width) &&
                mouseY in y..(y + height)
    }
}