package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Something drawn under a setting's row that the setting itself does not store.
 *
 * A setting's value is what the player chose; a detail is what that choice currently means, worked
 * out from whatever the mod knows at the moment the config is open - a tick length, a countdown, a
 * count of something on a plot. It is produced by a lambda rather than held as a field, so it is
 * never written to disk and never goes stale, and it knows its own height so a row can make room
 * for it without assuming it is one line of text.
 *
 * [Text] is the only kind so far. A picture, a bar or a small live preview would each be one more
 * case here and nothing else would have to change.
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
