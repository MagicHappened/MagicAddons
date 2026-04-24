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
    val gridSize: Int = 10
) : Renderable, GuiEventListener, NarratableEntry {

    private val slotWidgets = mutableListOf<GreenhouseSlotWidget>()

    var widgetX: Int = 0
    var widgetY: Int = 0
    var widgetWidth: Int = 300
    var widgetHeight: Int = 300

    var borderPadding: Int = 6
    var slotSize: Int = 0

    private var focused: Boolean = false

    fun init() {
        slotWidgets.clear()

        val innerSize = widgetWidth - borderPadding * 2
        slotSize = innerSize / gridSize

        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {

                val slot = grid.getSlot(x, y) ?: continue

                val widget = GreenhouseSlotWidget(slot)

                widget.widgetWidth = slotSize
                widget.widgetHeight = slotSize

                widget.widgetX = widgetX + borderPadding + x * slotSize
                widget.widgetY = widgetY + borderPadding + y * slotSize

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
                widgetX + borderPadding + i * slotSize,
                widgetY + borderPadding,
                widgetX + borderPadding + i * slotSize,
                widgetY + borderPadding + gridSize * slotSize,
                1,
                0xFFFFFFFF.toInt()
            )

            // horizontal
            graphics.drawLine(
                widgetX + borderPadding,
                widgetY + borderPadding + i * slotSize,
                widgetX + borderPadding + gridSize * slotSize,
                widgetY + borderPadding + i * slotSize,
                1,
                0xFFFFFFFF.toInt()
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