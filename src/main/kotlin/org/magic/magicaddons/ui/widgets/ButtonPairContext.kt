package org.magic.magicaddons.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common

abstract class ButtonPairContext(
    override val overlayX: Int,
    override val overlayY: Int,
    override val overlayWidth: Int,
    override val overlayHeight: Int,
    leftLabel: String,
    rightLabel: String,
    buttonWidth: Int
) : ContextMenu() {

    override var hoveredElement: GuiEventListener? = null

    protected val font = Minecraft.getInstance().font

    protected val leftButton = ClickableButtonWidget(
        overlayX + PADDING,
        overlayY + overlayHeight - PADDING - BUTTON_HEIGHT,
        buttonWidth,
        BUTTON_HEIGHT,
        Component.literal(leftLabel)
    )

    protected val rightButton = ClickableButtonWidget(
        overlayX + overlayWidth - PADDING - buttonWidth,
        overlayY + overlayHeight - PADDING - BUTTON_HEIGHT,
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
        const val PADDING: Int = Common.UI.SPACING_LARGE
        const val BUTTON_HEIGHT: Int = 20

        val ONE_LINE_PANEL_HEIGHT: Int = PADDING * 2 + Minecraft.getInstance().font.lineHeight + Common.UI.SPACING_LARGE + BUTTON_HEIGHT
    }
}
