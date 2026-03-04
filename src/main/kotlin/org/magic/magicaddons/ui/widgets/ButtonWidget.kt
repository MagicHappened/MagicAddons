package org.magic.magicaddons.ui.widgets

import com.ibm.icu.number.IntegerWidth
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

open class ButtonWidget(x: Int, y: Int, width: Int, height: Int, message: Text) : ClickableWidget(x,y,width,height,message) {

    var borderColor: Int = 0xFF000000.toInt()
    var fillColor: Int = 0xFF333333.toInt()
    var borderWidth: Int = 2

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

        if (isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
            renderTooltip(context, mouseX, mouseY, Text.literal("Tooltip information"))
        }
    }

    fun renderTooltip(context: DrawContext, mouseX: Int, mouseY: Int, tooltip: Text) {
        context.drawTooltip(
            MinecraftClient.getInstance().textRenderer,
            tooltip,
            mouseX,
            mouseY
        )
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (!isMouseOver(click.x, click.y)) return false

        if (click.button() == 0) { // Left click
            MinecraftClient.getInstance().player?.sendMessage(
                Text.literal("Button Clicked"),
                false
            )
        }
        if (click.button() == 1) {
            MinecraftClient.getInstance().player?.sendMessage(
                Text.literal("Right Clicked"),
                false
            )
        }

        return true
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        TODO("Not yet implemented")
    }

}