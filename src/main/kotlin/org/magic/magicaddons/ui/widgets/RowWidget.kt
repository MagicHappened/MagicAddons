package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawWrappedText
import org.magic.magicaddons.util.ScreenUtil.ellipsised
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.wrappedHeight

open class RowWidget<T>(
    val value: T,
    val onClick: ((T) -> Unit)? = null
) : Focusable {

    var isHovered = false

    var isSelected = false

    var hasDividerBelow = true

    var width: Int = 0
    var height: Int = 0

    var x: Int = 0
    var y: Int = 0

    override var focusedState: Boolean = false

    open var isTextWrapped: Boolean = true

    protected val font get() = Minecraft.getInstance().font

    open fun rightReservedWidth(): Int = 0

    protected open fun labelText(): Component = Component.literal(value.toString())

    protected fun textWidth(): Int = width - rightReservedWidth() - TEXT_LEFT_PADDING * 2

    fun fitHeight(minHeight: Int) {
        val textHeight = if (isTextWrapped) wrappedHeight(font, labelText(), textWidth()) else font.lineHeight

        height = (textHeight + TEXT_VERTICAL_PADDING * 2).coerceAtLeast(minHeight)
    }

    protected open fun isHighlighted(): Boolean = isHovered || isFocused

    open fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        graphics.fill(x, y, x + width, y + height, Common.UI.BACKGROUND_COLOR)
        if (isSelected) {
            graphics.fill(x, y, x + width, y + height, Common.UI.PRESSED_SHADE)
        } else if (isHighlighted()) {
            graphics.fill(x, y, x + width, y + height, Common.UI.HOVER_WASH)
        }
        if (hasDividerBelow) graphics.fill(x, y + height - 1, x + width, y + height, Common.UI.DIVIDER_COLOR)

        val text = labelText()
        val textLeft = x + TEXT_LEFT_PADDING

        if (!isTextWrapped) {
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

    fun isMouseOverText(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, x, y, width - rightReservedWidth(), height)

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        isHovered = isMouseOverText(mouseX, mouseY)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOverText(mouseButtonEvent.x, mouseButtonEvent.y)) return false
        onClick?.invoke(value)
        return true
    }

    private companion object {
        val TEXT_LEFT_PADDING: Int = Common.UI.TEXT_X_PAD
        const val TEXT_VERTICAL_PADDING: Int = 2
    }
}
