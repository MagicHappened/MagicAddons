package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.ui.widgets.AbstractSelectorContextMenu
import org.magic.magicaddons.ui.widgets.ClickableRowWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import kotlin.math.max

class ApplyToContext(
    override var overlayX: Int,
    override var overlayY: Int,
    val gridSelected: (grid: GreenhouseGrid) -> Unit
) : AbstractSelectorContextMenu<GreenhouseGrid>(GreenhouseData.greenhouseGrids) {

    override val valueWidgets: MutableList<ClickableRowWidget<GreenhouseGrid>> = mutableListOf()
    val font = Minecraft.getInstance().font
    private val yPadding = 4
    private val title = "Assign To:"
    override val overlayWidth: Int
        get() = max(getMaxRowWidth(), font.width(title))

    override val overlayHeight: Int
        get() =
            font.lineHeight +
                    yPadding * 2 +
                    (valueWidgets.size * rowHeight)
    override val rowStartY: Int = overlayY + yPadding *2 + font.lineHeight




    override fun onValueSelected(value: GreenhouseGrid) {
        gridSelected(value)
    }

    override fun renderOverlay(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(
            overlayX,
            overlayY,
            overlayX + overlayWidth,
            overlayY + overlayHeight,
            Common.UI.BACKGROUND_COLOR
        )

        graphics.drawBorder(
            overlayX,
            overlayY,
            overlayX + overlayWidth,
            overlayY + overlayHeight,
            2,
            Common.UI.BORDER_COLOR
        )
        graphics.drawString(
            font,
            title,
            overlayX + Common.UI.TEXT_X_PAD,
            overlayY + yPadding,
            0xFFFFFFFF.toInt()
        )

        valueWidgets.forEach { it.render(graphics, mouseX, mouseY) }
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (!isMouseOver(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt())) return false
        valueWidgets.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled))
                return true
        }
        return true
    }

    @JvmField
    var isFocused = false

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused
}