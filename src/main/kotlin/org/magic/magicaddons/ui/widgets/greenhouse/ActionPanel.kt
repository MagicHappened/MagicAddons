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
 * A row of buttons belonging to one half of the greenhouse screen.
 *
 * Each mode of the screen has things the player can do in it, and they were being laid out, drawn
 * and clicked in whichever place happened to own them: the presets in a panel of their own and the
 * greenhouses loose on the screen. So the two lined up only because both did the same arithmetic,
 * and every new greenhouse button made the screen itself longer.
 *
 * A panel is told where it may sit and how much room it has, and fits its buttons into that. It
 * never measures the grid or the window itself, since a panel that works out where it is cannot be
 * put anywhere else, which is what made the old one only ever fit beside a ten by ten.
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
     * Every button this panel has, left to right, whether or not it is showing.
     *
     * All of them, always: a panel is laid out once and asked to draw many times, so a button
     * left out of the layout because it was hidden at that moment appears later still sitting at
     * the corner of the screen where it was born.
     */
    protected abstract val buttons: List<ClickableButtonWidget>

    /** Whether [button] is worth showing right now. Everything is, unless a panel says otherwise. */
    protected open fun isShown(button: ClickableButtonWidget): Boolean = true

    /** What each button does, asked in the same order the buttons are laid out. */
    protected abstract fun onPressed(button: ClickableButtonWidget, event: MouseButtonEvent): Boolean

    /**
     * Puts the panel at [x], [y] with [width] by [height] to work within.
     *
     * The buttons run along the top in a row, wrapping onto the next line when the room runs out
     * rather than continuing off the edge of what they were given.
     */
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
