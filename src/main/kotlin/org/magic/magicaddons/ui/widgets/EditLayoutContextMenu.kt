package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class EditLayoutContextMenu(
    override val overlayX: Int,
    override val overlayY: Int,
    var layout: GreenhouseLayout
) : AbstractContextMenu() {

    override val overlayWidth: Int = 200
    override val overlayHeight: Int = 100

    val submitButton = ClickableButtonWidget(
        overlayX+20,
        overlayY+75,
        40,
        20,
        Component.literal("Submit")
    )

    val cancelButton = ClickableButtonWidget(
        overlayX+140,
        overlayY+75,
        40,
        20,
        Component.literal("Cancel")
    )

    override fun renderOverlay(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        graphics.fill(
            overlayX,
            overlayY,
            overlayX+overlayWidth,
            overlayY+overlayHeight,
            Common.UI.BACKGROUND_COLOR
        )

        graphics.drawBorder(
            overlayX,
            overlayY,
            overlayX+overlayWidth,
            overlayY+overlayHeight,
            4,
            Common.UI.BORDER_COLOR
        )

        submitButton.render(graphics, mouseX, mouseY, delta)
        cancelButton.render(graphics, mouseX, mouseY, delta)
    }





    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())) return false
        if (submitButton.mouseClicked(mouseButtonEvent, doubled)) return true
        if (cancelButton.mouseClicked(mouseButtonEvent, doubled)) return true
        return true
    }

}