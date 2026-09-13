package org.magic.magicaddons.data.greenhouse

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.Optional
import org.magic.magicaddons.util.compat.McCompat

/** extra data for a plant that doesn't contribute to its stage but needs parsing */
class CropStandReader(
    val key: String,
    val matches: (ArmorStand) -> Boolean,
    val read: (ArmorStand) -> Int?
) {
    companion object {

        const val ASLEEP: String = "asleep"
        const val HUNGER: String = "hunger"
        const val BONUS: String = "bonus"

        const val NEEDS_TIME: String = "time"
        const val NEEDS_DAY: Int = 0
        const val NEEDS_NIGHT: Int = 1

        private const val BAR_CHAR: Char = '|'

        private val FILLED = McCompat.chatColor(ChatFormatting.BLUE)
        private val DEBT = McCompat.chatColor(ChatFormatting.RED)
        private val EMPTY = McCompat.chatColor(ChatFormatting.WHITE)

        class BarNotches(val filled: Int, val debt: Int, val other: Int, val total: Int)

        /** bar notches for a given component */
        fun barNotches(name: Component): BarNotches? {
            var filled = 0
            var debt = 0
            var other = 0
            var total = 0

            name.visit({ style, text ->
                val notches = text.count { it == BAR_CHAR }

                if (notches > 0) {
                    when (style.color?.value) {
                        FILLED -> filled += notches
                        DEBT -> debt += notches
                        EMPTY -> Unit
                        else -> other += notches
                    }

                    total += notches
                }

                Optional.empty<Unit>()
            }, Style.EMPTY)

            if (total == 0) return null

            return BarNotches(filled, debt, other, total)
        }

        fun barPercent(name: Component): Int? = barNotches(name)?.let {
            (it.filled + it.debt + it.other) * 100 / it.total
        }

        /**
         * A bar of the plant's own, told by notches in a colour the water bar never uses. While a
         * can is held the water bar hangs where this one did, and an empty water bar is all white,
         * so a bar with no coloured notch is not read: the reading already held stands until the
         * plant's own bar is back.
         */
        fun bar(key: String): CropStandReader = CropStandReader(
            key = key,
            matches = { it.customName?.let { name -> ownBarPercent(name) } != null },
            read = { it.customName?.let { name -> ownBarPercent(name) } }
        )

        fun ownBarPercent(name: Component): Int? = barNotches(name)
            ?.takeIf { it.filled == 0 && it.debt == 0 && it.other > 0 }
            ?.let { it.other * 100 / it.total }

        fun hungerPercentLabel(key: String, contains: String): CropStandReader = CropStandReader(
            key = key,
            matches = { it.customName?.string?.contains(contains, ignoreCase = true) == true },
            read = {
                it.customName?.string
                    ?.let { text -> Regex("""(-?\d+)\s*%""").find(text)?.groupValues?.get(1) }
                    ?.toIntOrNull()
            }
        )

        const val LABEL_STAGE: String = "labelStage"
        const val REWARDS_RESET: String = "rewardsReset"
        const val REWARDS_MULTIPLIER: String = "rewardsMultiplier"

        fun aloeStageLabel(key: String = LABEL_STAGE): CropStandReader = CropStandReader(
            key = key,
            matches = { it.customName?.string?.trim()?.matches(Regex("""(?i)stage\s*\d+.*""")) == true },
            read = { it.customName?.string?.let { text -> Regex("""(?i)stage\s*(\d+)""").find(text)?.groupValues?.get(1) }?.toIntOrNull() }
        )

        fun aloeMultiplierLabel(key: String, contains: String): CropStandReader = CropStandReader(
            key = key,
            matches = { it.customName?.string?.contains(contains, ignoreCase = true) == true },
            read = { it.customName?.string?.let { text -> Regex("""(\d+)\s*x""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1) }?.toIntOrNull() }
        )

        fun standPresence(key: String, contains: String): CropStandReader = CropStandReader(
            key = key,
            matches = { it.customName?.string?.contains(contains, ignoreCase = true) == true },
            read = { 1 }
        )
    }
}
