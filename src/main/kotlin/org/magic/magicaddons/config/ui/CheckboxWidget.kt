package org.magic.magicaddons.config.ui

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import org.magic.magicaddons.util.ScreenUtil

class CheckboxWidget(val scale: Float = 1.0F) : Element {
    var x: Int = 0
    var y: Int = 0

    val pxSize: Int = 48
    val borderColor = 0xFF000000.toInt()
    val bgColor = 0xFFC6C6C6.toInt()
    val checkColor = 0xFF00FF00.toInt()
    val scaledSize: Int = (pxSize.toFloat() * scale).toInt()
    var checked = false

    fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        ctx.fill(x, y, x + scaledSize, y + scaledSize, bgColor)

        ScreenUtil.drawSquareBorder(ctx, x, y, scaledSize, (4*scale).toInt(), borderColor)

        if (checked) {
            drawCheckmark(ctx, x, y)
        }
    }

    fun drawCheckmark(ctx: DrawContext, x: Int, y: Int) {
        val x1 = x + (12f * scale)
        val y1 = y + (24f * scale)

        val x2 = x + (20f * scale)
        val y2 = y + (32f * scale)

        val x3 = x + (36f * scale)
        val y3 = y + (12f * scale)

        val thickness = 3f * scale

        ScreenUtil.drawLine(ctx, x1, y1, x2, y2, thickness.toInt(), checkColor)
        ScreenUtil.drawLine(ctx, x2, y2, x3, y3, thickness.toInt(), checkColor)
    }

    override fun mouseClicked(click: Click?, doubled: Boolean): Boolean {
        checked = !checked
        if (click?.x?.toInt() in x..x+scaledSize &&
            click?.y?.toInt() in y..y+scaledSize) return true
        return super.mouseClicked(click, doubled)
    }


    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }
    override fun isFocused(): Boolean = isFocused
}