package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.ui.screens.FeatureEditScreen
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight
import org.magic.magicaddons.util.compat.McCompat

class FeatureToggleWidget(
    val feature: Feature
) : Renderable, GuiEventListener {
    var x: Int = 0
    var y: Int = 0

    var width: Int = 100

    /** The row is this tall unless its name wraps to more lines; the height of a vanilla button. */
    val baseHeight: Int = 20

    var height: Int = baseHeight

    val borderSize = 2

    /** Gap between the checkbox and the name, like the gap after a pack icon in vanilla lists. */
    val textXPad: Int = 3

    val textYPad: Int = 2

    val borderColor: Int = 0xFF000000.toInt()

    val backgroundColor: Int = Common.UI.BACKGROUND_COLOR

    val checkbox = CheckboxWidget(checked = feature.baseSetting.value)

    private val font get() = Minecraft.getInstance().font

    private val name: Component get() = Component.literal(feature.displayName)

    /** The checkbox is as tall as the row, so the text starts past a square of the row's height. */
    private fun textWidth(): Int = width - height - textXPad * 2

    /** How tall the row must be for its name to fit beside a checkbox of [checkboxSize]. */
    fun neededHeight(checkboxSize: Int): Int {
        val textWidth = width - checkboxSize - textXPad * 2
        return (wrappedHeight(font, name, textWidth) + textYPad * 2).coerceAtLeast(baseHeight)
    }

    /** Sizes the row to [rowHeight], with the checkbox filling the full height. */
    fun layout(rowHeight: Int) {
        height = rowHeight
        checkbox.size = height
        checkbox.x = x
        checkbox.y = y
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseY: Int, j: Int, delta: Float) {
        guiGraphics.fill(x, y, x + width, y + height, backgroundColor)
        checkbox.render(guiGraphics)

        guiGraphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        val textHeight = wrappedHeight(font, name, textWidth())

        guiGraphics.drawWrappedText(
            font,
            name,
            x + checkbox.size + textXPad,
            y + (height - textHeight) / 2,
            textWidth(),
            0xFFFFFFFF.toInt()
        )
    }

    /** The narrowest the row is allowed to be; a longer name wraps instead of widening it. */
    fun minWidth(): Int = 80

    /** The width that holds the whole name on one line beside a checkbox of [baseHeight]. */
    fun naturalWidth(): Int = baseHeight + textXPad * 2 + font.width(name)

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

                val currentScreen = McCompat.currentScreen()
                if (currentScreen !is ConfigScreen) {
                    return false
                }
                if (feature.baseSetting.children == null){
                    ChatUtils.sendWithPrefix("Feature ${feature.displayName} does not have sub settings.")
                    return true
                }
                val featureEditScreen = FeatureEditScreen(feature, currentScreen)
                McCompat.setScreen(featureEditScreen)
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
