package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common

/**
 * The coloured bookmarks down the right of the grid: picking one writes its fact onto every plant
 * at once. One at a time, since a second line of text would cover the plant it describes.
 */
class HoverControls : Renderable {

    /** The frame's right edge, and how far the bookmarks reach past it. */
    var x: Int = 0
        private set
    val width: Int = Bookmarks.REACH

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

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        bookmarks.mouseMoved(mouseX, mouseY)
    }

    /** Picks the clicked tab, or drops it when it was already picked. */
    fun mouseClicked(mouseButtonEvent: MouseButtonEvent): Boolean = bookmarks.mouseClicked(mouseButtonEvent)

    /** Moves the pick one tab along, wrapping. From nothing, down starts at the top and up at the bottom. */
    fun cycle(down: Boolean) {
        val infos = ElementWidget.HoverInfo.entries
        val current = selectedInfo

        selectedInfo = when (current) {
            null -> if (down) infos.first() else infos.last()
            else -> infos[(current.ordinal + (if (down) 1 else -1) + infos.size) % infos.size]
        }
    }

    companion object {
        /** The picked fact, kept across screens for as long as the game is running. */
        var selectedInfo: ElementWidget.HoverInfo? = null
            private set

        /** What the bookmarks take up beside the grid, gap included. */
        const val TOTAL_WIDTH: Int = Bookmarks.REACH + Common.UI.SPACING_LARGE
    }
}
