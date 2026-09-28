package org.magic.magicaddons.ui.background


enum class BackgroundFit(private val label: String) {
    Cover("Cover"),
    Contain("Contain"),
    Stretch("Stretch"),
    Tile("Tile");

    override fun toString(): String = label
}
