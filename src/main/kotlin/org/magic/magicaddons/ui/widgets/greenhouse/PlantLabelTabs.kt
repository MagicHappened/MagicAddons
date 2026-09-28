package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common

class PlantLabelTabs : Renderable {

    var x: Int = 0
        private set

    private val tabs = Bookmarks<PlantLabel>(
        side = Bookmarks.Side.Right,
        labelOf = { it.tabName },
        fillColorOf = { it.color },
        tooltipOf = { it.tabName },
        onPick = { label, _ -> selectedLabel = if (label == selectedLabel) null else label }
    ).apply { items = PlantLabel.entries }

    fun layoutAgainstGrid(gridRight: Int, gridTop: Int, gridHeight: Int) {
        x = gridRight
        tabs.layoutAlong(gridRight, gridTop, gridHeight)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        tabs.selectedItem = selectedLabel
        tabs.render(graphics)
    }

    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        tabs.renderTooltip(graphics, mouseX, mouseY)
    }

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        tabs.mouseMoved(mouseX, mouseY)
    }

    fun mouseClicked(mouseButtonEvent: MouseButtonEvent): Boolean = tabs.mouseClicked(mouseButtonEvent)

    fun cycleSelectedLabel(down: Boolean) {
        val labels = PlantLabel.entries
        val current = selectedLabel

        selectedLabel = when (current) {
            null -> if (down) labels.first() else labels.last()
            else -> labels[(current.ordinal + (if (down) 1 else -1) + labels.size) % labels.size]
        }
    }

    companion object {
        var selectedLabel: PlantLabel? = null
            private set

        const val TOTAL_WIDTH: Int = Bookmarks.TOTAL_REACH + Common.UI.SPACING_LARGE
    }
}
