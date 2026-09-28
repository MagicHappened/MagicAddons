package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.modText

class UnsavedChangesContext(
    override val overlayX: Int,
    override val overlayY: Int,
    saveTarget: String,
    private val overlayContext: OverlayContext,
    private val onDiscard: () -> Unit,
    private val onSave: () -> Unit
) : ContextMenu() {

    override var hoveredElement: GuiEventListener? = null

    private val font = Minecraft.getInstance().font

    private val discardButton = ClickableButtonWidget("Discard")
    private val saveButton = ClickableButtonWidget("Save to $saveTarget")
    private val cancelButton = ClickableButtonWidget("Cancel")

    private val buttons = listOf(discardButton, saveButton, cancelButton)

    override val overlayWidth: Int = widthFor(saveTarget)
    override val overlayHeight: Int = ButtonPairContext.ONE_LINE_PANEL_HEIGHT

    init {
        var buttonX = overlayX + ButtonPairContext.PADDING
        buttons.forEach { button ->
            button.height = ButtonPairContext.BUTTON_HEIGHT
            button.x = buttonX
            button.y = overlayY + overlayHeight - ButtonPairContext.PADDING - ButtonPairContext.BUTTON_HEIGHT
            buttonX += button.width + Common.UI.SPACING
        }
    }

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)
        graphics.modText(font, Component.literal(QUESTION), overlayX + ButtonPairContext.PADDING, overlayY + ButtonPairContext.PADDING, Common.UI.TEXT_COLOR)
        buttons.forEach { it.extractRenderState(graphics, mouseX, mouseY, delta) }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        val pressedButton = buttons.firstOrNull { it.mouseClicked(mouseButtonEvent, doubled) } ?: return true
        overlayContext.removeOverlay(this)

        when (pressedButton) {
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

        fun widthFor(saveTarget: String): Int {
            val buttonsWidth = listOf("Discard", "Save to $saveTarget", "Cancel").sumOf { ClickableButtonWidget.widthFor(it) } + Common.UI.SPACING * 2

            return maxOf(Minecraft.getInstance().font.width(QUESTION), buttonsWidth) + ButtonPairContext.PADDING * 2
        }
    }
}
