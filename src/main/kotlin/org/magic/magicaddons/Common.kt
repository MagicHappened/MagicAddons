package org.magic.magicaddons

import org.magic.magicaddons.features.customization.Customization
import org.slf4j.LoggerFactory

object Common {
    const val MOD_ID = "magicaddons"
    const val MOD_NAME = "MagicAddons"
    val LOGGER = LoggerFactory.getLogger(MagicAddons::class.java)

    object UI {
        // the colours a palette decides are read from the one picked in the Customization category;
        // the rest are the same whichever palette that is
        val BACKGROUND_COLOR: Int get() = Customization.fadedPanel(Customization.palette.background)
        val BORDER_SIZE: Int get() = Customization.borderSize

        /** The frame of a small control such as a text field or selector, whatever the panel frame is. */
        const val CONTROL_BORDER_SIZE: Int = 1

        val BORDER_COLOR: Int get() = Customization.fadedBorder(Customization.palette.border)
        const val TEXT_X_PAD: Int = 4

        const val SEARCH_HINT: String = "Search…"

        const val SCROLL_STEP: Int = 20

        const val SPACING_SMALL: Int = 2
        const val SPACING: Int = 4
        const val SPACING_LARGE: Int = 10

        const val SCREEN_DIM_COLOR: Int = 0xB4101010.toInt()

        const val OVERLAY_TEXT_COLOR: Int = 0xFFFFFFFF.toInt()
        const val OVERLAY_BACKGROUND_COLOR: Int = 0xB0000000.toInt()

        val TEXT_COLOR: Int get() = Customization.fadedText(Customization.palette.text)

        val HOVER_WASH: Int get() = Customization.hoverWash

        const val PRESSED_SHADE: Int = 0x50000000

        val SELECTED_FRAME_COLOR: Int get() = Customization.palette.accent

        val FIELD_COLOR: Int get() = Customization.fadedPanel(Customization.palette.field)

        const val FIELD_INSET: Int = 4

        val ACCENT_COLOR: Int get() = SELECTED_FRAME_COLOR

        const val SCROLLBAR_WIDTH: Int = 3
        const val SCROLL_TRACK_COLOR: Int = 0x40000000

        val DISABLED_TEXT_COLOR: Int get() = Customization.fadedText(Customization.palette.disabledText)

        const val WARNING_COLOR: Int = 0xFFAA0000.toInt()

        const val GRID_LINE_COLOR: Int = 0x800683C1.toInt()

        val TEXT_DIM_COLOR: Int get() = Customization.fadedText(Customization.palette.textDim)

        const val SUCCESS_COLOR: Int = 0xFF00FF00.toInt()

        const val CHECK_COLOR: Int = 0xFF1FBF1F.toInt()

        val DIVIDER_COLOR: Int get() = BORDER_COLOR

        val THIN_DIVIDER_COLOR: Int
            get() = Customization.fadedBorder((Customization.palette.border and 0xFFFFFF) or 0x60000000)

        const val GROUP_SHADE: Int = 0x1A000000

        val SWITCH_OFF_COLOR: Int get() = FIELD_COLOR

        const val DANGER_COLOR: Int = 0xFFFF0000.toInt()

        const val WATER_FULL_COLOR: Int = 0xFF3F7FDF.toInt()
        const val WATER_DEBT_COLOR: Int = 0xFFCC3333.toInt()
        const val WATER_TRACK_COLOR: Int = 0xB0202020.toInt()
    }
}
