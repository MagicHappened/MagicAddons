package org.magic.magicaddons.config.ui

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import org.magic.magicaddons.util.ScreenUtil.drawLine

class CheckboxWidget(
    var size: Int = 24,
    var checked: Boolean = false
) : Element {

    var x: Int = 0
    var y: Int = 0

    val baseSize = 48f

    val bgColor = 0xFFC6C6C6.toInt()
    val checkColor = 0xFF00FF00.toInt()


    fun render(ctx: DrawContext) {

        ctx.fill(x, y, x + size, y + size, bgColor)

        if (checked) {
            drawCheckmark(ctx)
        }
    }


    private fun drawCheckmark(ctx: DrawContext) {

        fun sx(px: Float) = x + (px / baseSize * size)
        fun sy(py: Float) = y + (py / baseSize * size)

        val x1 = sx(12f)
        val y1 = sy(24f)

        val x2 = sx(20f)
        val y2 = sy(32f)

        val x3 = sx(36f)
        val y3 = sy(12f)

        val thickness = (size / 8).coerceAtLeast(1).toFloat()

        // extend first segment slightly
        val dx = x2 - x1
        val dy = y2 - y1
        val len = kotlin.math.sqrt(dx * dx + dy * dy)

        val extend = thickness * 0.5f

        val ex = (dx / len) * extend
        val ey = (dy / len) * extend

        val newX2 = x2 + ex
        val newY2 = y2 + ey

        ctx.state.drawLine(x1, y1, newX2, newY2, thickness, checkColor)
        ctx.state.drawLine(x2, y2, x3, y3, thickness, checkColor)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.x.toInt() in x..(x + size) &&
            click.y.toInt() in y..(y + size)
        ) {
            checked = !checked
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}