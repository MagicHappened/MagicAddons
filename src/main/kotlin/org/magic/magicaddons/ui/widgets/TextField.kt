package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.util.ScreenUtil.modText
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawField
import org.magic.magicaddons.util.ScreenUtil.inRect

/**
 * A one line text field drawn in this mod's own look. A vanilla EditBox holds the text and the
 * caret and takes the typing; only the drawing is done here.
 */
class TextField(
    var width: Int,
    var height: Int,
    var hint: Component? = null
) {
    var x: Int = 0
    var y: Int = 0

    private val font = Minecraft.getInstance().font

    private val box = EditBox(font, 0, 0, width, font.lineHeight, Component.empty()).apply {
        setBordered(false)
        setCanLoseFocus(true)
        setMaxLength(256)
    }

    var value: String
        get() = box.value
        set(text) {
            box.value = text
        }

    /** Thinly framed in the panel colour while it does not have the keyboard, for a field standing alone. */
    var framed: Boolean = false

    var focused: Boolean
        get() = box.isFocused
        set(on) {
            box.isFocused = on
        }

    fun setResponder(responder: (String) -> Unit) = box.setResponder(responder)

    fun setMaxLength(length: Int) = box.setMaxLength(length)

    /** Space between the frame and the text. */
    private val inset = Common.UI.FIELD_INSET + Common.UI.BORDER_SIZE

    private fun textLeft(): Int = x + inset
    private fun textTop(): Int = y + (height - font.lineHeight) / 2
    private fun room(): Int = (width - inset * 2).coerceAtLeast(1)

    /** Puts the box over the text so a click lands the caret where it was aimed. */
    private fun place() {
        box.x = textLeft()
        box.y = textTop()
        box.width = room()
        box.height = font.lineHeight
    }

    fun render(graphics: GuiGraphicsExtractor) {
        place()
        val frameSize = if (framed) Common.UI.CONTROL_BORDER_SIZE else Common.UI.BORDER_SIZE
        graphics.drawField(x, y, x + width, y + height, focused, frameSize)
        if (framed && !focused) graphics.drawBorder(x, y, x + width, y + height, frameSize, Common.UI.BORDER_COLOR)

        val text = value
        val caretX = font.width(text.substring(0, box.cursorPosition.coerceIn(0, text.length)))

        // while typing the text slides left only as far as it must for the caret to stay in view;
        // at rest it shows its start
        val shift = if (focused) (caretX - room() + 1).coerceAtLeast(0) else 0
        val left = textLeft() - shift

        graphics.enableScissor(textLeft(), y, textLeft() + room(), y + height)

        if (text.isEmpty() && !focused) {
            hint?.let { graphics.modText(font, it, left, textTop(), Common.UI.DISABLED_TEXT_COLOR) }
        } else {
            graphics.modText(font, Component.literal(text), left, textTop(), Common.UI.TEXT_COLOR)
        }

        if (focused && System.currentTimeMillis() / CARET_BLINK_MS % 2 == 0L) {
            graphics.fill(left + caretX, textTop() - 1, left + caretX + 1, textTop() + font.lineHeight, Common.UI.TEXT_COLOR)
        }

        graphics.disableScissor()
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = inRect(mouseX, mouseY, x, y, width, height)

    /** Focuses the field when the click is on it and drops focus otherwise; true when it was on it. */
    fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val inside = isMouseOver(event.x, event.y)
        focused = inside

        if (inside) {
            place()
            box.mouseClicked(event, doubled)
        }
        return inside
    }

    fun charTyped(event: CharacterEvent): Boolean = focused && box.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = focused && box.keyPressed(event)

    private companion object {
        const val CARET_BLINK_MS: Long = 500
    }
}
