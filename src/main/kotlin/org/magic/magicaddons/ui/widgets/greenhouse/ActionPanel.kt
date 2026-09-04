package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget

/**
 * A row of buttons belonging to one half of the greenhouse screen. A panel is told where it may sit
 * and fits its buttons into that, rather than measuring the grid or the window itself.
 */
abstract class ActionPanel : Renderable, Focusable, HoverableContainer {

    override var hoveredElement: GuiEventListener? = null

    override var focusedState: Boolean = false

    var x: Int = 0
        private set

    var y: Int = 0
        private set

    /** What the screen has offered, which is the most this panel may take rather than what it takes. */
    var width: Int = 0
        private set

    var height: Int = 0
        private set

    /**
     * Every button, showing or not: a panel is laid out once and drawn many times, so one left out
     * of the layout stays wherever it was born.
     */
    protected abstract val buttons: List<ClickableButtonWidget>

    /** Whether [button] is worth showing right now. Everything is, unless a panel says otherwise. */
    protected open fun isShown(button: ClickableButtonWidget): Boolean = true

    /** What each button does, asked in the same order the buttons are laid out. */
    protected abstract fun onPressed(button: ClickableButtonWidget, event: MouseButtonEvent): Boolean

    /** Puts the panel in the given box, buttons in a row along the top, wrapping when room runs out. */
    fun layoutIn(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height

        var rowX = x + PADDING
        var rowY = y + PADDING
        var rowHeight = 0

        buttons.forEach { button ->
            if (rowX + button.width > x + width - PADDING && rowX > x + PADDING) {
                rowX = x + PADDING
                rowY += rowHeight + Common.UI.SPACING
                rowHeight = 0
            }

            button.x = rowX
            button.y = rowY

            rowX += button.width + Common.UI.SPACING
            rowHeight = maxOf(rowHeight, button.height)
        }
    }

    /** Whether anything is on show, so an empty panel can be left out of the layout entirely. */
    fun hasShown(): Boolean = buttons.any { isShown(it) }

    /** How tall the panel's buttons actually came out, which a caller may want to lay out below. */
    val contentHeight: Int
        get() {
            val bottom = buttons.maxOfOrNull { it.y + it.height } ?: return 0

            return bottom - y + PADDING
        }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        buttons.filter { isShown(it) }
            .forEach { it.extractRenderState(graphics, mouseX, mouseY, delta) }

        renderContent(graphics, mouseX, mouseY, delta)
    }

    /** Anything the panel draws besides its buttons. Nothing, unless a panel says otherwise. */
    protected open fun renderContent(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) = Unit

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        buttons.filter { isShown(it) }.forEach { button ->
            if (button.mouseClicked(mouseButtonEvent, doubled)) {
                return onPressed(button, mouseButtonEvent)
            }
        }

        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until (x + width) && mouseY.toInt() in y until (y + height)

    companion object {
        /** How far the buttons sit inside the room the panel was given. */
        const val PADDING: Int = 10
    }
}
