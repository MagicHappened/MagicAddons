package org.magic.magicaddons.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent

interface OverlayRenderable {
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


    fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        return isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())
    }
    fun mouseMoved(mouseX: Double, mouseY: Double)  {
    }
    fun isMouseOver(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in overlayX..overlayX+overlayWidth &&
                mouseY in overlayY..overlayY+overlayHeight
    }

    fun charTyped(characterEvent: CharacterEvent): Boolean {
        return false
    }

    fun keyPressed(keyEvent: KeyEvent): Boolean {
        return false
    }

}