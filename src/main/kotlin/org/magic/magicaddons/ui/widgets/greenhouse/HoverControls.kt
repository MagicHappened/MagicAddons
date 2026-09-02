package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.util.ScreenUtil.drawBorder

/**
 * The coloured swatches beside the grid: picking one writes its fact onto every plant at once.
 *
 * Picked rather than hovered, so the mouse is free to be somewhere else, and one at a time, since a
 * second line of text would cover the plant it describes.
 */
class HoverControls : Renderable, Focusable, HoverableContainer {

    override var hoveredElement: GuiEventListener? = null

    var x: Int = 0
    var y: Int = 0
    var width: Int = SWATCH_WIDTH
    var height: Int = 0

    override var focusedState: Boolean = false

    /** The picked fact, kept across closing the screen: reopening is how a plot is looked at. */
    var selectedInfo: ElementWidget.HoverInfo?
        get() = lastPicked
        private set(value) {
            lastPicked = value
        }

    /** The swatch under the mouse, which is only ever drawn differently, never read from. */
    private var hoveredInfo: ElementWidget.HoverInfo? = null

    /** Sits the swatches against the right edge of a grid of [gridHeight] starting at [gridRight]. */
    fun layoutAgainstGrid(gridRight: Int, gridTop: Int, gridHeight: Int) {
        x = gridRight + Common.UI.SPACING_LARGE
        y = gridTop
        width = SWATCH_WIDTH
        height = gridHeight
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        forEachSwatch { info, top, bottom ->
            graphics.fill(x, top, x + width, bottom, info.color)

            // the picked swatch wears an outline; the one merely hovered is only lightened
            if (info == selectedInfo) {
                graphics.drawBorder(x, top, x + width, bottom, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)
            } else if (info == hoveredInfo) {
                graphics.fill(x, top, x + width, bottom, HOVER_WASH)
            }
        }
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredInfo = swatchAt(mouseX, mouseY)
        hoveredElement = if (hoveredInfo != null) this else null
    }

    /** Picks the clicked swatch, or drops it when it was already picked. */
    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        val clicked = swatchAt(mouseButtonEvent.x, mouseButtonEvent.y) ?: return false

        selectedInfo = if (clicked == selectedInfo) null else clicked

        return true
    }

    /** Moves the pick one swatch along, wrapping. From nothing, down starts at the top and up at the bottom. */
    fun cycle(down: Boolean) {
        val infos = ElementWidget.HoverInfo.entries
        val current = selectedInfo

        selectedInfo = when (current) {
            null -> if (down) infos.first() else infos.last()
            else -> infos[(current.ordinal + (if (down) 1 else -1) + infos.size) % infos.size]
        }
    }

    /** Which swatch, if any, sits under a point. */
    private fun swatchAt(mouseX: Double, mouseY: Double): ElementWidget.HoverInfo? {
        if (!isMouseOver(mouseX, mouseY)) return null

        var found: ElementWidget.HoverInfo? = null

        forEachSwatch { info, top, bottom ->
            if (mouseY.toInt() in top until bottom) found = info
        }

        return found
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until (x + width) && mouseY.toInt() in y until (y + height)

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
        /** What was last picked, remembered across screens for as long as the game is running. */
        private var lastPicked: ElementWidget.HoverInfo? = null

        /** Laid over the swatch the mouse is on, so it lifts rather than changes colour. */
        private const val HOVER_WASH: Int = 0x40FFFFFF

        private const val SWATCH_WIDTH: Int = 10
        private const val SWATCH_SPACING: Int = Common.UI.SPACING_SMALL

        /** What the swatches take up beside the grid, gap included. */
        const val TOTAL_WIDTH: Int = SWATCH_WIDTH + Common.UI.SPACING_LARGE
    }
}
