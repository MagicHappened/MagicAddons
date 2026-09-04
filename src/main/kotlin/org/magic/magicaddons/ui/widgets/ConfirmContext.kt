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
import kotlin.math.max

/** A question with Yes and No under it. Yes runs [onYes]; either answer closes the panel. */
class ConfirmContext(
    override val overlayX: Int,
    override val overlayY: Int,
    private val question: String,
    private val overlayContext: OverlayContext,
    private val onYes: () -> Unit
) : AbstractContextMenu() {

    override var hoveredElement: GuiEventListener? = null

    private val font = Minecraft.getInstance().font

    override val overlayWidth: Int = widthFor(question)
    override val overlayHeight: Int = HEIGHT

    private val yesButton = ClickableButtonWidget(
        overlayX + PAD,
        overlayY + HEIGHT - PAD - BUTTON_HEIGHT,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Component.literal("Yes")
    )

    private val noButton = ClickableButtonWidget(
        overlayX + overlayWidth - PAD - BUTTON_WIDTH,
        overlayY + HEIGHT - PAD - BUTTON_HEIGHT,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Component.literal("No")
    )

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        graphics.text(font, Component.literal(question), overlayX + PAD, overlayY + PAD, Common.UI.TEXT_COLOR, false)

        yesButton.extractRenderState(graphics, mouseX, mouseY, delta)
        noButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())) return false

        if (yesButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
            onYes()
        } else if (noButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
        }
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        yesButton.mouseMoved(mouseX, mouseY)
        noButton.mouseMoved(mouseX, mouseY)

        hoveredElement = when {
            yesButton.isMouseOver(mouseX, mouseY) -> yesButton
            noButton.isMouseOver(mouseX, mouseY) -> noButton
            else -> null
        }
    }

    companion object {
        private const val PAD: Int = Common.UI.SPACING_LARGE
        private const val BUTTON_WIDTH: Int = 50
        private const val BUTTON_HEIGHT: Int = 20

        /** The question, a gap, the buttons, padded. */
        val HEIGHT: Int = PAD * 2 + Minecraft.getInstance().font.lineHeight + Common.UI.SPACING_LARGE + BUTTON_HEIGHT

        /** Wide enough for the question or the two buttons, whichever is longer. */
        fun widthFor(question: String): Int =
            max(Minecraft.getInstance().font.width(question), BUTTON_WIDTH * 2 + Common.UI.SPACING_LARGE) + PAD * 2
    }
}
