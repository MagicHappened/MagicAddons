package org.magic.magicaddons.ui.background

enum class BackgroundSource(private val label: String) {
    None("None"),
    LocalFile("Local file"),
    Url("From a link");

    override fun toString(): String = label
}
