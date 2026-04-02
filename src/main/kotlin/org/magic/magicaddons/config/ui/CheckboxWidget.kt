package org.magic.magicaddons.config.ui

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import org.magic.magicaddons.util.ScreenUtil

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

        // normalize based on 48x48 design
        fun sx(px: Float) = x + (px / baseSize * size)
        fun sy(py: Float) = y + (py / baseSize * size)

        val x1 = sx(12f)
        val y1 = sy(24f)

        val x2 = sx(20f)
        val y2 = sy(32f)

        val x3 = sx(36f)
        val y3 = sy(12f)

        val thickness = (size / 8).coerceAtLeast(1)

        ScreenUtil.drawLine(ctx, x1, y1, x2, y2, thickness, checkColor)
        ScreenUtil.drawLine(ctx, x2, y2, x3, y3, thickness, checkColor)
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