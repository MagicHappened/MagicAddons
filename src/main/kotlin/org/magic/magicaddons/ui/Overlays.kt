package org.magic.magicaddons.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.ui.widgets.AbstractContextMenu
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.compat.McCompat

interface OverlayRenderable : GuiEventListener, HoverableContainer {

    /** higher priority draws first. */
    val renderPriority: Int

    val overlayX: Int
    val overlayY: Int
    val overlayWidth: Int
    val overlayHeight: Int

    fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean =
        isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)

    override fun mouseMoved(mouseX: Double, mouseY: Double) {}

    // An overlay takes no keyboard focus
    override fun isFocused(): Boolean = false

    override fun setFocused(focused: Boolean) {}

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, overlayX, overlayY, overlayWidth, overlayHeight)

    /** fired when the overlay is taken off-screen */
    fun onClosed() {
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = false

    override fun keyPressed(keyEvent: KeyEvent): Boolean = false

    companion object {
        /** A context menu opened at the cursor */
        const val MENU_PRIORITY: Int = 0

        /** A list dropped down under a selector or a text box. */
        const val DROPDOWN_PRIORITY: Int = 1

        /** A panel opened from a dropped down list, so it has to draw over that list. */
        const val DIALOG_PRIORITY: Int = 2

        /** Where a menu opened at a point should sit: at the cursor, folded back when it runs out. */
        fun placeOnScreen(x: Int, y: Int, menuWidth: Int, menuHeight: Int): Pair<Int, Int> {
            val screen = McCompat.currentScreen() ?: return x to y
            val scrolling = screen as? ScrollView

            // on a scrolling screen the edges are those of the part on screen, in content coordinates
            val left = scrolling?.viewLeft ?: 0
            val top = scrolling?.viewTop ?: 0
            val right = scrolling?.viewRight ?: screen.width
            val bottom = scrolling?.viewBottom ?: screen.height

            return (if (x + menuWidth > right) x - menuWidth else x).coerceAtLeast(left) to
                    (if (y + menuHeight > bottom) y - menuHeight else y).coerceAtLeast(top)
        }
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

    fun closeOverlays() {
        val closing = overlays.toList()

        overlays.clear()
        closing.forEach { it.onClosed() }
    }

    /** The overlays drawn lowest priority first, so the highest ends up on top. */
    fun renderOverlays(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        overlays.toList().asReversed().forEach { it.renderOverlay(graphics, mouseX, mouseY, delta) }
    }

    /** Whether an overlay consumed the click. Walked over a copy, since a handler may open or close one. */
    fun overlaysMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean =
        overlays.toList().any { it.mouseClicked(event, doubled) }

    fun overlaysMouseMoved(mouseX: Double, mouseY: Double) {
        overlays.toList().forEach { it.mouseMoved(mouseX, mouseY) }
    }

    fun overlaysMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        overlays.toList().any { it.mouseScrolled(mouseX, mouseY, scrollX, scrollY) }

    fun overlaysCharTyped(event: CharacterEvent): Boolean =
        overlays.toList().any { it.charTyped(event) }

    fun overlaysKeyPressed(event: KeyEvent): Boolean =
        overlays.toList().any { it.keyPressed(event) }
}
