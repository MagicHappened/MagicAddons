package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.ui.CheckboxWidget
import org.magic.magicaddons.config.ui.feature.SettingWidget
import org.magic.magicaddons.util.ScreenUtil

class BooleanSettingWidget(
    private val setting: BooleanSetting
) : SettingWidget<Boolean>(setting) {

    val borderSize = 2
    val borderColor: Int = 0xFF000000.toInt()

    val textXPad: Int = 10

    val checkbox = CheckboxWidget(checked = setting.value)

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val color = if (setting.value) 0xFF00FF00.toInt() else 0xFF555555.toInt()
        ctx.fill(x, y, x + width, y + height, color)

        ScreenUtil.drawBorder(ctx,x,y,x+height,y+height, borderSize , borderColor)

        checkbox.width = height
        checkbox.height = height
        checkbox.render(ctx, mouseX, mouseY, delta)

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val textY = y + (height - textRenderer.fontHeight) / 2

        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal(setting.displayName),
            x + checkbox.width + textXPad,
            textY,
            0xFFFFFF,
            false
        )
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val inside = click.x in x.toDouble()..(x + width).toDouble() &&
                click.y in y.toDouble()..(y + height).toDouble()

        if (inside) {
            setting.value = !setting.value
            return true
        }

        // TODO mouse clicked for children

        return false
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}