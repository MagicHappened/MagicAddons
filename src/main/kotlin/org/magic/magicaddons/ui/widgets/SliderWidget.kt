package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel

/**
 * A whole-number slider: a thin track with a small button for a handle, dragged or clicked to a step
 * between [min] and [max]. Nothing is drawn when there is only one step to pick.
 */
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

    private var dragging: Boolean = false

    /** Whether there is more than one step, which is the only case the slider is drawn or used in. */
    val usable: Boolean get() = max > min

    /** Sets the steps this slider picks between, keeping the value inside them. */
    fun range(min: Int, max: Int) {
        this.min = min
        this.max = max
        value = value.coerceIn(min, maxOf(min, max))
    }

    /** Moves the handle without telling the owner, for a value that changed somewhere else. */
    fun show(step: Int) {
        value = step.coerceIn(min, maxOf(min, max))
    }

    fun set(step: Int) {
        val clamped = step.coerceIn(min, maxOf(min, max))
        if (clamped == value) return

        value = clamped
        onChange(clamped)
    }

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (!usable) return

        val trackY = y + height / 2
        graphics.fill(x, trackY - 1, x + width, trackY + 1, Common.UI.BORDER_COLOR)

        val handleX = handleX()
        val onHandle = mouseX in handleX until handleX + HANDLE_WIDTH && mouseY in y until y + height

        // the handle is a small button: washed under the mouse, shaded while it is being dragged
        graphics.drawButtonPanel(
            handleX, y,
            handleX + HANDLE_WIDTH, y + height,
            hovered = onHandle || dragging,
            pressed = dragging,
            fill = Common.UI.ACCENT_COLOR
        )
    }

    private fun handleX(): Int = x + ((value - min) * (width - HANDLE_WIDTH)) / (max - min)

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!usable) return false
        if (!isMouseOver(mouseX, mouseY)) return false

        dragging = true
        dragTo(mouseX)
        return true
    }

    fun mouseDragged(mouseX: Double): Boolean {
        if (!dragging) return false

        dragTo(mouseX)
        return true
    }

    fun mouseReleased(): Boolean {
        if (!dragging) return false

        dragging = false
        return true
    }

    /** A little above and below the track counts as the slider, so the handle is easy to catch. */
    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseY.toInt() in y - GRAB_SLACK..y + height + GRAB_SLACK && mouseX.toInt() in x..x + width

    private fun dragTo(mouseX: Double) {
        val along = ((mouseX - x) / width).coerceIn(0.0, 1.0)

        set(min + Math.round(along * (max - min)).toInt())
    }

    companion object {
        const val HEIGHT: Int = 10
        const val HANDLE_WIDTH: Int = 8

        private const val GRAB_SLACK: Int = 2
    }
}
