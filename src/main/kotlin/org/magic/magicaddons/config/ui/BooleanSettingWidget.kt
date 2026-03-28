package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.features.Feature


class BooleanSettingWidget(
    private val setting: BooleanSetting
) : SettingWidget<Boolean>(setting) {


    override fun renderSelf(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val color = if (setting.value) 0xFF00FF00.toInt() else 0xFF555555.toInt()

        ctx.fill(x, y, x + width, y + height, color)

        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal(setting.displayName),
            x + 6,
            y + 6,
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