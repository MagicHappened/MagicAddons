package org.magic.magicaddons.events.chat

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/**
 * A system chat message. Skyblock sends almost everything this way, the action bar included, and
 * the text is stripped of formatting codes for matching.
 */
class OnSystemChatEvent(
    val message: Component,
    val overlay: Boolean
) {
    val text: String = ChatFormatting.stripFormatting(message.string) ?: ""
}
