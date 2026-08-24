package org.magic.magicaddons.events.chat

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/**
 * A system chat message received from the server. Skyblock routes almost everything through system
 * chat, so this covers normal server messages as well as the action bar ([overlay]).
 *
 * [text] is the message without any formatting codes, which is what matching server messages needs.
 */
class OnSystemChatEvent(
    val message: Component,
    val overlay: Boolean
) {
    val text: String = ChatFormatting.stripFormatting(message.string) ?: ""
}
