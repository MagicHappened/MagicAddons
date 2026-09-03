package org.magic.magicaddons

import org.slf4j.LoggerFactory

object Common {
    val MOD_ID = "magicaddons"
    val MOD_NAME = "MagicAddons"
    val LOGGER = LoggerFactory.getLogger(MagicAddons::class.java)

    object UI {
        const val BACKGROUND_COLOR: Int = 0xFF555555.toInt()
        const val BORDER_SIZE: Int = 2
        const val BORDER_COLOR: Int = 0xFF000000.toInt()
        const val TEXT_X_PAD: Int = 4

        /** The gaps of this ui: SPACING between things that belong together, SPACING_LARGE between groups. */
        const val SPACING_SMALL: Int = 2
        const val SPACING: Int = 4
        const val SPACING_LARGE: Int = 10

        /** Plain text on a panel of this mod. */
        const val TEXT_COLOR: Int = 0xFFFFFFFF.toInt()

        /** Text on a control that does nothing at the moment. */
        const val DISABLED_TEXT_COLOR: Int = 0xFF888888.toInt()

        /** A tick, a plus, anything that says yes. */
        const val SUCCESS_COLOR: Int = 0xFF00FF00.toInt()

        /** A cross, a remove button, anything that takes something away. */
        const val DANGER_COLOR: Int = 0xFFFF0000.toInt()
    }
}
