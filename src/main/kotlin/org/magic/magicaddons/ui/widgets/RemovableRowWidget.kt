package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.resources.Identifier
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ScreenUtil.drawLine

open class RemovableRowWidget<T>(
    value: T,
    onClick: (RemovableRowWidget<T>) -> Unit,
    val onRemove: ((RemovableRowWidget<T>) -> Unit)? = null
) : ClickableRowWidget<T>(
    value,
    { onClick.invoke(it as RemovableRowWidget<T>) }
) {

    override fun getSprite(): Identifier {
        return if (isFocused) BUTTON_HOVERED else super.getSprite()
    }
    private val removeWidth = 20

    private val removeButton = ClickableButtonWidget(
        x = 0,
        y = 0,
        width = removeWidth,
        height = 20
    ) { graphics ->

        val pad = 4

        val size = minOf(width, height) - pad * 2

        val startX = x + (width - size) / 2
        val startY = y + (height - size) / 2

        val endX = startX + size
        val endY = startY + size

        graphics.drawLine(
            startX,
            startY,
            endX,
            endY,
            2,
            0xFFFF0000.toInt()
        )

        graphics.drawLine(
            endX,
            startY,
            startX,
            endY,
            2,
            0xFFFF0000.toInt()
        )
    }

    override fun getRightReservedWidth(): Int {
        return super.getRightReservedWidth() + removeWidth
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        super.render(graphics, mouseX, mouseY)

        if (onRemove != null) {
            removeButton.x = x + width - removeWidth
            removeButton.y = y
            removeButton.height = height

            removeButton.render(graphics, mouseX, mouseY, 0f)
        }
    }

    override fun mouseClicked(
        mouseButtonEvent: MouseButtonEvent,
        double: Boolean
    ): Boolean {

        if (onRemove != null &&
            removeButton.mouseClicked(mouseButtonEvent, double)
        ) {
            onRemove.invoke(this)
            return true
        }

        if (super.mouseClicked(mouseButtonEvent, double)){
            return true
        }

        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
        removeButton.mouseMoved(mouseX, mouseY)
    }
}