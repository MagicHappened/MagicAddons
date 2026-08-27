package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget

/**
 * The row of buttons that pins one fact about every plant to always be on show, instead of only
 * appearing in the tooltip of whichever plant the mouse happens to be over.
 *
 * Only one fact can be pinned at a time: the plants are small and a second line of text over them
 * would cover the plant it is describing. Clicking the pinned button again unpins it.
 */
class HoverControls : Renderable, GuiEventListener, HoverableContainer {

    override var hoveredElement: GuiEventListener? = null

    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = BUTTON_HEIGHT

    @JvmField
    var isFocused: Boolean = false

    /** The fact drawn on every plant, or null while nothing is pinned. */
    var selected: ElementWidget.HoverInfo? = null
        private set

    private val buttons: Map<ElementWidget.HoverInfo, ClickableButtonWidget> =
        ElementWidget.HoverInfo.entries.associateWith { info ->
            ClickableButtonWidget(0, BUTTON_HEIGHT, Component.literal(info.label))
        }

    /** Spreads the buttons evenly over [width], call again whenever the position changes. */
    fun layout() {
        height = BUTTON_HEIGHT

        val count = buttons.size
        if (count == 0) return

        val buttonWidth = (width - (count - 1) * BUTTON_SPACING) / count

        buttons.values.forEachIndexed { index, button ->
            button.width = buttonWidth
            button.height = BUTTON_HEIGHT
            button.x = x + index * (buttonWidth + BUTTON_SPACING)
            button.y = y
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        buttons.forEach { (info, button) ->
            // a button only looks pressed while its fact is the pinned one, hovering says nothing
            button.isFocused = info == selected
            button.extractRenderState(graphics, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        buttons.forEach { (info, button) ->
            if (!button.mouseClicked(mouseButtonEvent, doubled)) return@forEach

            selected = if (selected == info) null else info
            return true
        }

        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = buttons.values.firstOrNull { it.isMouseOver(mouseX, mouseY) }
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x..(x + width) && mouseY.toInt() in y..(y + height)

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused

    companion object {
        private const val BUTTON_HEIGHT: Int = 16
        private const val BUTTON_SPACING: Int = 2
    }
}
