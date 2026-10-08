package org.magic.magicaddons.features.misc

import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.features.Feature

object SkipResourceReloadOnClose : Feature() {

    override val id: String = "SkipResourceReloadOnClose"
    override val displayName: String = "Skip Resource Reload On Close"
    override val description: String =
        "Closing the game while on a server reloads all resource packs to remove the server's pack. " +
            "This skips that reload, which can speed up the game shutting down, and prevent closing crashes."
    override val category: String = "misc"

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false
    )

    @JvmStatic
    fun isEnabled(): Boolean = baseSetting.value

    @JvmStatic
    fun logPreventedReload() {
        Common.LOGGER.info("Prevented game from reloading resources")
    }
}
