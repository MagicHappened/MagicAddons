package org.magic.magicaddons.util


import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ChatUtils {
    fun sendWithPrefix(message: String) {
        sendWithPrefix(Component.literal(message).withStyle(ChatFormatting.WHITE))
    }

    fun buildWithPrefix(message: String?): Component {
        val body = message?.takeIf { it.isNotBlank() } ?: return Component.literal("")

        return buildWithPrefix(Component.literal(body).withStyle(ChatFormatting.WHITE))
    }

    fun buildWithPrefix(message: Component?): Component {
        val prefix = Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)

        return if (message != null && message != Component.empty()) prefix.append(message) else prefix
    }

    fun sendWithPrefix(message: Component) {
        val prefixed = buildWithPrefix(message)
        Minecraft.getInstance().player?.sendSystemMessage(prefixed)
    }
    /** Runs a command as if the player typed it, [command] is given without the leading slash. */
    fun sendCommand(command: String) {
        Minecraft.getInstance().player?.connection?.sendCommand(command)
    }

}