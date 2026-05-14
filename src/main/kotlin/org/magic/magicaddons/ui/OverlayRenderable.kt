package org.magic.magicaddons.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent

interface OverlayRenderable : GuiEventListener, HoverableContainer {
    val renderPriority: Int

    val overlayX: Int
    val overlayY: Int
    val overlayWidth: Int
    val overlayHeight: Int

    fun renderOverlay(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ){

    }


    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        return isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())
    }
    override fun mouseMoved(mouseX: Double, mouseY: Double)  {
    }
    fun isMouseOver(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in overlayX..overlayX+overlayWidth &&
                mouseY in overlayY..overlayY+overlayHeight
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        return false
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        return false
    }

}