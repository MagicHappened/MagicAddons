package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Something drawn under a setting's row that the setting does not store: what the chosen value
 * currently means, worked out afresh each frame and never written to disk.
 *
 * Text is the only kind so far; a picture or a bar would each be one more case here.
 */
sealed interface SettingDetail {

    /** How tall this wants to be drawn, in pixels. */
    fun height(font: Font): Int

    /** Draws itself into the strip a row has set aside for it. */
    fun render(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int, width: Int)

    /** One line of plain text, the ordinary case. */
    data class Text(val text: String, val color: Int = GRAY) : SettingDetail {

        override fun height(font: Font): Int = font.lineHeight

        override fun render(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int, width: Int) {
            graphics.text(font, text, x, y, color, false)
        }
    }

    companion object {
        /** Quiet enough to read as a note rather than as another setting. */
        const val GRAY: Int = 0xFFAAAAAA.toInt()
    }
}
