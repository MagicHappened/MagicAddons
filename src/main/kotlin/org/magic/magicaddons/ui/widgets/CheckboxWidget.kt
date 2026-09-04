package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawField
import org.magic.magicaddons.util.ScreenUtil.drawLine
import kotlin.math.sqrt

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

    private fun drawCheckmark(graphics: GuiGraphicsExtractor) {
        fun sx(px: Float) = x + (px / baseSize * size)
        fun sy(py: Float) = y + (py / baseSize * size)

        val x1 = sx(12f)
        val y1 = sy(24f)
        val x2 = sx(20f)
        val y2 = sy(32f)
        val x3 = sx(36f)
        val y3 = sy(12f)

        // a small box still gets a mark two pixels thick, or the tick reads as a faint scratch
        val thickness = (size / 8f).coerceAtLeast(2f)

        // the first stroke runs a little past the corner so the two meet without a notch
        val dx = x2 - x1
        val dy = y2 - y1
        val len = sqrt(dx * dx + dy * dy)
        val extend = thickness * 0.5f

        graphics.drawLine(x1, y1, x2 + dx / len * extend, y2 + dy / len * extend, thickness, Common.UI.SUCCESS_COLOR)
        graphics.drawLine(x2, y2, x3, y3, thickness, Common.UI.SUCCESS_COLOR)
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
