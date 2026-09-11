package org.magic.magicaddons.ui.background

/** How a picture is fitted to the space it is drawn in when the two are not the same shape. */
enum class BackgroundFit(private val label: String) {
    /** Filled, keeping the picture's shape, with whatever hangs over the edges cut off. */
    Cover("Cover"),

    /** Whole picture, keeping its shape, with the space left over showing around it. */
    Contain("Contain"),

    /** Pulled to the edges, which changes the picture's shape. */
    Stretch("Stretch"),

    /** Drawn at its own size, over and over, until the space is full. */
    Tile("Tile");

    override fun toString(): String = label
}
