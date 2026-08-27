package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.util.ScreenUtil.drawBorder

/**
 * The coloured swatches down the right hand side of the grid. Holding the mouse over one writes the
 * fact it stands for onto every plant at once, so the whole greenhouse can be read at a glance
 * without hovering the plants one by one.
 *
 * Only one fact shows at a time because a plant is one slot wide and a second line of text over it
 * would cover the plant it is describing.
 */
class HoverControls : Renderable, Focusable, HoverableContainer {

    override var hoveredElement: GuiEventListener? = null

    var x: Int = 0
    var y: Int = 0
    var width: Int = SWATCH_WIDTH
    var height: Int = 0

    override var focusedState: Boolean = false

    /** The fact the mouse is asking for, or null while it is not over a swatch. */
    var hoveredInfo: ElementWidget.HoverInfo? = null
        private set

    /** Sits the swatches against the right edge of a grid of [gridHeight] starting at [gridRight]. */
    fun layoutAgainstGrid(gridRight: Int, gridTop: Int, gridHeight: Int) {
        x = gridRight + GRID_PADDING
        y = gridTop
        width = SWATCH_WIDTH
        height = gridHeight
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        forEachSwatch { info, top, bottom ->
            graphics.fill(x, top, x + width, bottom, info.color)

            // the swatch the mouse is on is the one talking, an outline says so without a label
            if (info == hoveredInfo) {
                graphics.drawBorder(x, top, x + width, bottom, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)
            }
        }
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredInfo = null
        hoveredElement = null

        if (!isMouseOver(mouseX, mouseY)) return

        forEachSwatch { info, top, bottom ->
            if (mouseY.toInt() in top until bottom) {
                hoveredInfo = info
                hoveredElement = this
            }
        }
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x..(x + width) && mouseY.toInt() in y..(y + height)

    /** Walks the swatches top to bottom, handing out the vertical span each one covers. */
    private inline fun forEachSwatch(action: (ElementWidget.HoverInfo, top: Int, bottom: Int) -> Unit) {
        val infos = ElementWidget.HoverInfo.entries
        if (infos.isEmpty() || height <= 0) return

        infos.forEachIndexed { index, info ->
            // spans are cut from the running total so rounding never leaves a gap between swatches
            val top = y + height * index / infos.size
            val bottom = y + height * (index + 1) / infos.size

            action(info, top, bottom - SWATCH_SPACING)
        }
    }



    companion object {
        private const val SWATCH_WIDTH: Int = 10
        private const val SWATCH_SPACING: Int = 2
        private const val GRID_PADDING: Int = 6
    }
}
