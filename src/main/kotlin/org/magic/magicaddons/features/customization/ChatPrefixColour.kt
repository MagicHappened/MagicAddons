package org.magic.magicaddons.features.customization

enum class ChatPrefixColour(private val label: String, val rgb: Int) {
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
