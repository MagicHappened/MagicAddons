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

    /** Quarter turns clockwise the picture is given; the slots underneath never move. */
    var turns: Int = 0

    /** Where slot ([x], [y]) is drawn, as a cell of the turned picture. */
    private fun turned(x: Int, y: Int): Pair<Int, Int> {
        val last = layout.size - 1
        return when (Math.floorMod(turns, 4)) {
            1 -> (last - y) to x
            2 -> (last - x) to (last - y)
            3 -> y to (last - x)
            else -> x to y
        }
    }

    /** The slot drawn at cell ([cx], [cy]) of the turned picture. */
    private fun unturned(cx: Int, cy: Int): Pair<Int, Int> {
        val last = layout.size - 1
        return when (Math.floorMod(turns, 4)) {
            1 -> cy to (last - cx)
            2 -> (last - cx) to (last - cy)
            3 -> (last - cy) to cx
            else -> cx to cy
        }
    }

    /** The slot under a point, or null off the grid. */
    fun slotAt(mouseX: Double, mouseY: Double): Pair<Int, Int>? {
        val step = slotSize + LINE_WIDTH
        val cx = (mouseX.toInt() - widgetX) / step
        val cy = (mouseY.toInt() - widgetY) / step
        if (mouseX < widgetX || mouseY < widgetY || cx !in 0 until layout.size || cy !in 0 until layout.size) return null
        return unturned(cx, cy)
    }

    /** The screen rectangle a plant [width] by [height] slots anchored at slot ([sx], [sy]) covers. */
    fun cellRect(sx: Int, sy: Int, width: Int, height: Int): IntArray {
        val (ax, ay) = turned(sx, sy)
        val (bx, by) = turned(sx + width - 1, sy + height - 1)
        val left = minOf(ax, bx)
        val top = minOf(ay, by)
        val across = maxOf(ax, bx) - left + 1
        val down = maxOf(ay, by) - top + 1
        return intArrayOf(
            widgetX + offsetOf(left),
            widgetY + offsetOf(top),
            widgetX + offsetOf(left) + slotSize * across + (across - 1),
            widgetY + offsetOf(top) + slotSize * down + (down - 1)
        )
    }

    fun footprintRect(sx: Int, sy: Int, footprint: Footprint): IntArray = cellRect(sx, sy, footprint.width, footprint.height)

    fun init() {
        slotWidgets.clear()
        elementWidgets.clear()

        for (x in 0 until layout.size) {
            for (y in 0 until layout.size) {


                val slot = layout.getSlot(x, y) ?: continue

                val widget = SlotWidget(slot)

                widget.widgetWidth = slotSize
                widget.widgetHeight = slotSize

                val (cx, cy) = turned(x, y)
                widget.widgetX = widgetX + offsetOf(cx)
                widget.widgetY = widgetY + offsetOf(cy)

                widget.init()

                slotWidgets.add(widget)
            }
        }

        layout.elementInstances.forEach { instance ->
            val widget = ElementWidget(instance)

            widget.padding = slotSize / 10

            // each axis swallows the lines between the slots it covers, and a crop is not always
            // square, so the axes cannot share one border count; turned, a wide crop may stand tall
            val footprint = instance.cropDef.footprint
            val rect = cellRect(instance.slot.x, instance.slot.y, footprint.width, footprint.height)

            widget.widgetX = rect[0]
            widget.widgetY = rect[1]
            widget.width = rect[2] - rect[0]
            widget.height = rect[3] - rect[1]
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
        slotWidgets.forEach {
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }

        // the lines live in the pixel between two slots, drawn over the soil so no slot covers them
        // and under the plants so a wide plant covers them
        for (i in 1 until layout.size) {
            val at = offsetOf(i) - LINE_WIDTH
            graphics.fill(widgetX + at, widgetY, widgetX + at + LINE_WIDTH, widgetY + gridSpan, Common.UI.GRID_LINE_COLOR)
            graphics.fill(widgetX, widgetY + at, widgetX + gridSpan, widgetY + at + LINE_WIDTH, Common.UI.GRID_LINE_COLOR)
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