package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.modText

/**
 * Asked before something with unsaved changes is left: Discard, Save to the named preset, or Cancel.
 * Discard and Save run their action; every button closes the panel.
 */
class UnsavedChangesContext(
    override val overlayX: Int,
    override val overlayY: Int,
    saveTo: String,
    private val overlayContext: OverlayContext,
    private val onDiscard: () -> Unit,
    private val onSave: () -> Unit
) : AbstractContextMenu() {

    override var hoveredElement: GuiEventListener? = null

    private val font = Minecraft.getInstance().font

    private val discardButton = ClickableButtonWidget("Discard")
    private val saveButton = ClickableButtonWidget("Save to $saveTo")
    private val cancelButton = ClickableButtonWidget("Cancel")

    private val buttons = listOf(discardButton, saveButton, cancelButton)

    override val overlayWidth: Int = widthFor(saveTo)
    override val overlayHeight: Int = HEIGHT

    init {
        var buttonX = overlayX + ButtonPairContext.PAD
        buttons.forEach { button ->
            button.height = ButtonPairContext.BUTTON_HEIGHT
            button.x = buttonX
            button.y = overlayY + overlayHeight - ButtonPairContext.PAD - ButtonPairContext.BUTTON_HEIGHT
            buttonX += button.width + Common.UI.SPACING
        }
    }

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)
        graphics.modText(font, Component.literal(QUESTION), overlayX + ButtonPairContext.PAD, overlayY + ButtonPairContext.PAD, Common.UI.TEXT_COLOR)
        buttons.forEach { it.extractRenderState(graphics, mouseX, mouseY, delta) }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        val pressed = buttons.firstOrNull { it.mouseClicked(mouseButtonEvent, doubled) } ?: return true
        overlayContext.removeOverlay(this)

        when (pressed) {
            discardButton -> onDiscard()
            saveButton -> onSave()
        }
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        buttons.forEach { it.mouseMoved(mouseX, mouseY) }
        hoveredElement = buttons.firstOrNull { it.isMouseOver(mouseX, mouseY) }
    }

    companion object {
        private const val QUESTION: String = "There are unsaved changes."

        /** The question, a gap, the buttons, padded. */
        val HEIGHT: Int = ButtonPairContext.PAD * 2 + Minecraft.getInstance().font.lineHeight + Common.UI.SPACING_LARGE + ButtonPairContext.BUTTON_HEIGHT

        /** Wide enough for the question or the three buttons, whichever is wider. */
        fun widthFor(saveTo: String): Int {
            val buttonsWidth = listOf("Discard", "Save to $saveTo", "Cancel").sumOf { ClickableButtonWidget.widthFor(it) } + Common.UI.SPACING * 2

            return maxOf(Minecraft.getInstance().font.width(QUESTION), buttonsWidth) + ButtonPairContext.PAD * 2
        }
    }
}
