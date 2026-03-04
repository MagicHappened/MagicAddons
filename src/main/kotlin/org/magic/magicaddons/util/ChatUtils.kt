package org.magic.magicaddons.util

import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object ChatUtils {
    fun sendWithPrefix(message: String) {
        MinecraftClient.getInstance().player?.sendMessage(Text.literal("[MagicAddons] $message"), false) ?: return
    }
}