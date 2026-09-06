package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import kotlin.math.max

/** A question with Yes and No under it. Yes runs [onYes]; either answer closes the panel. */
class ConfirmContext(
    overlayX: Int,
    overlayY: Int,
    private val question: String,
    private val overlayContext: OverlayContext,
    private val onYes: () -> Unit
) : ButtonPairContext(overlayX, overlayY, widthFor(question), HEIGHT, "Yes", "No", BUTTON_WIDTH) {

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        graphics.text(font, Component.literal(question), overlayX + PAD, overlayY + PAD, Common.UI.TEXT_COLOR, false)

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

        /** The question, a gap, the buttons, padded. */
        val HEIGHT: Int = ButtonPairContext.PAD * 2 + Minecraft.getInstance().font.lineHeight + Common.UI.SPACING_LARGE + ButtonPairContext.BUTTON_HEIGHT

        /** Wide enough for the question or the two buttons, whichever is longer. */
        fun widthFor(question: String): Int =
            max(Minecraft.getInstance().font.width(question), BUTTON_WIDTH * 2 + Common.UI.SPACING_LARGE) + ButtonPairContext.PAD * 2
    }
}
