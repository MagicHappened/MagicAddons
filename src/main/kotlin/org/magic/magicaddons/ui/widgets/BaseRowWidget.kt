package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

open class BaseRowWidget<T>(
    val value: T,
    val displayText: (T) -> String
) {

    val BUTTON = Identifier.fromNamespaceAndPath("minecraft", "widget/button")
    val BUTTON_HOVERED = Identifier.fromNamespaceAndPath("minecraft", "widget/button_highlighted")

    var hovered = false
    var width: Int = 200
    var height: Int = 20

    var x: Int = 0
    var y: Int = 0

    open val textLeftPadding = 2

    open fun getRightReservedWidth(): Int = 0

    open fun getLeftReservedWidth(): Int = 0


    open fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font
        hovered = isMouseOver(mouseX, mouseY)
        val usableWidth = width - getRightReservedWidth() - getLeftReservedWidth()
        val sprite = if (hovered) BUTTON_HOVERED else BUTTON
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            x,
            y,
            width,
            height
        )

        val text = font.plainSubstrByWidth(displayText(value), usableWidth)

        graphics.drawString(
            font,
            Component.literal(text),
            x + textLeftPadding + getLeftReservedWidth(),
            y + (height - font.lineHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

    }

    open fun isMouseOver(mouseX: Int, mouseY: Int): Boolean {
        return (mouseX in x..x+width && mouseY in y..y+height)
    }
}