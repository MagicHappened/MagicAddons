package org.magic.magicaddons.features.account

import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.SettingNode
import org.magic.magicaddons.data.server.ServerSession
import org.magic.magicaddons.features.Feature

object ServerConnection : Feature() {

    const val CATEGORY: String = "account"

    const val PRIVACY_POLICY_URL: String = "https://github.com/MagicHappened/MagicAddons/blob/beta/PRIVACY.md"

    override val id: String = "ServerConnection"
    override val displayName: String = "Connect to MagicAddons Server"
    override val description: String =
        "Enables features marked with ${SettingNode.SERVER_ICON_TOKEN} that need a connection to the MagicAddons server.\n" +
                "§7Sends some of the game data, player name and player UUID to the MagicAddons server\n" +
                "See the privacy policy: $PRIVACY_POLICY_URL"
    override val category: String = CATEGORY

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        valueChanged = { value -> if (value) ServerSession.connectIfNeeded() }
    )
}
