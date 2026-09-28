package org.magic.magicaddons.events.chat

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

class SystemChatEvent(
    val message: Component,
    val overlay: Boolean
) {
    val text: String = ChatFormatting.stripFormatting(message.string) ?: ""
}
