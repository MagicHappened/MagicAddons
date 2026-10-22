package org.magic.magicaddons.commands.debug

import com.google.gson.GsonBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.RegistryOps
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.util.ChatUtils

object ExportSackMessage {

    private const val ARGUMENT: String = "sackMessage"

    private const val SACKS_PREFIX: String = "[Sacks]"

    private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private var lastSackMessage: Component? = null

    init {
        EventBus.register(this)
    }

    fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(ARGUMENT).executes { copyLastSackMessage() }

    @EventHandler
    fun onSystemChat(event: SystemChatEvent) {
        if (event.text.startsWith(SACKS_PREFIX)) lastSackMessage = event.message
    }

    private fun copyLastSackMessage(): Int {
        val message = lastSackMessage
        if (message == null) {
            ChatUtils.sendWithPrefix("No sacks message seen since the game started.")
            return 0
        }
        val registries = Minecraft.getInstance().connection?.registryAccess()
        if (registries == null) {
            ChatUtils.sendWithPrefix("Not connected to a server.")
            return 0
        }

        val json = ComponentSerialization.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), message).getOrThrow()
        Minecraft.getInstance().keyboardHandler.clipboard = GSON.toJson(json)
        ChatUtils.sendWithPrefix("Copied the last sacks message to your clipboard.")
        return 1
    }
}
