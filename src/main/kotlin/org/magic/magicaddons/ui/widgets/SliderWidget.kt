package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel

class SliderWidget(val onChange: (Int) -> Unit) {

    var x: Int = 0
    var y: Int = 0
    var width: Int = 0

    val height: Int = HEIGHT

    var min: Int = 0
        private set

    var max: Int = 0
        private set

    var value: Int = 0
        private set

    private var isDragging: Boolean = false

    val isUsable: Boolean get() = max > min

    fun setRange(min: Int, max: Int) {
        this.min = min
        this.max = max
        value = value.coerceIn(min, maxOf(min, max))
    }

    fun setValueWithoutNotifying(step: Int) {
        value = step.coerceIn(min, maxOf(min, max))
    }

    fun setValue(step: Int) {
        val clamped = step.coerceIn(min, maxOf(min, max))
        if (clamped == value) return

        value = clamped
        onChange(clamped)
    }

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (!isUsable) return

        val trackY = y + height / 2
        graphics.fill(x, trackY - 1, x + width, trackY + 1, Common.UI.BORDER_COLOR)

        val handleX = handleX()
        val isHandleHovered = mouseX in handleX until handleX + HANDLE_WIDTH && mouseY in y until y + height

        graphics.drawButtonPanel(
            handleX, y,
            handleX + HANDLE_WIDTH, y + height,
            hovered = isHandleHovered || isDragging,
            pressed = isDragging,
            fill = Common.UI.ACCENT_COLOR
        )
    }

    private fun handleX(): Int = x + ((value - min) * (width - HANDLE_WIDTH)) / (max - min)

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!isUsable) return false
        if (!isMouseOver(mouseX, mouseY)) return false

        isDragging = true
        setValueFromMouse(mouseX)
        return true
    }

    fun mouseDragged(mouseX: Double): Boolean {
        if (!isDragging) return false

        setValueFromMouse(mouseX)
        return true
    }

    fun mouseReleased(): Boolean {
        if (!isDragging) return false

        isDragging = false
        return true
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseY.toInt() in y - GRAB_MARGIN..y + height + GRAB_MARGIN && mouseX.toInt() in x..x + width

    private fun setValueFromMouse(mouseX: Double) {
        val trackFraction = ((mouseX - x) / width).coerceIn(0.0, 1.0)

        setValue(min + Math.round(trackFraction * (max - min)).toInt())
    }

    companion object {
        const val HEIGHT: Int = 10
        const val HANDLE_WIDTH: Int = 8

        private const val GRAB_MARGIN: Int = 2
    }
}
