package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import net.minecraft.world.item.Items
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.util.ScreenUtil.drawLine

class GridWidget(
    val layout: GreenhouseLayout,
    val slotSize: Int
) : Renderable, Focusable, NarratableEntry, HoverableContainer {

    /** How far a slot sits from the grid's corner: a slot and the line after it, counted that often. */
    private fun offsetOf(index: Int): Int = index * (slotSize + LINE_WIDTH)

    /** Every slot and the line after each, which is the whole grid across or down. */
    val gridSpan: Int get() = offsetOf(layout.size)

    private val slotWidgets = mutableListOf<SlotWidget>()
    private val elementWidgets = mutableListOf<ElementWidget>()

    var widgetX: Int = 0
    var widgetY: Int = 0
    var widgetWidth: Int = 300
    var widgetHeight: Int = 300

    override var focusedState: Boolean = false

    override var hoveredElement: GuiEventListener? = null

    /** The fact pinned on the hover controls, written over every plant while it is set. */
    var pinnedInfo: ElementWidget.HoverInfo? = null

    /** Plants placed since the last build, which arrive with a little pop. Cleared by [init]. */
    val justPlaced: MutableSet<GreenhouseElementInstance> = mutableSetOf()

    /** The slot under a point, or null off the grid. */
    fun slotAt(mouseX: Double, mouseY: Double): Pair<Int, Int>? {
        val step = slotSize + LINE_WIDTH
        val sx = (mouseX.toInt() - widgetX) / step
        val sy = (mouseY.toInt() - widgetY) / step
        if (mouseX < widgetX || mouseY < widgetY || sx !in 0 until layout.size || sy !in 0 until layout.size) return null
        return sx to sy
    }

    /** The screen rectangle a plant of [footprint] anchored at slot ([sx], [sy]) covers. */
    fun footprintRect(sx: Int, sy: Int, footprint: Footprint): IntArray = intArrayOf(
        widgetX + offsetOf(sx),
        widgetY + offsetOf(sy),
        widgetX + offsetOf(sx) + slotSize * footprint.width + (footprint.width - 1),
        widgetY + offsetOf(sy) + slotSize * footprint.height + (footprint.height - 1)
    )

    fun init() {
        slotWidgets.clear()
        elementWidgets.clear()

        for (x in 0 until layout.size) {
            for (y in 0 until layout.size) {


                val slot = layout.getSlot(x, y) ?: continue

                val widget = SlotWidget(slot)

                widget.widgetWidth = slotSize
                widget.widgetHeight = slotSize

                widget.widgetX = widgetX + offsetOf(x)
                widget.widgetY = widgetY + offsetOf(y)

                widget.init()

                slotWidgets.add(widget)
            }
        }

        layout.elementInstances.forEach { instance ->
            val widget = ElementWidget(instance)

            widget.padding = slotSize / 10

            val originX = instance.slot.x
            val originY = instance.slot.y

            widget.widgetX = widgetX + offsetOf(originX)
            widget.widgetY = widgetY + offsetOf(originY)
            // each axis swallows the lines between the slots it covers, and a crop is not always
            // square, so the axes cannot share one border count
            val footprint = instance.cropDef.footprint
            val widgetWidth = slotSize * footprint.width + (footprint.width - 1)
            val widgetHeight = slotSize * footprint.height + (footprint.height - 1)

            widget.width = widgetWidth
            widget.height = widgetHeight
            widget.waterEffect = layout.waterEffectAt(instance.slot)
            widget.init()
            // an id skyblock has no item for resolves to an empty stack, which draws nothing at all
            widget.renderedStack = instance.cropDef.displayItem?.let { ItemStack(it) }
                ?: instance.cropDef.skyblockId?.toItem()?.takeUnless { it.isEmpty }
                ?: ItemStack(Items.BARRIER)
            if (instance in justPlaced) widget.appearedAt = System.currentTimeMillis()
            widget.inPreset = layout.id.startsWith("preset_")
            elementWidgets.add(widget)
        }
        justPlaced.clear()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // draw grid lines
        for (i in 1 until layout.size) {
            // vertical
            graphics.drawLine(
                widgetX + offsetOf(i),
                widgetY,
                widgetX + offsetOf(i),
                widgetY + gridSpan,
                LINE_WIDTH,
                Common.UI.GRID_LINE_COLOR
            )

            // horizontal
            graphics.drawLine(
                widgetX,
                widgetY + offsetOf(i),
                widgetX + gridSpan,
                widgetY + offsetOf(i),
                LINE_WIDTH,
                Common.UI.GRID_LINE_COLOR
            )
        }

        slotWidgets.forEach {
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }

        elementWidgets.forEach {
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }

        // drawn after every plant so the text of one never ends up under the plant next to it
        pinnedInfo?.let { info ->
            elementWidgets.forEach { it.renderHoverButtonInfo(graphics, info) }
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        elementWidgets.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)){
                return true
            }
        }
        slotWidgets.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) {
                return true
            }
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= widgetX &&
                mouseX <= widgetX + widgetWidth &&
                mouseY >= widgetY &&
                mouseY <= widgetY + widgetHeight
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        elementWidgets.forEach { it.mouseMoved(mouseX, mouseY) }
        hoveredElement = elementWidgets.firstOrNull { it.isMouseOver(mouseX, mouseY) }
    }



    override fun narrationPriority(): NarratableEntry.NarrationPriority {
        return NarratableEntry.NarrationPriority.NONE
    }

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {}

    companion object {
        /** The line drawn between one slot and the next, and around the outside. */
        const val LINE_WIDTH: Int = 1

        /**
         * The largest slot that fits a grid into the room available, lines included. Divided once,
         * here, so nothing else lands a pixel out.
         */
        fun slotSizeFor(room: Int, slots: Int): Int = room / slots - LINE_WIDTH

        /** What a grid of [slots] at [slotSize] takes up, which is the span plus its closing line. */
        fun spanFor(slotSize: Int, slots: Int): Int = slots * (slotSize + LINE_WIDTH)
    }



}