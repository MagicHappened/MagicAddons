package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.util.ScreenUtil

open class BaseRowWidget<T>(
    val value: T,
    val displayText: (T) -> String
) {

    var width: Int = 200
    var height: Int = 20

    var x: Int = 0
    var y: Int = 0

    open fun getRightReservedWidth(): Int = 0

    open fun getLeftReservedWidth(): Int = 0


    open fun render(ctx: DrawContext) {
        val tr = MinecraftClient.getInstance().textRenderer

        ctx.fill(x, y, x + width, y + height, 0xFF555555.toInt())

        val usableWidth = width - getRightReservedWidth() - getLeftReservedWidth() - 10

        val text = tr.trimToWidth(displayText(value), usableWidth)

        ctx.drawText(
            tr,
            Text.literal(text),
            x + getLeftReservedWidth(),
            y + (height - tr.fontHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, 1, 0xFF000000.toInt())
    }
}