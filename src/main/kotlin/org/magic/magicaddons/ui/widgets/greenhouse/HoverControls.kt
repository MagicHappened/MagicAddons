package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.ui.HoverableContainer

/**
 * The coloured bookmarks down the right of the grid: picking one writes its fact onto every plant
 * at once. One at a time, since a second line of text would cover the plant it describes.
 */
class HoverControls : Renderable, Focusable, HoverableContainer {

    override var hoveredElement: GuiEventListener? = null

    /** The frame's right edge, and how far the bookmarks reach past it. */
    var x: Int = 0
    var y: Int = 0
    var width: Int = Bookmarks.REACH
    var height: Int = 0

    override var focusedState: Boolean = false

    /** The picked fact, kept across closing the screen: reopening is how a plot is looked at. */
    var selectedInfo: ElementWidget.HoverInfo?
        get() = lastPicked
        private set(value) {
            lastPicked = value
        }

    private val bookmarks = Bookmarks<ElementWidget.HoverInfo>(
        side = Bookmarks.Side.Right,
        label = { it.label },
        fill = { it.color },
        tooltip = { it.label },
        onPick = { info, _ -> selectedInfo = if (info == selectedInfo) null else info }
    ).apply { items = ElementWidget.HoverInfo.entries }

    /** Hangs the bookmarks off the right edge of a grid of [gridHeight] starting at [gridRight]. */
    fun layoutAgainstGrid(gridRight: Int, gridTop: Int, gridHeight: Int) {
        x = gridRight
        y = gridTop
        height = gridHeight
        bookmarks.layoutAlong(gridRight, gridTop, gridHeight)
    }

    /** The tabs themselves. Draw them before the grid's frame, which covers where they tuck in. */
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        bookmarks.selected = selectedInfo
        bookmarks.render(graphics)
    }

    /** The name of the tab under the mouse. Draw it last, over everything else. */
    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        bookmarks.renderTooltip(graphics, mouseX, mouseY)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        bookmarks.mouseMoved(mouseX, mouseY)
        hoveredElement = if (bookmarks.hovered != null) this else null
    }

    /** Picks the clicked tab, or drops it when it was already picked. */
    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean =
        bookmarks.mouseClicked(mouseButtonEvent)

    /** Moves the pick one tab along, wrapping. From nothing, down starts at the top and up at the bottom. */
    fun cycle(down: Boolean) {
        val infos = ElementWidget.HoverInfo.entries
        val current = selectedInfo

        selectedInfo = when (current) {
            null -> if (down) infos.first() else infos.last()
            else -> infos[(current.ordinal + (if (down) 1 else -1) + infos.size) % infos.size]
        }
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = bookmarks.isMouseOver(mouseX, mouseY)

    companion object {
        /** What was last picked, remembered across screens for as long as the game is running. */
        private var lastPicked: ElementWidget.HoverInfo? = null

        /** What the bookmarks take up beside the grid, gap included. */
        const val TOTAL_WIDTH: Int = Bookmarks.REACH + Common.UI.SPACING_LARGE
    }
}
