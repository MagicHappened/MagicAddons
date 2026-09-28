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

    var isFramed: Boolean = false

    var isFocused: Boolean
        get() = box.isFocused
        set(on) {
            box.isFocused = on
        }

    fun setResponder(responder: (String) -> Unit) = box.setResponder(responder)

    fun setMaxLength(length: Int) = box.setMaxLength(length)

    private fun textLeft(): Int = x + TEXT_INSET
    private fun textTop(): Int = y + (height - font.lineHeight) / 2
    private fun textAreaWidth(): Int = (width - TEXT_INSET * 2).coerceAtLeast(1)

    private fun placeEditBox() {
        box.x = textLeft()
        box.y = textTop()
        box.width = textAreaWidth()
        box.height = font.lineHeight
    }

    fun render(graphics: GuiGraphicsExtractor) {
        placeEditBox()
        val frameSize = if (isFramed) Common.UI.CONTROL_BORDER_SIZE else Common.UI.BORDER_SIZE
        graphics.drawField(x, y, x + width, y + height, isFocused, frameSize)
        if (isFramed && !isFocused) graphics.drawBorder(x, y, x + width, y + height, frameSize, Common.UI.BORDER_COLOR)

        val text = value
        val caretX = font.width(text.substring(0, box.cursorPosition.coerceIn(0, text.length)))

        val scrollOffset = if (isFocused) (caretX - textAreaWidth() + 1).coerceAtLeast(0) else 0
        val left = textLeft() - scrollOffset

        graphics.enableScissor(textLeft(), y, textLeft() + textAreaWidth(), y + height)

        if (text.isEmpty() && !isFocused) {
            hint?.let { graphics.modText(font, it, left, textTop(), Common.UI.DISABLED_TEXT_COLOR) }
        } else {
            graphics.modText(font, Component.literal(text), left, textTop(), Common.UI.TEXT_COLOR)
        }

        if (isFocused && System.currentTimeMillis() / CARET_BLINK_MS % 2 == 0L) {
            graphics.fill(left + caretX, textTop() - 1, left + caretX + 1, textTop() + font.lineHeight, Common.UI.TEXT_COLOR)
        }

        graphics.disableScissor()
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = inRect(mouseX, mouseY, x, y, width, height)

    fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val isInside = isMouseOver(event.x, event.y)
        isFocused = isInside

        if (isInside) {
            placeEditBox()
            box.mouseClicked(event, doubled)
        }
        return isInside
    }

    fun charTyped(event: CharacterEvent): Boolean = isFocused && box.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = isFocused && box.keyPressed(event)

    private companion object {
        const val CARET_BLINK_MS: Long = 500

        val TEXT_INSET: Int get() = Common.UI.FIELD_INSET + Common.UI.BORDER_SIZE
    }
}
