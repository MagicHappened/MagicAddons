package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawField
import org.magic.magicaddons.util.ScreenUtil.inRect
import kotlin.math.abs
import kotlin.math.roundToInt

class CheckboxWidget(
    var size: Int = 24,
    var isChecked: Boolean = false
) : Focusable {

    override var focusedState: Boolean = false

    var x: Int = 0
    var y: Int = 0

    fun render(graphics: GuiGraphicsExtractor) {
        graphics.drawField(x, y, x + size, y + size, false)

        if (isChecked) {
            drawCheckmark(graphics)
        }
    }

    private fun drawCheckmark(graphics: GuiGraphicsExtractor) {
        fun gridToScreenX(gridX: Float) = x + gridX / CHECK_GRID_SIZE * size
        fun gridToScreenY(gridY: Float) = y + gridY / CHECK_GRID_SIZE * size

        val thickness = (size / 8).coerceAtLeast(2)

        fun drawStroke(x1: Float, y1: Float, x2: Float, y2: Float) {
            val steps = (maxOf(abs(x2 - x1), abs(y2 - y1))).toInt().coerceAtLeast(1)
            for (step in 0..steps) {
                val px = (x1 + (x2 - x1) * step / steps).roundToInt()
                val py = (y1 + (y2 - y1) * step / steps).roundToInt()
                graphics.fill(px, py, px + thickness, py + thickness, Common.UI.CHECK_COLOR)
            }
        }

        drawStroke(gridToScreenX(11f), gridToScreenY(23f), gridToScreenX(19f), gridToScreenY(31f))
        drawStroke(gridToScreenX(19f), gridToScreenY(31f), gridToScreenX(35f), gridToScreenY(11f))
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            isChecked = !isChecked
            return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = inRect(mouseX, mouseY, x, y, size, size)

    private companion object {
        const val CHECK_GRID_SIZE: Float = 48f
    }
}
