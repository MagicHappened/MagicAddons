package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.ui.screens.FeatureEditScreen
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class FeatureToggleWidget(
    val feature: Feature
) : Drawable, Element {
    var x: Int = 0
    var y: Int = 0


    var width: Int = 100

    var height: Int = 25

    val borderSize = 2

    val textXPad: Int = 10



    val borderColor: Int = 0xFF000000.toInt()

    val checkbox = CheckboxWidget(checked = feature.baseSetting.value)

    fun init(){
        checkbox.size = height

    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        checkbox.render(ctx)

        ScreenUtil.drawBorder(ctx, x, y, x + width, y + height, borderSize, borderColor)

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val textY = y + (height - textRenderer.fontHeight) / 2

        ctx.drawText(
            textRenderer,
            feature.displayName,
            x + checkbox.size + textXPad,
            textY,
            0xFFFFFFFF.toInt(),
            false
        )
    }

    fun getContentWidth(): Int {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val textWidth = textRenderer.getWidth(feature.displayName)

        val padding = checkbox.size + textXPad + 10

        return maxOf(100, textWidth + padding)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (checkbox.mouseClicked(click, doubled)) {
            feature.baseSetting.value = !feature.baseSetting.value
            return true
        }

        if (click.button() == 1) {

            // no need to check for checkbox x and y because of above if statement
            if (click.x.toInt() in x..x + width
                && click.y.toInt() in y + 0..y + height
            ) {

                val currentScreen = MinecraftClient.getInstance().currentScreen
                if (currentScreen !is ConfigScreen) {
                    return false
                }
                if (feature.baseSetting.children == null){
                    ChatUtils.sendWithPrefix("Feature ${feature.displayName} does not have sub settings.")
                    return true
                }
                val featureEditScreen = FeatureEditScreen(feature, currentScreen)
                MinecraftClient.getInstance().setScreen(featureEditScreen)
                return true
            }
        }
        return super.mouseClicked(click, doubled)
    }


    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }
    override fun isFocused(): Boolean = isFocused

}