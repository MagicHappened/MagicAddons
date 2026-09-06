package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.inRect

/** A small mouse with its wheel marked, sat beside a selector the wheel can turn. */
class ScrollHint(var tooltip: String) {

    var x: Int = 0
    var y: Int = 0

    /** Sits the hint beside a control, centred on the control's middle. */
    fun layoutBeside(controlRight: Int, controlTop: Int, controlHeight: Int) {
        x = controlRight + Common.UI.SPACING
        y = controlTop + (controlHeight - SIZE) / 2
    }

    /** Sits the hint with its left edge at [left], centred on a strip of [stripHeight] from [stripTop]. */
    fun layoutAt(left: Int, stripTop: Int, stripHeight: Int) {
        x = left
        y = stripTop + (stripHeight - SIZE) / 2
    }

    fun isMouseOver(mouseX: Int, mouseY: Int): Boolean = inRect(mouseX, mouseY, x, y, SIZE, SIZE)

    fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val color = Common.UI.TEXT_DIM_COLOR

        // the body, its two buttons split by a line, and the wheel between them
        graphics.drawBorder(x, y, x + SIZE, y + SIZE, 1, color)

        val split = y + SIZE * 2 / 5
        graphics.fill(x, split, x + SIZE, split + 1, color)

        // as wide as keeps it centred: an odd body takes an odd wheel
        val wheelWidth = if (SIZE % 2 == 0) 2 else 3
        val wheelLeft = x + (SIZE - wheelWidth) / 2
        graphics.fill(wheelLeft, y + 2, wheelLeft + wheelWidth, split, Common.UI.TEXT_COLOR)

        if (isMouseOver(mouseX, mouseY)) {
            graphics.drawTooltipAtCursor(tooltip, mouseX, mouseY)
        }
    }

    companion object {
        /** The same everywhere it appears, whatever it sits beside. */
        const val SIZE: Int = 13
    }
}
