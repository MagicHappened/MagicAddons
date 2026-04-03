package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.text.Text
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class DropDownBoxWidget<T : Enum<T>>(
    val enumValue: T,
    val onClick: (T) -> Unit
) : Element {
    var width: Int = 200
    var height: Int = 40

    var x: Int = 0
    var y: Int = 0

    var widgetText: String = enumValue.name

    val bgColor = 0xFF555555.toInt()
    val borderColor = 0xFF000000.toInt()


    fun render(ctx: DrawContext) {

        ctx.fill(x, y, x + width, y + height, bgColor)
        ScreenUtil.drawBorder(ctx,x,y,x + width,y + height,1, borderColor)
        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal(widgetText),
            x + 10,
            y + (height - MinecraftClient.getInstance().textRenderer.fontHeight) / 2,
            0xFFFFFFFF.toInt(),
            false
        )

    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.x.toInt() in x..(x + width) &&
            click.y.toInt() in y..(y + height)
        ) {
            onClick(enumValue)
            return true
        }
        return false
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}