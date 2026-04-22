package org.magic.magicaddons.util


import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ChatUtils {
    fun sendWithPrefix(message: String) {
        Minecraft.getInstance().player?.displayClientMessage(Component.literal("[MagicAddons] $message"), false) ?: return
    }
    fun buildWithPrefix(message: String?): Component {
        message?.takeIf { it.isNotBlank() }?.let {
            return Component.literal("[MagicAddons] $message")
        }
        return Component.literal("")
    }
    fun sendWithPrefix(message: Component) {
        val prefixed = buildWithPrefix(message)
        Minecraft.getInstance().player?.displayClientMessage(prefixed, false)
    }
    fun buildWithPrefix(message: Component?): Component {
        val prefix = Component.literal("[MagicAddons] ")

        return if (message != null && message != Component.empty()) {
            prefix.append(message)
        } else {
            prefix
        }
    }

}