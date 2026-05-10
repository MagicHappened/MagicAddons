package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.util.ScreenUtil.drawLine

class GreenhouseGridWidget(
    val layout: GreenhouseLayout,
    val slotSize: Int
) : Renderable, GuiEventListener, NarratableEntry {

    private val slotWidgets = mutableListOf<GreenhouseSlotWidget>()
    private val elementWidgets = mutableListOf<GreenhouseElementWidget>()

    var widgetX: Int = 0
    var widgetY: Int = 0
    var widgetWidth: Int = 300
    var widgetHeight: Int = 300

    private var focused: Boolean = false

    fun init() {
        slotWidgets.clear()
        elementWidgets.clear()

        for (x in 0 until layout.size) {
            for (y in 0 until layout.size) {


                val slot = layout.getSlot(x, y) ?: continue

                val widget = GreenhouseSlotWidget(slot)

                widget.widgetWidth = slotSize
                widget.widgetHeight = slotSize

                widget.widgetX = widgetX + x * slotSize + x
                widget.widgetY = widgetY + y * slotSize + y

                widget.init()

                slotWidgets.add(widget)
            }
        }

        layout.elementInstances.forEach { instance ->
            val def = CropRegistry.all.find { instance.elementId == (it.skyblockId?.id ?: it.name) } ?: return@forEach
            val widget = GreenhouseElementWidget(instance,def)

            widget.padding = slotSize / 10

            val originX = instance.slot.x
            val originY = instance.slot.y

            widget.widgetX = widgetX + originX * slotSize + originX
            widget.widgetY = widgetY + originY * slotSize + originY
            //todo add footprint scaling for width and height
            widget.width = slotSize
            widget.height = slotSize

            widget.renderedStack = def.skyblockId?.toItem() ?: ItemStack(Items.BARRIER)
            elementWidgets.add(widget)
        }

    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // draw slots
        slotWidgets.forEach {
            it.render(graphics, mouseX, mouseY, delta)
        }

        // draw grid lines
        for (i in 1 until layout.size) {
            // vertical
            graphics.drawLine(
                widgetX  + i * slotSize + i,
                widgetY,
                widgetX + i * slotSize + i,
                widgetY + layout.size * slotSize + layout.size,
                1,
                0xFF0683c1.toInt()
            )

            // horizontal
            graphics.drawLine(
                widgetX,
                widgetY + i * slotSize + i,
                widgetX + layout.size * slotSize + layout.size,
                widgetY + i * slotSize + i,
                1,
                0xFF0683c1.toInt()
            )
        }
        elementWidgets.forEach {
            it.render(graphics, mouseX, mouseY, delta)
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
        elementWidgets.forEach {
            it.mouseMoved(mouseX, mouseY)
        }
    }

    override fun isFocused(): Boolean = focused

    override fun setFocused(focused: Boolean) {
        this.focused = focused
    }

    override fun narrationPriority(): NarratableEntry.NarrationPriority {
        return NarratableEntry.NarrationPriority.NONE
    }

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {}


}