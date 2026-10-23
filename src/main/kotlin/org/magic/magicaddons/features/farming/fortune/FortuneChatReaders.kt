package org.magic.magicaddons.features.farming.fortune

import java.time.Duration
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent

object FortuneChatReaders {

    private val PERSONAL_BEST: Regex = Regex("""^\[NPC] Jacob: Your Personal Bests perk is now granting you \+([\d.]+)\W*\s*(.+?) Fortune!$""")
    private val ANITA_TIER_UP: Regex = Regex("""^You tiered up the Extra Farming Fortune upgrade to \+(\d+)""")
    private val COMMUNITY_CLAIM: Regex = Regex("""^You claimed the (.*Farming Fortune) ([IVX]+) upgrade!$""")
    private val BESTIARY_TIER_UP: Regex = Regex("""^BESTIARY (.+?) (\d+) ➡ (\d+)$""")
    private val ROSEWATER_CONSUMED: Regex = Regex("""^You consumed a Filled Rosewater Flask""")
    private val TRUFFLE_CONSUMED: Regex = Regex("""^You consumed a Refined Dark Cacao Truffle and gained \+(\d+)\W*\s*(.+?) Fortune for (\d+)m!$""")
    private val PERMANENT_LIMIT_REACHED: Regex = Regex("""^You have consumed (\d+) of these items and will no longer receive permanent \W*\s*(.+?) Fortune when consuming them!$""")
    private val TRUFFLE_FORTUNE_NAMES: Set<String> = setOf("Global", "Cocoa Beans")

    private val GARDEN_PESTS: Set<String> = setOf(
        "Beetle", "Cricket", "Earthworm", "Fly", "Locust", "Mite", "Mosquito", "Moth", "Rat", "Slug", "Field Mouse",
        "Dragonfly", "Firefly", "Praying Mantis", "Lunar Moth", "Zombuddy", "Timestalk Clone"
    )

    private val ROMAN_VALUES: Map<Char, Int> = mapOf('I' to 1, 'V' to 5, 'X' to 10)

    @EventHandler
    fun onChat(event: SystemChatEvent) {
        val text = event.text.trim()
        PERSONAL_BEST.find(text)?.let { match ->
            FortuneCrops.byStatName(match.groupValues[2]).forEach { FortuneReadings.set(FortuneField.PersonalBest, it, match.groupValues[1].toDouble()) }
            return
        }
        ANITA_TIER_UP.find(text)?.let {
            FortuneReadings.set(FortuneField.Anita, null, it.groupValues[1].toDouble() / FortuneTables.PER_ANITA_LEVEL)
            return
        }
        COMMUNITY_CLAIM.find(text)?.let {
            FortuneReadings.set(FortuneField.Community, null, romanToInt(it.groupValues[2]).toDouble())
            return
        }
        BESTIARY_TIER_UP.find(text)?.let { match ->
            if (match.groupValues[1] !in GARDEN_PESTS) return
            val brackets = FortuneReadings.get(FortuneField.PestBestiary)?.level ?: return
            val gained = match.groupValues[3].toInt() - match.groupValues[2].toInt()
            FortuneReadings.set(FortuneField.PestBestiary, null, (brackets + gained).coerceAtMost(FortuneTables.MAX_PEST_BRACKETS.toDouble()))
            return
        }
        if (ROSEWATER_CONSUMED.containsMatchIn(text)) {
            val flasks = FortuneReadings.get(FortuneField.RosewaterFlasks)?.level ?: return
            if (flasks < FortuneTables.MAX_ROSEWATER_FLASKS) FortuneReadings.set(FortuneField.RosewaterFlasks, null, flasks + 1)
            return
        }
        TRUFFLE_CONSUMED.find(text)?.let { match ->
            val minutes = Duration.ofMinutes(match.groupValues[3].toLong())
            FortuneReadings.setTemporary(TemporaryFortune.CACAO_TRUFFLE, match.groupValues[1].toDouble(), System.currentTimeMillis() + minutes.toMillis())
            val truffles = FortuneReadings.get(FortuneField.Truffles)?.level ?: return
            if (truffles < FortuneTables.MAX_TRUFFLES) FortuneReadings.set(FortuneField.Truffles, null, truffles + 1)
            return
        }
        PERMANENT_LIMIT_REACHED.find(text)?.let { match ->
            if (match.groupValues[2] in TRUFFLE_FORTUNE_NAMES) FortuneReadings.set(FortuneField.Truffles, null, match.groupValues[1].toDouble())
        }
    }

    private fun romanToInt(roman: String): Int {
        var total = 0
        roman.forEachIndexed { index, char ->
            val value = ROMAN_VALUES.getValue(char)
            val next = roman.getOrNull(index + 1)?.let(ROMAN_VALUES::getValue) ?: 0
            total += if (value < next) -value else value
        }
        return total
    }
}
