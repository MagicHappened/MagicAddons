package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.AbstractContextMenu
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawPanel

/** A small panel for renaming a layout: a field, Submit and Cancel. */
class EditLayoutContextMenu(
    override val overlayX: Int,
    override val overlayY: Int,
    var layout: GreenhouseLayout,
    private val overlayContext: OverlayContext,
    /** The owner has a header and a selector sized from the old name, both need rebuilding. */
    private val onLayoutRenamed: (GreenhouseLayout) -> Unit
) : AbstractContextMenu() {
    val font = Minecraft.getInstance().font
    override val overlayWidth: Int = WIDTH
    override val overlayHeight: Int = HEIGHT

    /** Opened from a selector's list, so it has to draw over that list. */
    override val renderPriority: Int = 2

    override var hoveredElement: GuiEventListener? = null

    private val pad = Common.UI.SPACING_LARGE

    val textField = TextField(WIDTH - pad * 2, FIELD_HEIGHT, Component.literal("New name")).apply {
        x = overlayX + pad
        y = overlayY + pad + font.lineHeight + Common.UI.SPACING
        focused = true
    }

    val submitButton = ClickableButtonWidget(
        overlayX + pad,
        overlayY + HEIGHT - pad - BUTTON_HEIGHT,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Component.literal("Submit")
    )

    val cancelButton = ClickableButtonWidget(
        overlayX + WIDTH - pad - BUTTON_WIDTH,
        overlayY + HEIGHT - pad - BUTTON_HEIGHT,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Component.literal("Cancel")
    )

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        graphics.text(
            font,
            Component.literal("Editing ${layout.id}:"),
            overlayX + pad,
            overlayY + pad,
            Common.UI.TEXT_COLOR,
            false
        )

        textField.render(graphics)
        submitButton.extractRenderState(graphics, mouseX, mouseY, delta)
        cancelButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean =
        textField.charTyped(characterEvent) || super.charTyped(characterEvent)

    override fun keyPressed(keyEvent: KeyEvent): Boolean =
        textField.keyPressed(keyEvent) || super.keyPressed(keyEvent)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())) return false
        if (textField.mouseClicked(mouseButtonEvent, doubled)) return true

        if (submitButton.mouseClicked(mouseButtonEvent, doubled)) {
            if (textField.value.isBlank()) {
                ChatUtils.sendWithPrefix("Please enter a value to submit.")
                return true
            }
            layout.name = textField.value.trim()
            onLayoutRenamed(layout)
            overlayContext.removeOverlay(this)
            return true
        }
        if (cancelButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
            return true
        }
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        cancelButton.mouseMoved(mouseX, mouseY)
        submitButton.mouseMoved(mouseX, mouseY)

        hoveredElement = when {
            cancelButton.isMouseOver(mouseX, mouseY) -> cancelButton
            submitButton.isMouseOver(mouseX, mouseY) -> submitButton
            else -> null
        }
    }

    companion object {
        const val WIDTH: Int = 200
        const val HEIGHT: Int = 80
        private const val FIELD_HEIGHT: Int = 20
        private const val BUTTON_WIDTH: Int = 60
        private const val BUTTON_HEIGHT: Int = 20
    }
}
