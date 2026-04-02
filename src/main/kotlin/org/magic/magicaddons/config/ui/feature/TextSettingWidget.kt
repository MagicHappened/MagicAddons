package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.magic.magicaddons.config.data.TextSetting

class TextSettingWidget(
    private val setting: TextSetting
) : SettingWidget<String>(setting) {

    override val childrenWidgets: List<SettingWidget<*>>? = null

    override fun init() {}

    override fun getActualHeight(): Int = height

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // todo add a text box renderer here for input
        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("${setting.displayName}: ${setting.value}"),
            x,
            y + 6,
            0xFFCCCCCC.toInt(),
            false
        )
    }

}