package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.ui.widgets.AbstractContextMenu
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawBorder

class EditLayoutContextMenu(
    override val overlayX: Int,
    override val overlayY: Int,
    var layout: GreenhouseLayout
) : AbstractContextMenu() {
    val font = Minecraft.getInstance().font
    override val overlayWidth: Int = 200
    override val overlayHeight: Int = 100

    @JvmField
    var isFocused: Boolean = false

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

        graphics.drawString(
            font,
            "Editing ${layout.id}:",
            overlayX + 10,
            overlayY + 10,
            0xFFFFFFFF.toInt()
        )
        textField.render(graphics, mouseX, mouseY, delta)

        submitButton.render(graphics, mouseX, mouseY, delta)
        cancelButton.render(graphics, mouseX, mouseY, delta)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (textField.isFocused){
            textField.charTyped(characterEvent)
            return true
        }
        return super.charTyped(characterEvent)
    }

    override fun setFocused(focused: Boolean) {
        this.isFocused = focused
    }

    override fun isFocused(): Boolean = this.isFocused

    override fun keyPressed(keyEvent: KeyEvent): Boolean  {
        if (textField.isFocused){
            textField.keyPressed(keyEvent)
            return true
        }
        return super.keyPressed(keyEvent)
    }



    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())) return false
        val screen: GreenhouseScreen = Minecraft.getInstance().screen as? GreenhouseScreen ?: return false
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
            screen.overlays.remove(this)
            return true
        }
        if (cancelButton.mouseClicked(mouseButtonEvent, doubled)) {
            screen.overlays.remove(this)
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

}