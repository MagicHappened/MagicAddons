package org.magic.magicaddons.features.account

import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.data.server.ServerSession
import org.magic.magicaddons.features.Feature

object ServerConnection : Feature() {

    const val CATEGORY: String = "account"

    override val id: String = "ServerConnection"
    override val displayName: String = "Connect to MagicAddons Server"
    override val description: String =
        "Enables features marked with ${SettingNode.SERVER_ICON_TOKEN} that need a connection to the MagicAddons server."
    override val category: String = CATEGORY

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        valueChanged = { value -> if (value) ServerSession.connectIfNeeded() }
    )
}
