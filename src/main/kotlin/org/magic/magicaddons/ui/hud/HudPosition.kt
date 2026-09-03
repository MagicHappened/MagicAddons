package org.magic.magicaddons.ui.hud

/**
 * Where a hud element sits. Only the corner offset is drawn today; the fractions of the screen
 * are kept for the hud position editor to come.
 */
class HudPosition(
    val offsetX: Int,
    val offsetY: Int,
    val xFraction: Float,
    val yFraction: Float
) {
    fun x(): Int = offsetX

    fun y(): Int = offsetY
}
