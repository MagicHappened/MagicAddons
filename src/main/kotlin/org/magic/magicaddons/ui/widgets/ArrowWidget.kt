package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

class ArrowWidget(
    var x: Int,
    var y: Int,
    val width: Int = 16,
    val height: Int = 16,
    val normal: Identifier,
    val hovered: Identifier,
    val onClick: () -> Unit
) : Renderable, GuiEventListener {

    private var isHovered = false

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY:  Int, delta: Float) {
        isHovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())

        val texture = if (isHovered) hovered else normal

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x,
            y,
            0f,0f,
            width,
            height,
            32,
            32

        )

    }


    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            onClick()
            return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height
    }

    override fun isFocused(): Boolean = false
    override fun setFocused(focused: Boolean) {}
}