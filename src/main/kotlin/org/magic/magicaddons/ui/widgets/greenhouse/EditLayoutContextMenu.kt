package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.AbstractContextMenu
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class EditLayoutContextMenu(
    override val overlayX: Int,
    override val overlayY: Int,
    var layout: GreenhouseLayout,
    private val overlayContext: OverlayContext,
    /** The owner has a header and a selector sized from the old name, both need rebuilding. */
    private val onLayoutRenamed: (GreenhouseLayout) -> Unit
) : AbstractContextMenu() {
    val font = Minecraft.getInstance().font
    override val overlayWidth: Int = WIDTH
    override val overlayHeight: Int = HEIGHT


    override var hoveredElement: GuiEventListener? = null

    val submitButton = ClickableButtonWidget(
        overlayX + 20,
        overlayY + 75,
        40,
        20,
        Component.literal("Submit")
    )

    val cancelButton = ClickableButtonWidget(
        overlayX + 140,
        overlayY + 75,
        40,
        20,
        Component.literal("Cancel")
    )

    val textField = EditBox(
        font,
        overlayX + 10,
        overlayY + 20,
        100,
        20,
        Component.empty()
    )


    override fun renderOverlay(
        graphics: GuiGraphicsExtractor,
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

        graphics.text(
            font,
            "Editing ${layout.id}:",
            overlayX + 10,
            overlayY + 10,
            Common.UI.TEXT_COLOR
        )
        textField.extractRenderState(graphics, mouseX, mouseY, delta)

        submitButton.extractRenderState(graphics, mouseX, mouseY, delta)
        cancelButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (textField.isFocused){
            textField.charTyped(characterEvent)
            return true
        }
        return super.charTyped(characterEvent)
    }



    override fun keyPressed(keyEvent: KeyEvent): Boolean  {
        if (textField.isFocused){
            textField.keyPressed(keyEvent)
            return true
        }
        return super.keyPressed(keyEvent)
    }



    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())) return false
        if (textField.mouseClicked(mouseButtonEvent,doubled)){
            textField.isFocused = true
            return true
        }
        textField.isFocused = false
        if (submitButton.mouseClicked(mouseButtonEvent, doubled)) {
            if (textField.value.isBlank()){
                ChatUtils.sendWithPrefix("Please enter a value to submit.")
                return true
            }
            layout.name = textField.value.trim()
            onLayoutRenamed(layout)
            overlayContext.removeOverlay(this)
            return true
        }
        if (cancelButton.mouseClicked(mouseButtonEvent, doubled)) {
            overlayContext.removeOverlay(this)
            return true
        }
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        textField.mouseMoved(mouseX, mouseY)
        cancelButton.mouseMoved(mouseX, mouseY)
        submitButton.mouseMoved(mouseX, mouseY)
        hoveredElement = null
        if (hoveredElement == null){
            if (cancelButton.isMouseOver(mouseX, mouseY)){
                hoveredElement = cancelButton
            }
        }
        if (hoveredElement == null){
            if (submitButton.isMouseOver(mouseX, mouseY)){
                hoveredElement = submitButton
            }
        }

    }

    companion object {
        const val WIDTH: Int = 200
        const val HEIGHT: Int = 100
    }

}