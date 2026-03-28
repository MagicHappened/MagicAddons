package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.features.Feature

class TextSettingWidget(
    private val setting: TextSetting
) : SettingWidget<String>(setting) {


    override fun renderSelf(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("${setting.displayName}: ${setting.value}"),
            x,
            y + 6,
            0xCCCCCC,
            false
        )
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean {
        return isFocused
    }
}