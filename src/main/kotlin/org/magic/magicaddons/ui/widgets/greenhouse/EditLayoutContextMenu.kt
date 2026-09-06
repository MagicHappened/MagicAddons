package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ButtonPairContext
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawPanel

/** A small panel for renaming a plot or a preset: a field, Submit and Cancel. */
class EditLayoutContextMenu(
    overlayX: Int,
    overlayY: Int,
    /** What is being renamed, as the player knows it now. */
    private val currentName: String,
    private val overlayContext: OverlayContext,
    /** Given the new name once submitted; the owner writes it where it belongs and relays out. */
    private val onRename: (String) -> Unit
) : ButtonPairContext(overlayX, overlayY, WIDTH, HEIGHT, "Submit", "Cancel", BUTTON_WIDTH) {

    override val renderPriority: Int = OverlayRenderable.DIALOG_PRIORITY

    private val textField = TextField(WIDTH - PAD * 2, FIELD_HEIGHT, Component.literal("New name")).apply {
        x = overlayX + PAD
        y = overlayY + PAD + font.lineHeight + Common.UI.SPACING
        focused = true
    }

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        graphics.text(
            font,
            Component.literal("Renaming $currentName:"),
            overlayX + PAD,
            overlayY + PAD,
            Common.UI.TEXT_COLOR,
            false
        )

        textField.render(graphics)
        renderButtons(graphics, mouseX, mouseY, delta)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean =
        textField.charTyped(characterEvent) || super.charTyped(characterEvent)

    override fun keyPressed(keyEvent: KeyEvent): Boolean =
        textField.keyPressed(keyEvent) || super.keyPressed(keyEvent)

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false
        if (textField.mouseClicked(mouseButtonEvent, doubled)) return true

        if (leftButton.mouseClicked(mouseButtonEvent, doubled)) {
            if (textField.value.isBlank()) {
                ChatUtils.sendWithPrefix("Please enter a value to submit.")
                return true
            }
            onRename(textField.value.trim())
            overlayContext.removeOverlay(this)
            return true
        }
        if (rightButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
            return true
        }
        return true
    }

    companion object {
        const val WIDTH: Int = 200
        const val HEIGHT: Int = 80
        private const val FIELD_HEIGHT: Int = 20
        private const val BUTTON_WIDTH: Int = 60
    }
}
