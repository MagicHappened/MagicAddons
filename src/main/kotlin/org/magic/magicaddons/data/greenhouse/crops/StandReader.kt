package org.magic.magicaddons.data.greenhouse.crops

import java.util.Optional
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.compat.McCompat

/** extra data for a plant that doesn't contribute to its stage but needs parsing */
class StandReader(
    val key: String,
    val matches: (ArmorStand) -> Boolean,
    val read: (ArmorStand) -> Int?
) {
    companion object {

        const val ASLEEP: String = "asleep"
        const val HUNGER: String = "hunger"
        const val BONUS: String = "bonus"
        const val CHARGE: String = "charge"

        const val NEEDS_TIME: String = "time"
        const val NEEDS_DAY: Int = 0
        const val NEEDS_NIGHT: Int = 1

        private const val BAR_CHAR: Char = '|'

        private val PERCENT_REGEX = Regex("""(-?\d+)\s*%""")
        private val STAGE_LABEL_REGEX = Regex("""(?i)stage\s*\d+.*""")
        private val STAGE_NUMBER_REGEX = Regex("""(?i)stage\s*(\d+)""")
        private val MULTIPLIER_REGEX = Regex("""(\d+)\s*x""", RegexOption.IGNORE_CASE)

        private val FILLED = McCompat.chatColor(ChatFormatting.BLUE)
        private val DEBT = McCompat.chatColor(ChatFormatting.RED)
        private val EMPTY = setOf(
            McCompat.chatColor(ChatFormatting.WHITE), McCompat.chatColor(ChatFormatting.GRAY), McCompat.chatColor(ChatFormatting.DARK_GRAY)
        )

        private const val SHORTEST_GLYPH_BAR: Int = 3

        class BarNotches(val filled: Int, val debt: Int, val otherColoured: Int, val total: Int)

        fun barNotches(name: Component): BarNotches? {
            var filled = 0
            var debt = 0
            var otherColoured = 0
            var total = 0

            name.visit({ style, text ->
                val notches = notchesIn(text)

                if (notches > 0) {
                    when (style.color?.value) {
                        FILLED -> filled += notches
                        DEBT -> debt += notches
                        in EMPTY -> Unit
                        else -> otherColoured += notches
                    }

                    total += notches
                }

                Optional.empty<Unit>()
            }, Style.EMPTY)

            if (total == 0) return null

            return BarNotches(filled, debt, otherColoured, total)
        }

        private fun notchesIn(text: String): Int {
            val run = text.trim()
            val glyph = run.firstOrNull() ?: return 0
            if (run.length >= SHORTEST_GLYPH_BAR && !glyph.isLetterOrDigit() && run.all { it == glyph }) return run.length
            return text.count { it == BAR_CHAR }
        }

        fun barPercent(name: Component): Int? = barNotches(name)?.let {
            (it.filled + it.debt + it.otherColoured) * 100 / it.total
        }

        /** any coloured notches but the water bar's blue; an all-white bar says nothing */
        fun nonWaterBar(key: String): StandReader = StandReader(
            key = key,
            matches = { it.customName?.let { name -> nonWaterBarPercent(name) } != null },
            read = { it.customName?.let { name -> nonWaterBarPercent(name) } }
        )

        fun nonWaterBarPercent(name: Component): Int? = barNotches(name)
            ?.takeIf { it.filled == 0 && it.debt + it.otherColoured > 0 }
            ?.let { (it.debt + it.otherColoured) * 100 / it.total }

        fun chargeBarPercent(name: Component): Int? = barNotches(name)
            ?.takeIf { it.filled == 0 }
            ?.let { (it.debt + it.otherColoured) * 100 / it.total }

        fun looksLikeWaterBar(name: Component): Boolean = barNotches(name)?.let { it.otherColoured == 0 } == true

        fun percentLabel(key: String, contains: String): StandReader = StandReader(
            key = key,
            matches = { it.customName?.string?.contains(contains, ignoreCase = true) == true },
            read = {
                it.customName?.string
                    ?.let { text -> PERCENT_REGEX.find(text)?.groupValues?.get(1) }
                    ?.toIntOrNull()
            }
        )

        const val LABEL_STAGE: String = "labelStage"
        const val REWARDS_RESET: String = "rewardsReset"
        const val REWARDS_MULTIPLIER: String = "rewardsMultiplier"

        fun stageNumberLabel(key: String = LABEL_STAGE): StandReader = StandReader(
            key = key,
            matches = { it.customName?.string?.trim()?.matches(STAGE_LABEL_REGEX) == true },
            read = { it.customName?.string?.let { text -> STAGE_NUMBER_REGEX.find(text)?.groupValues?.get(1) }?.toIntOrNull() }
        )

        fun multiplierLabel(key: String, contains: String): StandReader = StandReader(
            key = key,
            matches = { it.customName?.string?.contains(contains, ignoreCase = true) == true },
            read = { it.customName?.string?.let { text -> MULTIPLIER_REGEX.find(text)?.groupValues?.get(1) }?.toIntOrNull() }
        )

        fun skullPresence(key: String, hash: String): StandReader = StandReader(
            key = key,
            matches = { PlayerUtils.getSkullHash(it) == hash },
            read = { 1 }
        )

        fun standPresence(key: String, contains: String): StandReader = StandReader(
            key = key,
            matches = { it.customName?.string?.contains(contains, ignoreCase = true) == true },
            read = { 1 }
        )
    }
}
