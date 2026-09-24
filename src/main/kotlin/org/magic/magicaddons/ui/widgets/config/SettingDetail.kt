package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.Common
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight


sealed interface SettingDetail {

    fun height(font: Font, width: Int): Int

    fun render(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int, width: Int)

    data class Text(val text: String, val color: Int = Common.UI.TEXT_DIM_COLOR) : SettingDetail {

        override fun height(font: Font, width: Int): Int =
            wrappedHeight(font, Component.literal(text), width)

        override fun render(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int, width: Int) {
            graphics.drawWrappedText(font, Component.literal(text), x, y, width, color)
        }
    }
}
