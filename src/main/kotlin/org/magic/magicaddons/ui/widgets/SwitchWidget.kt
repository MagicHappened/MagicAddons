package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.eased
import org.magic.magicaddons.util.ScreenUtil.fillPill

/** A pill switch: an amber track with the knob on the right when on, a dark one with it on the left when off. */
class SwitchWidget(var on: Boolean, val width: Int = WIDTH, val height: Int = HEIGHT) {

    var x: Int = 0
    var y: Int = 0

    var hovered: Boolean = false

    /** When the switch last flipped, so the knob slides over rather than jumps. */
    private var flippedAt: Long = 0

    fun set(value: Boolean) {
        if (value == on) return
        on = value
        flippedAt = System.currentTimeMillis()
    }

    fun render(graphics: GuiGraphicsExtractor) {
        graphics.fillPill(x, y, x + width, y + height, if (on) Common.UI.ACCENT_COLOR else Common.UI.SWITCH_OFF_COLOR)
        if (hovered) graphics.fillPill(x, y, x + width, y + height, Common.UI.HOVER_WASH)

        val knob = height - KNOB_INSET * 2
        val travel = width - KNOB_INSET * 2 - knob
        val along = eased(flippedAt, SLIDE_MS)
        val fraction = if (on) along else 1f - along
        val knobX = x + KNOB_INSET + kotlin.math.round(travel * fraction).toInt()

        graphics.fillPill(knobX, y + KNOB_INSET, knobX + knob, y + KNOB_INSET + knob, if (on) Common.UI.TEXT_COLOR else Common.UI.DISABLED_TEXT_COLOR)
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)
    }

    companion object {
        const val WIDTH: Int = 22
        const val HEIGHT: Int = 12
        private const val KNOB_INSET: Int = 2
        private const val SLIDE_MS: Long = 150
    }
}
