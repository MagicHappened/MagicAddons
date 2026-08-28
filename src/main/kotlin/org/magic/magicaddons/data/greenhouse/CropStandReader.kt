package org.magic.magicaddons.data.greenhouse

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.Optional

/**
 * A value read off a stand standing near a plant.
 *
 * Not the same thing as a [CropArmorStand], which is a stand that has to be there for a stage to
 * match at all. A reader never decides whether a plant is what we think it is: it looks at what is
 * already standing there and takes a number off it. That distinction is the whole point. A
 * fleshtrap's hunger and a snoozling's sleep change from moment to moment, and a stage that failed
 * to match because a bar happened to be empty would be a stage that fails for the wrong reason.
 *
 * [key] is what the value is filed under on the plant, so anything reading it later asks by name.
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
        private val EMPTY = TextColor.WHITE.value

        /**
         * Reads any of skyblock's bars as the percentage of it that is filled.
         *
         * The filled notches come first in some colour and the empty ones follow in white, and
         * which colour the filled ones are is the bar saying how it feels about the number rather
         * than part of the number: a hunger bar runs green to yellow to red on its way down while
         * still meaning the same thing at the same fill. So anything that is not white counts as
         * filled and the colour itself is ignored.
         *
         * Counting notches rather than taking the leading run is what keeps a bar of nothing but
         * white from reading as completely full.
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
