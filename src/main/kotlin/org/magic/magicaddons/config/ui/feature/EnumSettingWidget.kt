package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.ui.feature.SettingWidget

class EnumSettingWidget<T : Enum<T>>(
    private val setting: EnumSetting<T>
) : SettingWidget<T>(setting) {


    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float)  {
        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("${setting.displayName}: ${setting.value}"),
            x,
            y + 6,
            0xFFFFFF,
            false
        )
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        return false
        //todo change this to actually make the rendering render an on top thing to click different values
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}