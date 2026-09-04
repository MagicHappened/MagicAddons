package org.magic.magicaddons.ui.widgets.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel

/** A button drawn as a panel that lights under the mouse, with whatever [renderContent] puts on it. */
class ClickableButtonWidget(
    var width: Int,
    var height: Int,
    val renderContent: ClickableButtonWidget.(GuiGraphicsExtractor) -> Unit,
    val shouldRenderButton: Boolean = true
) : Focusable {
    var x: Int = 0
    var y: Int = 0

    constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        renderContent: ClickableButtonWidget.(GuiGraphicsExtractor) -> Unit,
        shouldRenderButton: Boolean = true
    ) : this(width, height, renderContent, shouldRenderButton) {
        this.x = x
        this.y = y
    }

    constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        shouldRenderButton: Boolean = true
    ) : this(width, height, message, shouldRenderButton) {
        this.x = x
        this.y = y
    }

    constructor(
        width: Int,
        height: Int,
        message: Component,
        shouldRenderButton: Boolean = true
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

    override var focusedState: Boolean = false

    /** Set from [mouseMoved], so the button lights up under the mouse. */
    var hovered: Boolean = false

    /** Drawn pressed in while true, for a button that stands for a state rather than an action. */
    var pressed: Boolean = false

    var message: Component? = null

    fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (shouldRenderButton) {
            graphics.drawButtonPanel(x, y, x + width, y + height, hovered || isFocused, pressed)
        }

        renderContent(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        return isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = isMouseOver(mouseX, mouseY)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in x until (x + width) &&
                mouseY.toInt() in y until (y + height)
    }

    private companion object {
        /** A style colour carries no alpha, and text drawn with none is invisible. */
        const val OPAQUE: Int = 0xFF000000.toInt()
    }
}
