package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawLine

open class RemovableRowWidget<T>(
    value: T,
    onClick: (T) -> Unit,
    val onRemove: ((T) -> Unit)? = null
) : RowWidget<T>(value, onClick) {

    private val removeButton = ClickableButtonWidget(
        width = REMOVE_BUTTON_WIDTH,
        height = 0,
        { graphics ->
            val size = minOf(width, height) - CROSS_INSET * 2

            val startX = x + (width - size) / 2
            val startY = y + (height - size) / 2
            val endX = startX + size
            val endY = startY + size

            graphics.drawLine(startX, startY, endX, endY, 2, Common.UI.DANGER_COLOR)
            graphics.drawLine(endX, startY, startX, endY, 2, Common.UI.DANGER_COLOR)
        }
    )

    override fun rightReservedWidth(): Int = super.rightReservedWidth() + if (onRemove != null) REMOVE_BUTTON_WIDTH else 0

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        super.extractRenderState(graphics, mouseX, mouseY)

        if (onRemove != null) {
            removeButton.x = x + width - REMOVE_BUTTON_WIDTH
            removeButton.y = y
            removeButton.height = height

            removeButton.extractRenderState(graphics, mouseX, mouseY, 0f)
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (onRemove != null && removeButton.mouseClicked(mouseButtonEvent, doubled)) {
            onRemove.invoke(value)
            return true
        }

        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        removeButton.mouseMoved(mouseX, mouseY)
    }

    private companion object {
        const val REMOVE_BUTTON_WIDTH: Int = 20
        const val CROSS_INSET: Int = 4
    }
}
