package org.magic.magicaddons.ui.background

/** Where the picture behind the config screen comes from. */
enum class BackgroundSource(private val label: String) {
    None("None"),
    LocalFile("Local file"),
    Url("From a link");

    override fun toString(): String = label
}
