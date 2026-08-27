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
    }
}