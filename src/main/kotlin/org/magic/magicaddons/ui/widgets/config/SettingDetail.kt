package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.Common
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight

/**
 * Something drawn under a setting's row that the setting does not store: what the chosen value
 * currently means, worked out afresh each frame and never written to disk.
 *
 * Text is the only kind so far; a picture or a bar would each be one more case here.
 */
sealed interface SettingDetail {

    /** How tall this wants to be drawn when given [width] to wrap in. */
    fun height(font: Font, width: Int): Int

    /** Draws itself into the strip a row has set aside for it. */
    fun render(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int, width: Int)

    /** Plain text, wrapped to the strip's width. */
    data class Text(val text: String, val color: Int = GRAY) : SettingDetail {

        override fun height(font: Font, width: Int): Int =
            wrappedHeight(font, Component.literal(text), width)

        override fun render(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int, width: Int) {
            graphics.drawWrappedText(font, Component.literal(text), x, y, width, color)
        }
    }

    companion object {
        /** Quiet enough to read as a note rather than as another setting. */
        const val GRAY: Int = Common.UI.TEXT_DIM_COLOR
    }
}
