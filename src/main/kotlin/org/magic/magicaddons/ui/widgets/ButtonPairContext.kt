package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget

/** A panel with one button in its bottom left corner and one in its bottom right. */
abstract class ButtonPairContext(
    override val overlayX: Int,
    override val overlayY: Int,
    override val overlayWidth: Int,
    override val overlayHeight: Int,
    leftLabel: String,
    rightLabel: String,
    buttonWidth: Int
) : AbstractContextMenu() {

    override var hoveredElement: GuiEventListener? = null

    protected val font = Minecraft.getInstance().font

    protected val leftButton = ClickableButtonWidget(
        overlayX + PAD,
        overlayY + overlayHeight - PAD - BUTTON_HEIGHT,
        buttonWidth,
        BUTTON_HEIGHT,
        Component.literal(leftLabel)
    )

    protected val rightButton = ClickableButtonWidget(
        overlayX + overlayWidth - PAD - buttonWidth,
        overlayY + overlayHeight - PAD - BUTTON_HEIGHT,
        buttonWidth,
        BUTTON_HEIGHT,
        Component.literal(rightLabel)
    )

    protected fun renderButtons(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        leftButton.extractRenderState(graphics, mouseX, mouseY, delta)
        rightButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        leftButton.mouseMoved(mouseX, mouseY)
        rightButton.mouseMoved(mouseX, mouseY)

        hoveredElement = when {
            leftButton.isMouseOver(mouseX, mouseY) -> leftButton
            rightButton.isMouseOver(mouseX, mouseY) -> rightButton
            else -> null
        }
    }

    companion object {
        /** Room between the panel's frame and what it holds. */
        const val PAD: Int = Common.UI.SPACING_LARGE
        const val BUTTON_HEIGHT: Int = 20
    }
}
