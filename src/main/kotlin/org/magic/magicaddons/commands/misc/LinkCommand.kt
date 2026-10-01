package org.magic.magicaddons.commands.misc

import com.google.gson.JsonParser
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.server.ServerRequirements
import org.magic.magicaddons.data.server.ServerSession
import org.magic.magicaddons.util.ChatUtils
import java.net.http.HttpRequest

object LinkCommand : AbstractCommand() {

    private const val LINK_CODE_GROUP_LENGTH: Int = 3

    override val argument: String = "link"

    val command: String = "/${Common.MOD_NAME} $argument"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            requestLinkCode()
            return@executes 1
        }

    private fun requestLinkCode() {
        ServerRequirements.missingRequirementMessage(needsDiscordIntegration = false)?.let {
            ChatUtils.sendWithPrefix(it)
            return
        }

        ServerSession.sendAuthorized("/link/code") { POST(HttpRequest.BodyPublishers.noBody()) }.thenAccept { response ->
            Minecraft.getInstance().execute {
                if (response == null || response.statusCode() != ServerSession.HTTP_OK) {
                    ChatUtils.sendWithPrefix("Could not get a link code (${response?.statusCode() ?: "server unreachable"}). See the log.")
                    return@execute
                }

                val code = JsonParser.parseString(response.body()).asJsonObject.get("code").asString
                val shownCode = code.chunked(LINK_CODE_GROUP_LENGTH).joinToString("-")

                Minecraft.getInstance().keyboardHandler.clipboard = shownCode
                ChatUtils.sendWithPrefix(
                    Component.literal("Link code: ")
                        .append(
                            ChatUtils.buildStyled(
                                shownCode,
                                ChatFormatting.AQUA,
                                Component.literal("Click to copy"),
                                ClickEvent.CopyToClipboard(shownCode)
                            )
                        )
                        .append(Component.literal(" (copied, valid 10 minutes). Run /link in Discord with it."))
                )
            }
        }
    }
}
