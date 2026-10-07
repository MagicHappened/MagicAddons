package org.magic.magicaddons.commands.misc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import java.net.http.HttpRequest
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.server.ServerRequirements
import org.magic.magicaddons.data.server.ServerSession
import org.magic.magicaddons.util.ChatUtils

object PrivacyCommand : AbstractCommand() {

    private const val CONFIRM_WITHIN_MS: Long = 20_000

    private const val CONFIRM_MESSAGE: String =
        "This action is irreversible, if you wish to proceed run the command again within 20 seconds to confirm."

    override val argument: String = "privacy"

    private var deleteRequestedAtMs: Long = 0

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("DeleteMe").executes {
                    deleteEverything()
                    return@executes 1
                }
            )

    private fun deleteEverything() {
        ServerRequirements.missingRequirementMessage(needsDiscordIntegration = false)?.let {
            ChatUtils.sendWithPrefix(it)
            return
        }

        val now = System.currentTimeMillis()
        if (now - deleteRequestedAtMs > CONFIRM_WITHIN_MS) {
            deleteRequestedAtMs = now
            ChatUtils.sendWithPrefix(Component.literal(CONFIRM_MESSAGE).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            return
        }
        deleteRequestedAtMs = 0

        ServerSession.sendAuthorized("/link") { DELETE() }.thenAccept { response ->
            Minecraft.getInstance().execute {
                if (response == null || response.statusCode() != ServerSession.HTTP_OK) {
                    ChatUtils.sendWithPrefix("Could not delete your data (${response?.statusCode() ?: "server unreachable"}). See the log.")
                    return@execute
                }
                ChatUtils.sendWithPrefix("Everything stored for your account on the MagicAddons server was deleted, including the Discord link.")
            }
        }
    }
}
