package org.magic.magicaddons.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.ui.screens.ScrollableScreen
import org.magic.magicaddons.ui.widgets.AbstractContextMenu
import org.magic.magicaddons.util.compat.McCompat

interface OverlayRenderable : GuiEventListener, HoverableContainer {

    /** Higher wins: a higher priority overlay draws on top and is offered input first. */
    val renderPriority: Int

    val overlayX: Int
    val overlayY: Int
    val overlayWidth: Int
    val overlayHeight: Int

    fun renderOverlay(
        graphics: GuiGraphicsExtractor,
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
        return mouseX in overlayX until overlayX + overlayWidth &&
                mouseY in overlayY until overlayY + overlayHeight
    }

    /** Told when the overlay is taken off screen, so it can stop believing it is open. */
    fun onClosed() {
    }

    companion object {
        /** Where a menu opened at a point should sit: at the cursor, folded back when it runs out. */
        fun placeOnScreen(x: Int, y: Int, menuWidth: Int, menuHeight: Int): Pair<Int, Int> {
            val screen = McCompat.currentScreen() ?: return x to y
            val scrolling = screen as? ScrollableScreen

            // on a scrolling screen the edges are those of the part on screen, in content coordinates
            val left = scrolling?.viewLeft ?: 0
            val top = scrolling?.viewTop ?: 0
            val right = scrolling?.viewRight ?: screen.width
            val bottom = scrolling?.viewBottom ?: screen.height

            return (if (x + menuWidth > right) x - menuWidth else x).coerceAtLeast(left) to
                    (if (y + menuHeight > bottom) y - menuHeight else y).coerceAtLeast(top)
        }
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

    fun addContext(context: AbstractContextMenu) {
        overlays.filter { it::class == context::class }.forEach { removeOverlay(it) }

        addOverlay(context)
    }

    fun addOverlay(overlay: OverlayRenderable) {
        // an overlay registered twice, which init does on every resize, would render and take input
        // once per copy
        overlays.remove(overlay)

        overlays.add(overlay)
        overlays.sortByDescending { it.renderPriority }
    }

    fun removeOverlay(overlay: OverlayRenderable) {
        if (overlays.remove(overlay)) {
            overlay.onClosed()
        }
    }

    /** Takes every overlay off screen, telling each one so it does not stay half open. */
    fun closeOverlays() {
        val closing = overlays.toList()

        overlays.clear()
        closing.forEach { it.onClosed() }
    }
}