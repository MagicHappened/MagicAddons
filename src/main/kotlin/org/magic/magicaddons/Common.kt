package org.magic.magicaddons

import org.slf4j.LoggerFactory

object Common {
    const val MOD_ID = "magicaddons"
    const val MOD_NAME = "MagicAddons"
    val LOGGER = LoggerFactory.getLogger(MagicAddons::class.java)

    object UI {
        const val BACKGROUND_COLOR: Int = 0xFF555555.toInt()
        const val BORDER_SIZE: Int = 2
        const val BORDER_COLOR: Int = 0xFF000000.toInt()
        const val TEXT_X_PAD: Int = 4

        /** A column of settings or features never grows wider than this, however wide the screen. */
        const val COLUMN_MAX_WIDTH: Int = 250

        /** Space kept clear at each side of a screen, as a share of its width. */
        const val SCREEN_SIDE_PAD_FRACTION: Float = 0.025f

        /** Never less side space than this, even on a tiny window. */
        const val SCREEN_SIDE_PAD_MIN: Int = 4

        /** Layout gaps are written for a screen this many units tall and stretch with the real height. */
        const val LAYOUT_REFERENCE_HEIGHT: Int = 500

        /** How far one wheel notch moves a scrolling screen. */
        const val SCROLL_STEP: Int = 20

        /**
         * The gaps of this ui, so panels line up with each other instead of nearly lining up.
         * [SPACING] separates things that belong together, [SPACING_LARGE] separates groups.
         */
        const val SPACING_SMALL: Int = 2
        const val SPACING: Int = 4
        const val SPACING_LARGE: Int = 10

        /** Text drawn over something busy needs its own ground to stay readable. */
        const val OVERLAY_TEXT_COLOR: Int = 0xFFFFFFFF.toInt()
        const val OVERLAY_BACKGROUND_COLOR: Int = 0xB0000000.toInt()

        /** Plain text on a panel of this mod. */
        const val TEXT_COLOR: Int = 0xFFFFFFFF.toInt()

        /** Text on a control that does nothing at the moment. */
        const val DISABLED_TEXT_COLOR: Int = 0xFF888888.toInt()

        /** Something the player should look at, such as a greenhouse running on guessed data. */
        const val WARNING_COLOR: Int = 0xFFAA0000.toInt()

        /** The lines between the slots of a grid. */
        const val GRID_LINE_COLOR: Int = 0x800683C1.toInt()

        /** A label beside the value it names, quieter than the value itself. */
        const val TEXT_DIM_COLOR: Int = 0xFFCCCCCC.toInt()

        /** Something enabled or accepted. */
        const val SUCCESS_COLOR: Int = 0xFF00FF00.toInt()

        /** Something that removes or destroys, such as a delete cross. */
        const val DANGER_COLOR: Int = 0xFFFF0000.toInt()

        /** The water meter: what a plant has, what it owes, and the track they sit in. */
        const val WATER_FULL_COLOR: Int = 0xFF3F7FDF.toInt()
        const val WATER_DEBT_COLOR: Int = 0xFFCC3333.toInt()
        const val WATER_TRACK_COLOR: Int = 0xB0202020.toInt()
    }
}
