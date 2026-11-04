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
    private val note: String? = null,
    confirmLabel: String = CONFIRM_LABEL,
    cancelLabel: String = CANCEL_LABEL,
    private val onConfirm: () -> Unit
) : ButtonPairContext(
    overlayX,
    overlayY,
    widthFor(question, warning, note, confirmLabel, cancelLabel),
    heightFor(warning, note),
    confirmLabel,
    cancelLabel,
    buttonWidthFor(confirmLabel, cancelLabel)
) {

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        graphics.modText(font, Component.literal(question), overlayX + PADDING, overlayY + PADDING, Common.UI.TEXT_COLOR)

        var lineY = overlayY + PADDING + font.lineHeight + Common.UI.SPACING
        warning?.let { text ->
            warningLines(text).forEach { line ->
                graphics.modText(font, line, overlayX + PADDING, lineY, Common.UI.DANGER_COLOR)
                lineY += font.lineHeight
            }
        }
        note?.let { text ->
            noteLines(text).forEach { line ->
                graphics.modText(font, line, overlayX + PADDING, lineY, Common.UI.TEXT_DIM_COLOR)
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

        private const val CONFIRM_LABEL: String = "Yes"
        private const val CANCEL_LABEL: String = "No"

        private fun warningLines(warning: String) =
            Minecraft.getInstance().font.split(
                Component.literal(warning).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                WARNING_WRAP_WIDTH
            )

        private fun noteLines(note: String) = Minecraft.getInstance().font.split(Component.literal(note), WARNING_WRAP_WIDTH)

        private fun buttonWidthFor(confirmLabel: String, cancelLabel: String): Int =
            maxOf(BUTTON_WIDTH, ClickableButtonWidget.widthFor(confirmLabel), ClickableButtonWidget.widthFor(cancelLabel))

        fun heightFor(warning: String? = null, note: String? = null): Int {
            val extraLines = (warning?.let { warningLines(it).size } ?: 0) + (note?.let { noteLines(it).size } ?: 0)
            if (extraLines == 0) return ONE_LINE_PANEL_HEIGHT

            return ONE_LINE_PANEL_HEIGHT + Common.UI.SPACING + extraLines * Minecraft.getInstance().font.lineHeight
        }

        fun widthFor(
            question: String,
            warning: String? = null,
            note: String? = null,
            confirmLabel: String = CONFIRM_LABEL,
            cancelLabel: String = CANCEL_LABEL
        ): Int {
            val font = Minecraft.getInstance().font
            val warningWidth = warning?.let { text -> warningLines(text).maxOfOrNull { font.width(it) } ?: 0 } ?: 0
            val noteWidth = note?.let { text -> noteLines(text).maxOfOrNull { font.width(it) } ?: 0 } ?: 0

            return maxOf(font.width(question), warningWidth, noteWidth, buttonWidthFor(confirmLabel, cancelLabel) * 2 + Common.UI.SPACING_LARGE) + PADDING * 2
        }
    }
}
