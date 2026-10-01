package org.magic.magicaddons.data.server

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.features.EditFeature
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.account.ServerConnection
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.ui.widgets.ServerCableIcon
import org.magic.magicaddons.util.ChatUtils

object ServerRequirements {

    fun missingRequirementMessage(needsDiscordIntegration: Boolean): Component? {
        switchedOffSettingMessage(ServerConnection, ServerConnection.baseSetting.key)?.let { return it }
        if (needsDiscordIntegration) {
            switchedOffSettingMessage(GreenhousePresets, GreenhousePresets.DISCORD_INTEGRATION_KEY)?.let { return it }
        }
        if (!ServerSession.isConnected) {
            val reason = Component.empty()
            ServerCableIcon.statusLinesOf(ServerSession.status).forEachIndexed { index, line ->
                if (index > 0) reason.append(Component.literal("\n"))
                reason.append(line)
            }
            return ChatUtils.buildStyled("Not authenticated to the MagicAddons server.", hover = reason)
        }
        return null
    }

    private fun switchedOffSettingMessage(feature: Feature, settingKey: String): Component? {
        val switchedOff = feature.pathToSetting(settingKey)
            ?.filterIsInstance<BooleanSetting>()
            ?.firstOrNull { !it.value }
            ?: return null

        return Component.literal("Turn on ")
            .append(settingLink(feature, switchedOff))
            .append(Component.literal(" to use this."))
    }

    private fun settingLink(feature: Feature, setting: BooleanSetting): Component {
        val settingKey = setting.key.takeUnless { setting === feature.baseSetting }

        return ChatUtils.buildStyled(
            setting.displayName,
            ChatFormatting.AQUA,
            Component.literal("Click to open this setting"),
            ClickEvent.RunCommand(EditFeature.commandFor(feature.id, settingKey)),
            underlined = true
        )
    }
}
