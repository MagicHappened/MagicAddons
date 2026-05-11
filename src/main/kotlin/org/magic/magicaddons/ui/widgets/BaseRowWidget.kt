package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

open class BaseRowWidget<T>(
    val value: T
) {

    val BUTTON = Identifier.fromNamespaceAndPath("minecraft", "widget/button")


    var hovered = false
    var width: Int = 200
    var height: Int = 20

    var x: Int = 0
    var y: Int = 0

    open val textLeftPadding = 4

    open fun getRightReservedWidth(): Int = 0

    open fun getLeftReservedWidth(): Int = 0

    protected open fun getSprite(): Identifier {
        return BUTTON
    }

    open fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font
        val usableWidth = width - getRightReservedWidth() - getLeftReservedWidth()
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            getSprite(),
            x + getLeftReservedWidth(),
            y,
            usableWidth,
            height
        )

        val text = font.plainSubstrByWidth(value.toString(), usableWidth)

        graphics.drawString(
            font,
            Component.literal(text),
            x + textLeftPadding + getLeftReservedWidth(),
            y + (height - font.lineHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

    }
    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in x..x+width && mouseY.toInt() in y..y+height)
    }

    open fun isMouseOverRow(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in x+getLeftReservedWidth()..x+width-getRightReservedWidth() && mouseY.toInt() in y..y+height)
    }

    open fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOverRow(mouseX, mouseY)
    }


}