package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.ellipsised
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight

/**
 * One row of a list: lit under the mouse, pressed in when it is the picked one. Rows carry no
 * frame of their own, only the line under them; the list's panel frames them all.
 */
open class BaseRowWidget<T>(
    val value: T
) : Focusable {

    var hovered = false

    /** Whether this row is the value currently picked. */
    var selected = false

    /** The line under the row; the last row of a list leaves it to the list's frame. */
    var dividerBelow = true

    var width: Int = 0
    var height: Int = 0

    var x: Int = 0
    var y: Int = 0

    override var focusedState: Boolean = false
    open val textLeftPadding = Common.UI.TEXT_X_PAD

    /** Room kept above and below the text when the row grows to fit it. */
    open val textVerticalPadding = 2

    /** Whether long text wraps onto more lines; off, it is cut short with an ellipsis instead. */
    open var wrapText: Boolean = true

    protected val font get() = Minecraft.getInstance().font

    open fun getRightReservedWidth(): Int = 0

    open fun getLeftReservedWidth(): Int = 0

    protected open fun label(): Component = Component.literal(value.toString())

    /** How wide the text may be before it wraps. */
    protected fun textWidth(): Int =
        width - getLeftReservedWidth() - getRightReservedWidth() - textLeftPadding * 2

    /** Grows the row to hold its wrapped text, never below [minHeight]. Call after setting the width. */
    fun fitHeight(minHeight: Int) {
        val textHeight = if (wrapText) wrappedHeight(font, label(), textWidth()) else font.lineHeight

        height = (textHeight + textVerticalPadding * 2).coerceAtLeast(minHeight)
    }

    /** Whether the row is lit: the mouse on it, or focus handed to it. */
    protected open fun highlighted(): Boolean = hovered || isFocused

    open fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.fill(x, y, x + width, y + height, Common.UI.BACKGROUND_COLOR)
        if (selected) {
            graphics.fill(x, y, x + width, y + height, Common.UI.PRESSED_SHADE)
        } else if (highlighted()) {
            graphics.fill(x, y, x + width, y + height, Common.UI.HOVER_WASH)
        }
        if (dividerBelow) graphics.fill(x, y + height - 1, x + width, y + height, Common.UI.DIVIDER_COLOR)

        val text = label()
        val textLeft = x + textLeftPadding + getLeftReservedWidth()

        if (!wrapText) {
            val shown = ellipsised(font, text.string, textWidth())
            graphics.text(
                font,
                Component.literal(shown),
                textLeft,
                y + (height - font.lineHeight) / 2,
                Common.UI.TEXT_COLOR,
                false
            )
            return
        }

        val textHeight = wrappedHeight(font, text, textWidth())

        graphics.drawWrappedText(
            font,
            text,
            textLeft,
            y + (height - textHeight) / 2,
            textWidth(),
            Common.UI.TEXT_COLOR
        )
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, x, y, width, height)

    /** Whether the mouse is on the text part of the row, between the reserved ends. */
    open fun isMouseOverRow(mouseX: Double, mouseY: Double): Boolean {
        val left = x + getLeftReservedWidth()
        return inRect(mouseX, mouseY, left, y, x + width - getRightReservedWidth() - left, height)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOverRow(mouseX, mouseY)
    }
}
