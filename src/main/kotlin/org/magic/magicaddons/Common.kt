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
    }
}