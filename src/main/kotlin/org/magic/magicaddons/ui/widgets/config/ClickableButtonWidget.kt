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
    val renderContent: ClickableButtonWidget.(GuiGraphics) -> Unit
) {

    constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component
    ) : this(
        x,
        y,
        width,
        height,
        { graphics ->
            val font = Minecraft.getInstance().font

            graphics.drawString(
                font,
                message,
                x + (width - font.width(message)) / 2,
                y + (height - font.lineHeight) / 2,
                (message.style.color?.value ?: 0xFFFFFFFF.toInt()) or 0xFF000000.toInt(),
                false
            )
        }
    )

    val BUTTON = Identifier.fromNamespaceAndPath("minecraft", "widget/button")
    val BUTTON_HOVERED = Identifier.fromNamespaceAndPath("minecraft", "widget/button_highlighted")

    var hovered = false

    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {

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

        renderContent(graphics)
    }

    fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        return isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in x..(x + width) &&
                mouseY.toInt() in y..(y + height)
    }
    fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)
    }
}