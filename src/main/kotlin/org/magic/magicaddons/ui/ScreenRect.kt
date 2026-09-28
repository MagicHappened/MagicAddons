package org.magic.magicaddons.ui

data class ScreenRect(val x: Int, val y: Int, val width: Int, val height: Int) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height

    companion object {
        fun fromEdges(left: Int, top: Int, right: Int, bottom: Int): ScreenRect = ScreenRect(left, top, right - left, bottom - top)
    }
}
