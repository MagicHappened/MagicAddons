package org.magic.magicaddons.features.farming.fortune

import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent

object GardenVisitors {

    private val OFFER_ACCEPTED: Regex = Regex("""^OFFER ACCEPTED with (.+) \((\w+)\)$""")
    private val NOT_ID_CHARACTER: Regex = Regex("""[^a-z0-9]+""")

    fun idOf(name: String): String = name.lowercase().replace(NOT_ID_CHARACTER, "_").trim('_')

    @EventHandler
    fun onChat(event: SystemChatEvent) {
        val match = OFFER_ACCEPTED.find(event.text.trim()) ?: return
        val id = idOf(match.groupValues[1])
        if (FortuneReadings.hasServed(id)) return
        FortuneReadings.noteServed(listOf(id))
        val visitors = FortuneReadings.get(FortuneField.UniqueVisitors)?.level ?: return
        FortuneReadings.set(FortuneField.UniqueVisitors, null, visitors + 1)
    }
}
