package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import net.minecraft.network.chat.Component
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight

class ConfigCategoryWidget(
    val categoryName: String,
    categoryFeatures: List<Feature> // your featureMap
) : Renderable, Focusable, NarratableEntry {

    override var focusedState: Boolean = false


    val categoryFeatureWidgets = mutableListOf<FeatureToggleWidget>()

    var x: Int = 0
    var y: Int = 0

    var width: Int = 200
    var height: Int = 0

    val categoryTitlePadding: Int = 3

    private val font get() = Minecraft.getInstance().font

    private val title: Component get() = Component.literal(categoryName)

    init {
        categoryFeatures.forEach { feature ->
            categoryFeatureWidgets.add(FeatureToggleWidget(feature))
        }
    }

    /** The narrowest column that still holds every feature row. */
    fun minWidth(): Int = categoryFeatureWidgets.maxOfOrNull { it.minWidth() } ?: 80

    /** The width at which no feature name in this column wraps. */
    fun naturalWidth(): Int = categoryFeatureWidgets.maxOfOrNull { it.naturalWidth() } ?: 0

    /** The tallest row any feature here needs at [columnWidth] with a checkbox of [rowHeight]. */
    fun neededRowHeight(columnWidth: Int, rowHeight: Int): Int {
        categoryFeatureWidgets.forEach { it.width = columnWidth }
        return categoryFeatureWidgets.maxOfOrNull { it.neededHeight(rowHeight) } ?: 0
    }

    /** Lays the title and rows out from [baseX], [baseY], with [rowGap] between rows. */
    fun init(baseX: Int, baseY: Int, columnWidth: Int, rowHeight: Int, rowGap: Int) {
        x = baseX
        y = baseY
        width = columnWidth

        val titleHeight = wrappedHeight(font, title, width) + categoryTitlePadding * 2

        // start below the title
        var currentY = y + titleHeight

        categoryFeatureWidgets.forEach {
            it.x = x
            it.y = currentY
            it.width = width
            it.layout(rowHeight)
            currentY += it.height + rowGap
        }

        // total height includes title
        height = currentY - y
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val lines = font.split(title, width)
        var titleY = y + categoryTitlePadding

        lines.forEach { line ->
            graphics.text(
                font,
                line,
                x + (width - font.width(line)) / 2,
                titleY,
                Common.UI.TEXT_COLOR,
                false
            )
            titleY += font.lineHeight
        }

        categoryFeatureWidgets.forEach {
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        categoryFeatureWidgets.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) return true
        }
        return false
    }


    override fun narrationPriority(): NarratableEntry.NarrationPriority {
       return NarratableEntry.NarrationPriority.NONE
    }

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {
    }
}
