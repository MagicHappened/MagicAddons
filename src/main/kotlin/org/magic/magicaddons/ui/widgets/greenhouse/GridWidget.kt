package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.util.ScreenUtil.drawLine

class GridWidget(
    val layout: GreenhouseLayout,
    val slotSize: Int
) : Renderable, GuiEventListener, NarratableEntry, HoverableContainer {

    private val slotWidgets = mutableListOf<SlotWidget>()
    private val elementWidgets = mutableListOf<ElementWidget>()

    var widgetX: Int = 0
    var widgetY: Int = 0
    var widgetWidth: Int = 300
    var widgetHeight: Int = 300

    @JvmField
    var isFocused: Boolean = false

    override var hoveredElement: GuiEventListener? = null

    /** The fact pinned on the hover controls, written over every plant while it is set. */
    var pinnedInfo: ElementWidget.HoverInfo? = null

    fun init() {
        slotWidgets.clear()
        elementWidgets.clear()

        for (x in 0 until layout.size) {
            for (y in 0 until layout.size) {


                val slot = layout.getSlot(x, y) ?: continue

                val widget = SlotWidget(slot)

                widget.widgetWidth = slotSize
                widget.widgetHeight = slotSize

                widget.widgetX = widgetX + x * slotSize + x
                widget.widgetY = widgetY + y * slotSize + y

                widget.init()

                slotWidgets.add(widget)
            }
        }

        layout.elementInstances.forEach { instance ->
            val widget = ElementWidget(instance)

            widget.padding = slotSize / 10

            val originX = instance.slot.x
            val originY = instance.slot.y

            widget.widgetX = widgetX + originX * slotSize + originX
            widget.widgetY = widgetY + originY * slotSize + originY
            //hopefully work?

            val bordersSize = (instance.cropDef.footprint.width - 1) * 1
            val widgetWidth = slotSize * instance.cropDef.footprint.width + bordersSize
            val widgetHeight = slotSize * instance.cropDef.footprint.height + bordersSize

            widget.width = widgetWidth
            widget.height = widgetHeight
            widget.init()
            widget.renderedStack = instance.cropDef.skyblockId?.toItem() ?: ItemStack(Items.BARRIER)
            elementWidgets.add(widget)
        }

    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // draw grid lines
        for (i in 1 until layout.size) {
            // vertical
            graphics.drawLine(
                widgetX  + i * slotSize + i,
                widgetY,
                widgetX + i * slotSize + i,
                widgetY + layout.size * slotSize + layout.size,
                1,
                0x800683c1.toInt()
            )

            // horizontal
            graphics.drawLine(
                widgetX,
                widgetY + i * slotSize + i,
                widgetX + layout.size * slotSize + layout.size,
                widgetY + i * slotSize + i,
                1,
                0x800683c1.toInt()
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
        hoveredElement = null
        elementWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
            if (hoveredElement != null) return@forEach
            if (it.isMouseOver(mouseX, mouseY)){
                hoveredElement = it
            }
        }

    }

    override fun isFocused(): Boolean = isFocused

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun narrationPriority(): NarratableEntry.NarrationPriority {
        return NarratableEntry.NarrationPriority.NONE
    }

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {}



}