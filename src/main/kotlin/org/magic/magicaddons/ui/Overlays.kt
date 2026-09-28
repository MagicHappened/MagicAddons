package org.magic.magicaddons.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.ui.widgets.ContextMenu
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.compat.McCompat

interface OverlayRenderable : GuiEventListener, HoverableContainer {

    val renderPriority: Int

    val overlayX: Int
    val overlayY: Int
    val overlayWidth: Int
    val overlayHeight: Int

    fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean =
        isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)

    override fun mouseMoved(mouseX: Double, mouseY: Double) {}

    override fun isFocused(): Boolean = false

    override fun setFocused(focused: Boolean) {}

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, overlayX, overlayY, overlayWidth, overlayHeight)

    fun onClosed() {
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean = false

    override fun keyPressed(keyEvent: KeyEvent): Boolean = false

    companion object {

        const val MENU_PRIORITY: Int = 0

        const val DROPDOWN_PRIORITY: Int = 1

        const val DIALOG_PRIORITY: Int = 2

        fun placeOnScreen(x: Int, y: Int, menuWidth: Int, menuHeight: Int): Pair<Int, Int> {
            val screen = McCompat.currentScreen() ?: return x to y
            val scrolling = screen as? ScrollView

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

    fun addContext(context: ContextMenu) {
        overlays.filter { it::class == context::class }.forEach { removeOverlay(it) }

        addOverlay(context)
    }

    fun addOverlay(overlay: OverlayRenderable) {
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

    fun renderOverlays(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        overlays.toList().asReversed().forEach { it.renderOverlay(graphics, mouseX, mouseY, delta) }
    }

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
