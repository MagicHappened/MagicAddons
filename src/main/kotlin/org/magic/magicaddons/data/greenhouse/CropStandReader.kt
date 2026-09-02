package org.magic.magicaddons.data.greenhouse

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.Optional
import org.magic.magicaddons.util.compat.McCompat

/**
 * A value read off a stand near a plant, filed under its key. Unlike a CropArmorStand it never
 * decides whether the plant matched: a hunger bar that happens to be empty must not fail a stage.
 */
class CropStandReader(
    val key: String,
    /** Picks this reader's stand out of the ones standing around the plant. */
    val matches: (ArmorStand) -> Boolean,
    /** What that stand says, or null when it says nothing this reader understands. */
    val read: (ArmorStand) -> Int?
) {
    companion object {

        /** Names a plant carries that say it is asleep, and how far along its hunger is. */
        const val ASLEEP: String = "asleep"
        const val HUNGER: String = "hunger"
        const val BONUS: String = "bonus"

        /** Which time of day a plant craves, filed by a stage trait rather than a reader. */
        const val CRAVES: String = "craves"
        const val CRAVES_DAY: Int = 0
        const val CRAVES_NIGHT: Int = 1

        /** The character skyblock builds every one of its bars out of. */
        private const val BAR_CHAR: Char = '|'

        /** An empty notch, whatever the filled ones happen to be coloured. */
        private val EMPTY = McCompat.chatColor(ChatFormatting.WHITE)

        /**
         * Any skyblock bar as the percentage of it that is filled. Any colour but white counts as
         * filled, since a bar changes colour as it empties, and the filled notches are counted
         * rather than measured as a leading run.
         */
        fun barPercent(name: Component): Int? {
            var filled = 0
            var total = 0

            name.visit({ style, text ->
                val notches = text.count { it == BAR_CHAR }

                if (notches > 0) {
                    if (style.color?.value != EMPTY) filled += notches
                    total += notches
                }

                Optional.empty<Unit>()
            }, Style.EMPTY)

            if (total == 0) return null

            return filled * 100 / total
        }

        /** A reader for a bar, found by being one. */
        fun bar(key: String): CropStandReader = CropStandReader(
            key = key,
            matches = { it.customName?.let { name -> barPercent(name) != null } == true },
            read = { it.customName?.let { name -> barPercent(name) } }
        )

        /** A reader for a label such as "+40% Bonus", taking the number out of it. */
        fun percentLabel(key: String, contains: String): CropStandReader = CropStandReader(
            key = key,
            matches = { it.customName?.string?.contains(contains, ignoreCase = true) == true },
            read = {
                it.customName?.string
                    ?.let { text -> Regex("""(-?\d+)\s*%""").find(text)?.groupValues?.get(1) }
                    ?.toIntOrNull()
            }
        )

        /** A reader that only says whether a stand is there at all, as one or nothing. */
        fun presence(key: String, contains: String): CropStandReader = CropStandReader(
            key = key,
            matches = { it.customName?.string?.contains(contains, ignoreCase = true) == true },
            read = { 1 }
        )
    }
}
