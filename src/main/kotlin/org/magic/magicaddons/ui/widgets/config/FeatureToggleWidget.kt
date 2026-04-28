package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.ui.screens.FeatureEditScreen
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class FeatureToggleWidget(
    val feature: Feature
) : Renderable, GuiEventListener {
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

    override fun render(guiGraphics: GuiGraphics, mouseY: Int, j: Int, delta: Float) {
        checkbox.render(guiGraphics)

        guiGraphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        val font = Minecraft.getInstance().font
        val textY = y + (height - font.lineHeight) / 2

        guiGraphics.drawString(
            font,
            feature.displayName,
            x + checkbox.size + textXPad,
            textY,
            0xFFFFFFFF.toInt(),
            false
        )
    }

    fun getContentWidth(): Int {
        val font = Minecraft.getInstance().font
        val textWidth = font.width(feature.displayName)

        val padding = checkbox.size + textXPad + 10

        return maxOf(100, textWidth + padding)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (checkbox.mouseClicked(mouseButtonEvent, doubled)) {
            feature.baseSetting.value = !feature.baseSetting.value
            return true
        }

        if (mouseButtonEvent.button() == 1) {

            // no need to check for checkbox x and y because of above if statement
            if (mouseButtonEvent.x.toInt() in x..x + width
                && mouseButtonEvent.y.toInt() in y + 0..y + height
            ) {

                val currentScreen = Minecraft.getInstance().screen
                if (currentScreen !is ConfigScreen) {
                    return false
                }
                if (feature.baseSetting.children == null){
                    ChatUtils.sendWithPrefix("Feature ${feature.displayName} does not have sub settings.")
                    return true
                }
                val featureEditScreen = FeatureEditScreen(feature, currentScreen)
                Minecraft.getInstance().setScreen(featureEditScreen)
                return true
            }
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }


    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }
    override fun isFocused(): Boolean = isFocused

}