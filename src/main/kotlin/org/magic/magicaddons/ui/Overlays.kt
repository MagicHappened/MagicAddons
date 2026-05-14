package org.magic.magicaddons.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.ui.widgets.AbstractContextMenu

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

interface OverlayContext {
    val overlays: MutableList<OverlayRenderable>

    var activeContext: AbstractContextMenu?
    fun changeContext(context: AbstractContextMenu){
        activeContext?.let {
            removeOverlay(it)
        }
        activeContext = context
        addOverlay(context)
    }


    fun addOverlay(overlay: OverlayRenderable) {
        overlays.add(overlay)
        overlays.sortBy { it.renderPriority }
    }

    fun removeOverlay(overlay: OverlayRenderable) {
        overlays.remove(overlay)
    }
}


sealed class OverlayEvent {
    data class Open(val overlay: OverlayRenderable) : OverlayEvent()
    data class Close(val overlay: OverlayRenderable) : OverlayEvent()
}