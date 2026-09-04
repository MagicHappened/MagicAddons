package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawField
import kotlin.math.abs
import kotlin.math.roundToInt

/** A square field with a green tick in it when checked. */
class CheckboxWidget(
    var size: Int = 24,
    var checked: Boolean = false
) : Focusable {

    override var focusedState: Boolean = false

    var x: Int = 0
    var y: Int = 0

    /** The tick is drawn on a 48 unit grid and scaled to the box. */
    private val baseSize = 48f

    fun render(graphics: GuiGraphicsExtractor) {
        graphics.drawField(x, y, x + size, y + size, false)

        if (checked) {
            drawCheckmark(graphics)
        }
    }

    /** The tick as whole pixels: squares walked along its two strokes, so it never shimmers. */
    private fun drawCheckmark(graphics: GuiGraphicsExtractor) {
        fun sx(px: Float) = x + px / baseSize * size
        fun sy(py: Float) = y + py / baseSize * size

        // a small box still gets a mark two pixels thick, or the tick reads as a faint scratch
        val thickness = (size / 8).coerceAtLeast(2)

        fun stroke(x1: Float, y1: Float, x2: Float, y2: Float) {
            val steps = (maxOf(abs(x2 - x1), abs(y2 - y1))).toInt().coerceAtLeast(1)
            for (step in 0..steps) {
                val px = (x1 + (x2 - x1) * step / steps).roundToInt()
                val py = (y1 + (y2 - y1) * step / steps).roundToInt()
                graphics.fill(px, py, px + thickness, py + thickness, Common.UI.CHECK_COLOR)
            }
        }

        stroke(sx(11f), sy(23f), sx(19f), sy(31f))
        stroke(sx(19f), sy(31f), sx(35f), sy(11f))
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            checked = !checked
            return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in x..x + size) && (mouseY.toInt() in y..y + size)
    }
}
