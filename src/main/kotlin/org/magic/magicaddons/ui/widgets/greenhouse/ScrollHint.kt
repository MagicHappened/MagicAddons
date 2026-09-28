package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.inRect

class ScrollHint(var tooltip: String) {

    var x: Int = 0
    var y: Int = 0

    fun placeBesideControl(controlRight: Int, controlTop: Int, controlHeight: Int) {
        x = controlRight + Common.UI.SPACING
        y = controlTop + (controlHeight - SIZE) / 2
    }

    fun placeInStrip(left: Int, stripTop: Int, stripHeight: Int) {
        x = left
        y = stripTop + (stripHeight - SIZE) / 2
    }

    fun isMouseOver(mouseX: Int, mouseY: Int): Boolean = inRect(mouseX, mouseY, x, y, SIZE, SIZE)

    fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val color = Common.UI.TEXT_DIM_COLOR

        graphics.drawBorder(x, y, x + SIZE, y + SIZE, 1, color)

        val buttonLineY = y + SIZE * 2 / 5
        graphics.fill(x, buttonLineY, x + SIZE, buttonLineY + 1, color)

        val wheelLeft = x + (SIZE - WHEEL_WIDTH) / 2
        graphics.fill(wheelLeft, y + 2, wheelLeft + WHEEL_WIDTH, buttonLineY, Common.UI.TEXT_COLOR)

        if (isMouseOver(mouseX, mouseY)) {
            graphics.drawTooltipAtCursor(tooltip, mouseX, mouseY)
        }
    }

    companion object {
        const val SIZE: Int = 13
        private const val WHEEL_WIDTH: Int = 3
    }
}
