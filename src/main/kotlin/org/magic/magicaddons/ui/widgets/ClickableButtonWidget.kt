package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.withAlpha
import org.magic.magicaddons.util.ScreenUtil.easedProgress
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.inRect

class ClickableButtonWidget(
    var width: Int,
    var height: Int,
    val renderContent: ClickableButtonWidget.(GuiGraphicsExtractor) -> Unit
) : Focusable {
    var x: Int = 0
    var y: Int = 0

    constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        label: Component
    ) : this(width, height, label) {
        this.x = x
        this.y = y
    }

    constructor(
        width: Int,
        height: Int,
        label: Component
    ) : this(
        width,
        height,
        { graphics ->
            val font = Minecraft.getInstance().font
            this.label?.let {
                graphics.text(
                    font,
                    it,
                    this.x + (this.width - font.width(it)) / 2,
                    this.y + (this.height - font.lineHeight) / 2,
                    (it.style.color?.value ?: Common.UI.TEXT_COLOR) or Common.UI.OPAQUE_ALPHA,
                    false
                )
            }
        }
    ) {
        this.label = label
    }

    constructor(label: String) : this(widthFor(label), DEFAULT_HEIGHT, Component.literal(label))

    override var focusedState: Boolean = false

    var isHovered: Boolean = false

    var isPressed: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            pressedChangedAt = System.currentTimeMillis()
        }

    private var pressedChangedAt: Long = 0L

    var label: Component? = null

    fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val pressProgress = easedProgress(pressedChangedAt, PRESS_ANIMATION_MS)
        val pressedFraction = if (isPressed) pressProgress else 1f - pressProgress

        graphics.drawButtonPanel(x, y, x + width, y + height, isHovered || isFocused, pressed = false)

        if (pressedFraction > 0f) {
            graphics.fill(x, y, x + width, y + height, withAlpha(Common.UI.PRESSED_SHADE, pressedFraction))
            graphics.drawBorder(
                x, y, x + width, y + height,
                Common.UI.BORDER_SIZE,
                withAlpha(Common.UI.SELECTED_FRAME_COLOR, pressedFraction)
            )
        }

        renderContent(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean =
        mouseButtonEvent.button() == 0 && isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        isHovered = isMouseOver(mouseX, mouseY)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, x, y, width, height)

    companion object {
        const val DEFAULT_HEIGHT: Int = 26

        fun widthFor(label: String): Int =
            Minecraft.getInstance().font.width(label) + (Common.UI.TEXT_X_PAD + Common.UI.BORDER_SIZE) * 2

        private const val PRESS_ANIMATION_MS: Long = 150
    }
}
