package org.magic.magicaddons.ui.widgets.config

import org.magic.magicaddons.ui.Focusable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

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
    ) : this(width, height, renderContent, shouldRenderButton){
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
    ) : this(width, height, message, shouldRenderButton){
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
                    (it.style.color?.value ?: 0xFFFFFFFF.toInt()) or 0xFF000000.toInt(),
                    false
                )
            }

        }
    ){
        this.message = message
    }

    override var focusedState: Boolean = false

    /** Set from [mouseMoved], so the button lights up under the mouse like a vanilla one. */
    var hovered: Boolean = false

    var message: Component? = null


    val BUTTON = Identifier.fromNamespaceAndPath("minecraft", "widget/button")
    val BUTTON_HOVERED = Identifier.fromNamespaceAndPath("minecraft", "widget/button_highlighted")

    fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        if (shouldRenderButton) {
            val sprite = if (hovered || isFocused)
                BUTTON_HOVERED
            else
                BUTTON

            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                x,
                y,
                width,
                height
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

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in x..(x + width) &&
                mouseY.toInt() in y..(y + height)
    }


}