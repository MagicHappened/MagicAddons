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
import kotlin.math.max

/**
 * A question with Yes and No under it, and a warning in red between them when there is something
 * the player should know before saying yes. Yes runs [onYes]; either answer closes the panel.
 */
class ConfirmContext(
    overlayX: Int,
    overlayY: Int,
    private val question: String,
    private val overlayContext: OverlayContext,
    private val warning: String? = null,
    private val onYes: () -> Unit
) : ButtonPairContext(overlayX, overlayY, widthFor(question, warning), heightFor(question, warning), "Yes", "No", BUTTON_WIDTH) {

    constructor(
        overlayX: Int,
        overlayY: Int,
        question: String,
        overlayContext: OverlayContext,
        onYes: () -> Unit
    ) : this(overlayX, overlayY, question, overlayContext, null, onYes)

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        graphics.modText(font, Component.literal(question), overlayX + PAD, overlayY + PAD, Common.UI.TEXT_COLOR)

        warning?.let { text ->
            var lineY = overlayY + PAD + font.lineHeight + Common.UI.SPACING
            warningLines(text).forEach { line ->
                graphics.modText(font, line, overlayX + PAD, lineY, Common.UI.DANGER_COLOR)
                lineY += font.lineHeight
            }
        }

        renderButtons(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) return false

        if (leftButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
            onYes()
        } else if (rightButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
        }
        return true
    }

    companion object {
        private const val BUTTON_WIDTH: Int = 50

        /** A warning wraps at this width rather than stretching the panel across the screen. */
        private const val WARNING_WIDTH: Int = 220

        /** The question, a gap, the buttons, padded. */
        val HEIGHT: Int = ButtonPairContext.PAD * 2 + Minecraft.getInstance().font.lineHeight + Common.UI.SPACING_LARGE + ButtonPairContext.BUTTON_HEIGHT

        /** The warning as bold red lines, wrapped to fit. */
        private fun warningLines(warning: String) =
            Minecraft.getInstance().font.split(
                Component.literal(warning).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                WARNING_WIDTH
            )

        /** [HEIGHT], plus the wrapped warning and a gap when there is one. */
        fun heightFor(question: String, warning: String?): Int {
            if (warning == null) return HEIGHT

            return HEIGHT + Common.UI.SPACING + warningLines(warning).size * Minecraft.getInstance().font.lineHeight
        }

        /** Wide enough for the question, the warning, or the two buttons, whichever is widest. */
        fun widthFor(question: String, warning: String? = null): Int {
            val font = Minecraft.getInstance().font
            val warningWidth = warning?.let { text -> warningLines(text).maxOfOrNull { font.width(it) } ?: 0 } ?: 0

            return maxOf(font.width(question), warningWidth, BUTTON_WIDTH * 2 + Common.UI.SPACING_LARGE) + ButtonPairContext.PAD * 2
        }
    }
}
