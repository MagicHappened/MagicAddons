package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.withAlpha
import org.magic.magicaddons.util.ScreenUtil.eased
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.inRect

/** A button drawn as a panel that lights under the mouse, with whatever [renderContent] puts on it. */
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
        message: Component
    ) : this(width, height, message) {
        this.x = x
        this.y = y
    }

    constructor(
        width: Int,
        height: Int,
        message: Component
    ) : this(
        width,
        height,
        { graphics ->
            val font = Minecraft.getInstance().font
            this.message?.let {
                graphics.text(
                    font,
                    it,
                    this.x + (width - font.width(it)) / 2,
                    this.y + (height - font.lineHeight) / 2,
                    (it.style.color?.value ?: Common.UI.TEXT_COLOR) or OPAQUE,
                    false
                )
            }
        }
    ) {
        this.message = message
    }

    /** A button of the standard height, as wide as its [label] needs. */
    constructor(label: String) : this(widthFor(label), HEIGHT, Component.literal(label))

    override var focusedState: Boolean = false

    /** Set from [mouseMoved], so the button lights up under the mouse. */
    var hovered: Boolean = false

    /** Drawn pressed in while true, for a button that stands for a state rather than an action. */
    var pressed: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            pressedChangedAt = System.currentTimeMillis()
        }

    /** When the button last went in or came out, so the pressed look eases rather than snaps. */
    private var pressedChangedAt: Long = 0L

    var message: Component? = null

    fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val along = eased(pressedChangedAt, PRESS_MS)
        val pressedFraction = if (pressed) along else 1f - along

        graphics.drawButtonPanel(x, y, x + width, y + height, hovered || isFocused, pressed = false)

        // the shade and the bright frame laid over the plain button by how far in it is
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

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        return isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, x, y, width, height)

    companion object {
        /** The height of the toolbar buttons on the greenhouse screen. */
        const val HEIGHT: Int = 26

        /** A button as wide as its word and the usual padding, so a row of them wastes nothing. */
        fun widthFor(label: String): Int =
            Minecraft.getInstance().font.width(label) + (Common.UI.TEXT_X_PAD + Common.UI.BORDER_SIZE) * 2

        /** A style colour carries no alpha, and text drawn with none is invisible. */
        private const val OPAQUE: Int = 0xFF000000.toInt()

        /** How long the pressed look takes to settle in or out. */
        private const val PRESS_MS: Long = 150
    }
}
