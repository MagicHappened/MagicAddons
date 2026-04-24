package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import tech.thatgravyboat.skyblockapi.platform.drawSprite

class ArrowWidget(
    var x: Int,
    var y: Int,
    val width: Int = 14,
    val height: Int = 22,
    val normal: Identifier,
    val hovered: Identifier,
    val onClick: () -> Unit
) : Renderable, GuiEventListener {

    private var isHovered = false

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY:  Int, delta: Float) {
        isHovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())

        val texture = if (isHovered) hovered else normal
        val scaleX = 32f / 14f
        val scaleY = height.toFloat() / 22f



        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            (x / scaleX).toInt(),
            (y / scaleY).toInt(),
            0f,
            0f,
            14,
            22,
            14,
            22
        )
        /*
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x,
            y,
            4f,4f,
            width,
            height,
            32,
            32

        )
        */
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