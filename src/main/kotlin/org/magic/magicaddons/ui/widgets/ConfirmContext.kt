package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.util.ScreenUtil.modText
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.util.ScreenUtil.drawPanel

class ConfirmContext(
    overlayX: Int,
    overlayY: Int,
    private val question: String,
    private val overlayContext: OverlayContext,
    private val warning: String? = null,
    private val onConfirm: () -> Unit
) : ButtonPairContext(overlayX, overlayY, widthFor(question, warning), heightFor(warning), "Yes", "No", BUTTON_WIDTH) {

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        graphics.modText(font, Component.literal(question), overlayX + PADDING, overlayY + PADDING, Common.UI.TEXT_COLOR)

        warning?.let { text ->
            var lineY = overlayY + PADDING + font.lineHeight + Common.UI.SPACING
            warningLines(text).forEach { line ->
                graphics.modText(font, line, overlayX + PADDING, lineY, Common.UI.DANGER_COLOR)
                lineY += font.lineHeight
            }
        }

        renderButtons(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        if (leftButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
            onConfirm()
        } else if (rightButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
        }
        return true
    }

    companion object {
        private const val BUTTON_WIDTH: Int = 50

        private const val WARNING_WRAP_WIDTH: Int = 220

        private fun warningLines(warning: String) =
            Minecraft.getInstance().font.split(
                Component.literal(warning).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                WARNING_WRAP_WIDTH
            )

        fun heightFor(warning: String? = null): Int {
            if (warning == null) return ONE_LINE_PANEL_HEIGHT

            return ONE_LINE_PANEL_HEIGHT + Common.UI.SPACING + warningLines(warning).size * Minecraft.getInstance().font.lineHeight
        }

        fun widthFor(question: String, warning: String? = null): Int {
            val font = Minecraft.getInstance().font
            val warningWidth = warning?.let { text -> warningLines(text).maxOfOrNull { font.width(it) } ?: 0 } ?: 0

            return maxOf(font.width(question), warningWidth, BUTTON_WIDTH * 2 + Common.UI.SPACING_LARGE) + PADDING * 2
        }
    }
}
