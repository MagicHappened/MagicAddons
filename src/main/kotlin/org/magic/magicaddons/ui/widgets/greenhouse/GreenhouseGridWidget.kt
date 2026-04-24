package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.util.ScreenUtil.drawLine

class GreenhouseGridWidget(
    val grid: GreenhouseGrid,
    val gridSize: Int,
    val slotSize: Int
) : Renderable, GuiEventListener, NarratableEntry {

    private val slotWidgets = mutableListOf<GreenhouseSlotWidget>()

    var widgetX: Int = 0
    var widgetY: Int = 0
    var widgetWidth: Int = 300
    var widgetHeight: Int = 300

    private var focused: Boolean = false

    fun init() {
        slotWidgets.clear()

        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {

                val slot = grid.getSlot(x, y) ?: continue

                val widget = GreenhouseSlotWidget(slot)

                widget.widgetWidth = slotSize
                widget.widgetHeight = slotSize

                widget.widgetX = widgetX + x * slotSize
                widget.widgetY = widgetY + y * slotSize

                widget.init()

                slotWidgets.add(widget)
            }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // draw slots
        slotWidgets.forEach {
            it.render(graphics, mouseX, mouseY, delta)
        }

        // draw grid lines (clean + fixed)
        for (i in 1 until gridSize) {
            // vertical
            graphics.drawLine(
                widgetX  + i * slotSize,
                widgetY ,
                widgetX + i * slotSize,
                widgetY + gridSize * slotSize,
                2,
                0xFF000000.toInt()
            )

            // horizontal
            graphics.drawLine(
                widgetX,
                widgetY + i * slotSize,
                widgetX + gridSize * slotSize,
                widgetY + i * slotSize,
                2,
                0xFF000000.toInt()
            )
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        for (widget in slotWidgets) {
            if (widget.mouseClicked(mouseButtonEvent, doubled)) {
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

    override fun isFocused(): Boolean = focused

    override fun setFocused(focused: Boolean) {
        this.focused = focused
    }

    override fun narrationPriority(): NarratableEntry.NarrationPriority {
        return NarratableEntry.NarrationPriority.NONE
    }

    override fun updateNarration(narrationElementOutput: NarrationElementOutput) {}
}