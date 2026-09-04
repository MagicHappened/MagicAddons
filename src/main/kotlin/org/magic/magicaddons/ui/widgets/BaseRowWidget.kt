package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight

/** One row of a list, drawn as a button: lit under the mouse, pressed in when it is the picked one. */
open class BaseRowWidget<T>(
    val value: T
) : Focusable {

    var hovered = false

    /** Whether this row is the value currently picked. */
    var selected = false

    var width: Int = 200
    var height: Int = 20

    var x: Int = 0
    var y: Int = 0

    override var focusedState: Boolean = false
    open val textLeftPadding = Common.UI.TEXT_X_PAD

    /** Room kept above and below the text when the row grows to fit it. */
    open val textVerticalPadding = 2

    protected val font get() = Minecraft.getInstance().font

    open fun getRightReservedWidth(): Int = 0

    open fun getLeftReservedWidth(): Int = 0

    protected open fun label(): Component = Component.literal(value.toString())

    /** How wide the text may be before it wraps. */
    protected fun textWidth(): Int =
        width - getLeftReservedWidth() - getRightReservedWidth() - textLeftPadding * 2

    /** Grows the row to hold its wrapped text, never below [minHeight]. Call after setting the width. */
    fun fitHeight(minHeight: Int) {
        height = (wrappedHeight(font, label(), textWidth()) + textVerticalPadding * 2).coerceAtLeast(minHeight)
    }

    /** Whether the row is lit: the mouse on it, or focus handed to it. */
    protected open fun highlighted(): Boolean = hovered || isFocused

    open fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.drawButtonPanel(x, y, x + width, y + height, highlighted(), selected)

        val text = label()
        val textHeight = wrappedHeight(font, text, textWidth())

        graphics.drawWrappedText(
            font,
            text,
            x + textLeftPadding + getLeftReservedWidth(),
            y + (height - textHeight) / 2,
            textWidth(),
            Common.UI.TEXT_COLOR
        )
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in x..x + width && mouseY.toInt() in y..y + height)
    }

    open fun isMouseOverRow(mouseX: Double, mouseY: Double): Boolean {
        return (mouseX.toInt() in x + getLeftReservedWidth()..x + width - getRightReservedWidth() && mouseY.toInt() in y..y + height)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOverRow(mouseX, mouseY)
    }
}
