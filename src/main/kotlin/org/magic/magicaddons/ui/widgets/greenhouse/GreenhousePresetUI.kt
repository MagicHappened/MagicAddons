package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ChatUtils

class GreenhousePresetUI(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    val overlayContext: OverlayContext,
    val selectedPreset: GreenhouseLayout?,
    val currentGrids: List<GreenhouseGrid>,
    val onAssignedLayout: (assignedLayout: GreenhouseLayout?, selectedGrid: GreenhouseGrid) -> Unit,
) : Renderable, GuiEventListener, HoverableContainer {

    override var hoveredElement: GuiEventListener? = null
    var contextOpened = false

    @JvmField
    var isFocused: Boolean = false

    private val importButton = ClickableButtonWidget(
        0,
        0,
        60,
        26,
        Component.literal("Import")
    )

    private val exportButton = ClickableButtonWidget(
        0,
        0,
        60,
        26,
        Component.literal("Export")
    )

    val applyToButton = ClickableButtonWidget(
        0,
        0,
        60,
        26,
        Component.literal("Apply To:")
    )


    fun init() {

        importButton.x = x + 10
        importButton.y = y + 10

        exportButton.x = importButton.x + importButton.width + 10
        exportButton.y = importButton.y

        applyToButton.x = exportButton.x + exportButton.width + 10
        applyToButton.y = y + 10
        val font = Minecraft.getInstance().font

    }


    override fun render(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {

        importButton.render(graphics, mouseX, mouseY, delta)
        exportButton.render(graphics, mouseX, mouseY, delta)
        applyToButton.render(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (importButton.mouseClicked(mouseButtonEvent, doubled)) {
            ChatUtils.sendWithPrefix("tried importing")
            return true
        }
        if (exportButton.mouseClicked(mouseButtonEvent, doubled)) {
            ChatUtils.sendWithPrefix("tried exporting")
            return true
        }
        if (applyToButton.mouseClicked(mouseButtonEvent, doubled)) {
            val context = ApplyToContext(
                mouseButtonEvent.x.toInt(),
                mouseButtonEvent.y.toInt(),
                { onAssignedLayout.invoke(selectedPreset, it) }

            )
            context.init()
            overlayContext.changeContext(context)


            return true
        }
        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = null
        importButton.mouseMoved(mouseX, mouseY)
        exportButton.mouseMoved(mouseX, mouseY)
        applyToButton.mouseMoved(mouseX, mouseY)
        if (hoveredElement == null) {
            if (importButton.isMouseOver(mouseX, mouseY)) {
                hoveredElement = importButton
            }
        }
        if (hoveredElement == null) {
            if (exportButton.isMouseOver(mouseX, mouseY)) {
                hoveredElement = exportButton
            }
        }
        if (hoveredElement == null) {
            if (applyToButton.isMouseOver(mouseX, mouseY)) {
                hoveredElement = applyToButton
            }
        }
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused

    fun importPreset() {

    }

    fun exportPreset() {

    }

}