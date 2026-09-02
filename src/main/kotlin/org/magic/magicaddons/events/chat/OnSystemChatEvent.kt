package org.magic.magicaddons.events.chat

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/**
 * A system chat message, which is how skyblock sends almost everything, action bar included. The
 * text is stripped of formatting codes, since that is what matching needs.
 */
class OnSystemChatEvent(
    val message: Component,
    val overlay: Boolean
) {
    val text: String = ChatFormatting.stripFormatting(message.string) ?: ""
}
