package org.magic.magicaddons.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil


class FeatureToggleWidget(
    val feature: Feature
) : Drawable, Element {
    var x: Int = 0
    var y: Int = 0


    var width: Int = 200
    var height: Int = 100

    val textXPad: Int = 10
    val textYPad: Int = 5

    val checkboxPadding: Int = 2

    val borderColor: Int = 0xFF000000.toInt()

    val checkbox: CheckboxWidget = CheckboxWidget(2.toFloat())
    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, 2, borderColor)
        checkbox.x = x + checkboxPadding
        checkbox.y = y + checkboxPadding
        checkbox.render(ctx, mouseX, mouseY, delta)

        ctx.drawText(
            MinecraftClient.getInstance().textRenderer,
            feature.displayName,
            x + checkbox.scaledSize + textXPad,
            y + checkbox.scaledSize + textYPad,
            0xFFFFFFFF.toInt(),
            false
        )
    }

    fun getContentWidth(): Int {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val textWidth = textRenderer.getWidth(feature.displayName)

        val padding = checkbox.scaledSize + textXPad + 10
        return maxOf(200, textWidth + padding)
    }

    override fun mouseClicked(click: Click?, doubled: Boolean): Boolean {
        if (checkbox.mouseClicked(click, doubled)) {
            return true
        }
        if (click?.button() == 1){
            //summon screen later
            ChatUtils.sendWithPrefix("Right Clicked ${feature.displayName} not on checkbox")
            return true
        }
        return super.mouseClicked(click, doubled)
    }


    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }
    override fun isFocused(): Boolean = isFocused

}