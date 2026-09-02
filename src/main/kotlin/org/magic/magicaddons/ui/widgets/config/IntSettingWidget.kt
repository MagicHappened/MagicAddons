package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import kotlin.math.roundToInt

/**
 * A number picked either way round: dragged along a bar, or typed into the box beside its name.
 * Dragging and the wheel move by the setting's step; typing ignores it, so no number is out of reach.
 */
class IntSettingWidget(
    private val setting: IntSetting
) : SettingWidget<Int>(setting) {

    override val hasChildren: Boolean = false
    override val childrenWidgets: MutableList<SettingWidget<*>> = mutableListOf()

    /** Room for a five figure number and a little air. */
    private val boxWidth: Int = 46

    private val barHeight: Int = 5
    private val knobWidth: Int = 3

    private var dragging: Boolean = false

    private val valueBox by lazy {
        EditBox(
            Minecraft.getInstance().font,
            boxWidth,
            rowHeight(),
            Component.literal(setting.displayName)
        ).also {
            it.setMaxLength(12)
            it.value = setting.value.toString()
        }
    }

    /** The top half of the box, which the name and the number share. */
    private fun rowHeight(): Int = height / 2 - borderSize * 2

    private fun barTop(): Int = y + height / 2 + (height / 2 - barHeight) / 2
    private fun barLeft(): Int = x + textXPad
    private fun barWidth(): Int = width - textXPad * 2

    override fun layout() {
        valueBox.x = x + width - borderSize - textXPad - boxWidth
        valueBox.y = y + borderSize + textXPad / 2
        valueBox.width = boxWidth
        valueBox.height = rowHeight()

        if (!valueBox.isFocused) valueBox.value = setting.value.toString()

        valueBox.setResponder { typed ->
            // an empty box is somebody halfway through typing, not a request for zero
            typed.trim().toIntOrNull()?.let { setting.value = it.coerceIn(setting.range) }
        }
    }

    /** Where the value sits between the ends of its range, as a fraction of the bar. */
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

        if (!valueBox.isFocused) valueBox.value = setting.value.toString()
    }

    private fun overBar(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= barLeft() && mouseX <= barLeft() + barWidth() &&
                mouseY >= y + height / 2 && mouseY <= y + height

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val font = Minecraft.getInstance().font

        graphics.fill(x, y, x + width, y + height, backgroundColor)
        graphics.drawBorder(x, y, x + width, y + height, borderSize, borderColor)

        graphics.text(
            font,
            Component.literal(setting.displayName),
            x + textXPad + borderSize,
            y + borderSize + textXPad / 2 + (rowHeight() - font.lineHeight) / 2,
            Common.UI.TEXT_COLOR,
            false
        )

        valueBox.extractRenderState(graphics, mouseX, mouseY, delta)

        val filled = (barWidth() * fraction()).toInt()

        graphics.fill(barLeft(), barTop(), barLeft() + barWidth(), barTop() + barHeight, TRACK_COLOR)
        graphics.fill(barLeft(), barTop(), barLeft() + filled, barTop() + barHeight, FILL_COLOR)

        // the knob rides the end of the filled part, kept inside the track at either extreme
        val knobX = (barLeft() + filled - knobWidth / 2)
            .coerceIn(barLeft(), barLeft() + barWidth() - knobWidth)

        graphics.fill(knobX, barTop() - 2, knobX + knobWidth, barTop() + barHeight + 2, borderColor)

        renderDetail(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (valueBox.mouseClicked(mouseButtonEvent, doubled)) {
            valueBox.isFocused = true
            return true
        }

        // clicking anywhere else lets the box go, so what was typed is committed and the keyboard
        // stops being eaten by a setting the player has moved on from
        commitTypedValue()

        if (mouseButtonEvent.button() == 0 && overBar(mouseButtonEvent.x, mouseButtonEvent.y)) {
            dragging = true
            setFromMouse(mouseButtonEvent.x)
            return true
        }

        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseDragged(
        mouseButtonEvent: MouseButtonEvent,
        dragX: Double,
        dragY: Double
    ): Boolean {
        if (!dragging) return false

        setFromMouse(mouseButtonEvent.x)
        return true
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        if (!dragging) return false

        dragging = false
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        if (scrollY == 0.0) return false

        val direction = if (scrollY > 0) 1 else -1

        setting.value = (setting.value + direction * setting.step).coerceIn(setting.range)

        if (!valueBox.isFocused) valueBox.value = setting.value.toString()

        return true
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (!valueBox.isFocused) return false

        return valueBox.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (!valueBox.isFocused) return false

        if (keyEvent.key() == GLFW.GLFW_KEY_ENTER || keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
            commitTypedValue()
            return true
        }

        return valueBox.keyPressed(keyEvent)
    }

    /** Takes the typed number, or puts the value back. Clamped to the range, never to the step. */
    private fun commitTypedValue() {
        if (!valueBox.isFocused) return

        valueBox.isFocused = false

        val typed = valueBox.value.trim().toIntOrNull()

        if (typed == null) {
            valueBox.value = setting.value.toString()
            return
        }

        setting.value = typed.coerceIn(setting.range)
        valueBox.value = setting.value.toString()
    }

    private companion object {
        const val TRACK_COLOR: Int = 0xFF2A2A2A.toInt()
        const val FILL_COLOR: Int = 0xFF4C8FBF.toInt()
    }
}
