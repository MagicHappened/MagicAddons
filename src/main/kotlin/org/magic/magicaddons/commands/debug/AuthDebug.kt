package org.magic.magicaddons.commands.debug

import com.google.gson.JsonParser
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.server.ServerSession
import org.magic.magicaddons.util.ChatUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AuthDebug : AbstractCommand() {

    private val EXPIRY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm").withZone(ZoneId.systemDefault())

    override val argument: String = "auth"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("whoAmI").executes {
                    reportIdentity()
                    return@executes 1
                }
            )

    private fun reportIdentity() {
        if (!ServerSession.isConnected) {
            ChatUtils.sendWithPrefix("Not connected. The switch is off or no valid token is held.")
            return
        }

        ServerSession.sendAuthorized("/me").thenAccept { response ->
            Minecraft.getInstance().execute {
                if (response == null || response.statusCode() != ServerSession.HTTP_OK) {
                    ChatUtils.sendWithPrefix("Server did not accept the session token.")
                    return@execute
                }

                val identity = JsonParser.parseString(response.body()).asJsonObject
                val serverUuid = identity.get("uuid").asString
                val ownUuid = Minecraft.getInstance().user.profileId.toString()
                val verdict = if (serverUuid == ownUuid) "matches this account" else "DOES NOT match this account ($ownUuid)"
                val expiry = EXPIRY_FORMAT.format(Instant.ofEpochMilli(identity.get("expiresAt").asLong))

                ChatUtils.sendWithPrefix("Server says $serverUuid, $verdict. Token valid until $expiry.")

                val isLinked = identity.get("isLinked")?.asBoolean == true
                val absentSince = identity.get("absentSince")?.takeUnless { it.isJsonNull }?.asLong
                ChatUtils.sendWithPrefix(
                    "Linked to Discord: ${if (isLinked) "yes" else "no"}. " +
                            (absentSince?.let { "Absent since ${EXPIRY_FORMAT.format(Instant.ofEpochMilli(it))}." } ?: "Not absent.")
                )
                identity.getAsJsonArray("greenhouses")?.forEach { stored ->
                    val greenhouse = stored.asJsonObject
                    val uploadedAt = EXPIRY_FORMAT.format(Instant.ofEpochMilli(greenhouse.get("uploadedAt").asLong))
                    ChatUtils.sendWithPrefix("Stored greenhouse ${greenhouse.get("profile").asString}: ${greenhouse.get("plots").asInt} plots, uploaded $uploadedAt.")
                }
            }
        }
    }
}
