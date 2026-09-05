package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.TextField
import kotlin.math.roundToInt

/**
 * A number: a box on the right to type it, and a bar under the text to drag it. Dragging and the
 * wheel move by the setting's step; typing ignores it, so no number is out of reach.
 */
class IntSettingWidget(
    private val setting: IntSetting,
    overlays: OverlayContext
) : SettingWidget<Int>(setting, overlays) {

    override val controlWidth: Int = BOX_WIDTH
    override val controlHeight: Int = FIELD_HEIGHT

    private var dragging = false

    private val valueBox = TextField(BOX_WIDTH, FIELD_HEIGHT).also {
        it.setMaxLength(12)
        it.value = setting.value.toString()
        it.setResponder { typed ->
            // an empty box is somebody halfway through typing, not a request for zero
            typed.trim().toIntOrNull()?.let { number -> setting.value = number.coerceIn(setting.range) }
        }
    }

    override fun layoutControl() {
        valueBox.x = controlLeft()
        valueBox.y = controlTop()
        if (!valueBox.focused) valueBox.value = setting.value.toString()
    }

    override fun extraHeight(): Int = BAR_HEIGHT + KNOB_OVERHANG * 2

    private fun barTop(): Int = extraTop() + KNOB_OVERHANG
    private fun barLeft(): Int = extraLeft()
    private fun barWidth(): Int = extraWidth()

    private fun fraction(): Float {
        val span = (setting.range.last - setting.range.first).toFloat()
        if (span <= 0f) return 0f
        return ((setting.value - setting.range.first) / span).coerceIn(0f, 1f)
    }

    /** Reads a value off where along the bar the mouse is, snapped to the setting's step. */
    private fun setFromMouse(mouseX: Double) {
        val span = setting.range.last - setting.range.first
        if (span <= 0) return

        val along = ((mouseX - barLeft()) / barWidth().toDouble()).coerceIn(0.0, 1.0)
        val steps = (along * span / setting.step).roundToInt()
        setting.value = (setting.range.first + steps * setting.step).coerceIn(setting.range)

        if (!valueBox.focused) valueBox.value = setting.value.toString()
    }

    private fun overBar(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= barLeft() && mouseX <= barLeft() + barWidth() &&
                mouseY >= extraTop() && mouseY <= extraTop() + extraHeight()

    override fun renderControl(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        valueBox.render(graphics)
    }

    override fun renderExtra(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val filled = (barWidth() * fraction()).toInt()
        val top = barTop()

        graphics.fill(barLeft(), top, barLeft() + barWidth(), top + BAR_HEIGHT, Common.UI.FIELD_COLOR)
        graphics.fill(barLeft(), top, barLeft() + filled, top + BAR_HEIGHT, Common.UI.ACCENT_COLOR)

        // the knob rides the end of the filled part, kept inside the track at either extreme
        val knobX = (barLeft() + filled - KNOB_WIDTH / 2).coerceIn(barLeft(), barLeft() + barWidth() - KNOB_WIDTH)
        graphics.fill(knobX, top - KNOB_OVERHANG, knobX + KNOB_WIDTH, top + BAR_HEIGHT + KNOB_OVERHANG, Common.UI.TEXT_COLOR)
    }

    override fun controlClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (valueBox.mouseClicked(event, doubled)) return true

        // a click anywhere else lets the box go, so what was typed is committed
        commitTypedValue()

        if (event.button() == 0 && overBar(event.x, event.y)) {
            dragging = true
            setFromMouse(event.x)
            return true
        }
        return false
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (!dragging) return super.mouseDragged(event, dragX, dragY)
        setFromMouse(event.x)
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (!dragging) return super.mouseReleased(event)
        dragging = false
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true
        if (!overBar(mouseX, mouseY) || scrollY == 0.0) return false

        val direction = if (scrollY > 0) 1 else -1
        setting.value = (setting.value + direction * setting.step).coerceIn(setting.range)
        if (!valueBox.focused) valueBox.value = setting.value.toString()
        return true
    }

    override fun dropFocus() {
        commitTypedValue()
        super.dropFocus()
    }

    override fun charTyped(event: CharacterEvent): Boolean =
        valueBox.charTyped(event) || super.charTyped(event)

    override fun keyPressed(event: KeyEvent): Boolean {
        if (valueBox.focused && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            commitTypedValue()
            return true
        }
        return valueBox.keyPressed(event) || super.keyPressed(event)
    }

    /** Takes the typed number, or puts the value back. Clamped to the range, never to the step. */
    private fun commitTypedValue() {
        if (!valueBox.focused) return
        valueBox.focused = false

        val typed = valueBox.value.trim().toIntOrNull()
        if (typed != null) setting.value = typed.coerceIn(setting.range)
        valueBox.value = setting.value.toString()
    }

    private companion object {
        /** Room for a five figure number and a little air. */
        const val BOX_WIDTH: Int = 46
        const val BAR_HEIGHT: Int = 4
        const val KNOB_WIDTH: Int = 3
        const val KNOB_OVERHANG: Int = 2
    }
}
