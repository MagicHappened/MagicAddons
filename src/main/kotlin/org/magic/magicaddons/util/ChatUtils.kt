package org.magic.magicaddons.util

import net.minecraft.client.MinecraftClient
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import java.net.URI

object ChatUtils {
    fun sendWithPrefix(message: String) {
        MinecraftClient.getInstance().player?.sendMessage(Text.literal("[MagicAddons] $message"), false) ?: return
    }
    fun buildWithPrefix(message: String?): Text {
        message?.takeIf { it.isNotBlank() }?.let {
            return Text.literal("[MagicAddons] $message")
        }
        return Text.literal("")
    }
    fun sendWithPrefix(message: Text) {
        val prefixed = buildWithPrefix(message)
        MinecraftClient.getInstance().player?.sendMessage(prefixed, false)
    }
    fun buildWithPrefix(message: Text?): Text {
        val prefix = Text.literal("[MagicAddons] ")

        return if (message != null && message != Text.empty()) {
            prefix.append(message)
        } else {
            prefix
        }
    }

    fun clickableUrl(
        text: String,
        url: String
    ): Text {
        val text: Text = Text.literal(text).setStyle(
            Style.EMPTY
                .withClickEvent(
                    ClickEvent.OpenUrl(URI(url))
                )
        )
        return text
    }
}