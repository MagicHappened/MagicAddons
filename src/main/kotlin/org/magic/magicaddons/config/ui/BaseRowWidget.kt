package org.magic.magicaddons.config.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

open class BaseRowWidget<T>(
    val value: T,
    val displayText: (T) -> String
) {

    var width: Int = 200
    var height: Int = 20

    var x: Int = 0
    var y: Int = 0

    open val textLeftPadding = 2

    open fun getRightReservedWidth(): Int = 0

    open fun getLeftReservedWidth(): Int = 0


    open fun render(graphics: GuiGraphics) {
        val font = Minecraft.getInstance().font

        graphics.fill(x, y, x + width, y + height, 0xFF555555.toInt())

        val usableWidth = width - getRightReservedWidth() - getLeftReservedWidth()

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
}