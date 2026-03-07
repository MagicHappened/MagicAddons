package org.magic.magicaddons.ui.components

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

open class ButtonWidget(x: Int, y: Int, width: Int, height: Int, message: Text) : ClickableWidget(x,y,width,height,message) {

    var borderColor: Int = 0xFF000000.toInt()
    open val fillColor: Int = 0xFF800080.toInt()
    var borderWidth: Int = 1

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float
    ) {
        val left = x
        val top = y
        val right = x + width
        val bottom = y + height

        context.fill(left, top, right, bottom, borderColor)

        val innerLeft = left + borderWidth
        val innerTop = top + borderWidth
        val innerRight = right - borderWidth
        val innerBottom = bottom - borderWidth

        if (innerRight > innerLeft && innerBottom > innerTop) {
            context.fill(innerLeft, innerTop, innerRight, innerBottom, fillColor)
        }
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            message,
            x + (width / 2) - (MinecraftClient.getInstance().textRenderer.getWidth(message)/2),
            y + (height / 2) - (MinecraftClient.getInstance().textRenderer.fontHeight/2),
            0xFF000000.toInt(),
            false
        )
    }

    override fun onClick(click: Click?, doubled: Boolean) {
        // click?.button() == 0 left click 1 right click
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        return
    }

}