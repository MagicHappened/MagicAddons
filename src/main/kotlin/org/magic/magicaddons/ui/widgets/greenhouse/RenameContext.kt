package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ButtonPairContext
import org.magic.magicaddons.ui.widgets.TextField
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawPanel

class RenameContext(
    overlayX: Int,
    overlayY: Int,
    private val currentName: String,
    private val overlayContext: OverlayContext,
    private val onRename: (String) -> Unit
) : ButtonPairContext(overlayX, overlayY, WIDTH, HEIGHT, "Submit", "Cancel", BUTTON_WIDTH) {

    override val renderPriority: Int = OverlayRenderable.DIALOG_PRIORITY

    private val textField = TextField(WIDTH - PADDING * 2, FIELD_HEIGHT, Component.literal("New name")).apply {
        x = overlayX + PADDING
        y = overlayY + PADDING + font.lineHeight + Common.UI.SPACING
        value = currentName
        isFocused = true
    }

    private fun submit() {
        if (textField.value.isBlank()) {
            ChatUtils.sendWithPrefix("Please enter a value to submit.")
            return
        }
        onRename(textField.value.trim())
        overlayContext.removeOverlay(this)
    }

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        graphics.text(
            font,
            Component.literal("Renaming $currentName:"),
            overlayX + PADDING,
            overlayY + PADDING,
            Common.UI.TEXT_COLOR,
            false
        )

        textField.render(graphics)
        renderButtons(graphics, mouseX, mouseY, delta)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean =
        textField.charTyped(characterEvent) || super.charTyped(characterEvent)

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key() == GLFW.GLFW_KEY_ENTER || keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
            submit()
            return true
        }
        return textField.keyPressed(keyEvent) || super.keyPressed(keyEvent)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false
        if (textField.mouseClicked(mouseButtonEvent, doubled)) return true

        if (leftButton.mouseClicked(mouseButtonEvent, doubled)) {
            submit()
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
