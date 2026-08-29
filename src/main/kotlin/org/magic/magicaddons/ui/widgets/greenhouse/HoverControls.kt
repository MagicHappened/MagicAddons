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
 * The coloured swatches down the right hand side of the grid. Picking one writes the fact it stands
 * for onto every plant at once, so the whole greenhouse can be read at a glance without going over
 * the plants one by one.
 *
 * Picked rather than hovered: a fact worth reading across a hundred plants is worth reading with
 * the mouse somewhere else, and holding still on a swatch to keep it on screen meant never being
 * able to look at anything the swatch was describing. Clicking a swatch turns its fact on, clicking
 * another swaps to it, and clicking the one already on turns it off again.
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

    /**
     * The fact that has been picked, or null when none is.
     *
     * Kept where the screen cannot take it away: closing the greenhouse and opening it again is
     * how a player looks at their plot, not how they change their mind about what they wanted to
     * see, so the pick outlives the screen it was made on.
     */
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

            // the picked swatch is the one talking and wears the outline that says so. The one
            // merely under the mouse is only lightened, so a swatch about to be picked looks
            // different from the swatch already doing the talking
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

    /**
     * Picks the swatch that was clicked, or drops it when it was already the one picked, so the
     * same click both turns a fact on and takes it away again.
     */
    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        val clicked = swatchAt(mouseButtonEvent.x, mouseButtonEvent.y) ?: return false

        selectedInfo = if (clicked == selectedInfo) null else clicked

        return true
    }

    /**
     * Moves the pick one swatch down or up the list, wrapping at the ends.
     *
     * From nothing picked, down starts at the top of the list and up at the bottom, so the wheel
     * reaches every fact from either direction within a notch or two.
     */
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
