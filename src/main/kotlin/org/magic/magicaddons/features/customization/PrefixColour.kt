package org.magic.magicaddons.features.customization

/** The colour of the tag the mod puts before its chat messages. */
enum class PrefixColour(private val label: String, val rgb: Int) {
    Default("Default", 0xFFAA00),
    FollowAccent("Follow Accent", 0xFFAA00),
    Green("Green", 0x55FF55),
    Aqua("Aqua", 0x55FFFF),
    Gold("Gold", 0xFFD24A),
    Purple("Purple", 0xC77DFF),
    Red("Red", 0xFF5555),
    Custom("Custom", 0xFFAA00);

    override fun toString(): String = label
}
