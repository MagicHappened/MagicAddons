package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.util.Identifier

class ArrowWidget(
    var x: Int,
    var y: Int,
    val width: Int = 16,
    val height: Int = 16,
    val normal: Identifier,
    val hovered: Identifier,
    val onClick: () -> Unit
) : Drawable, Element {

    private var isHovered = false

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        isHovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())

        val texture = if (isHovered) hovered else normal

        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x,
            y,
            width,
            height
        )
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (isMouseOver(click.x, click.y)) {
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