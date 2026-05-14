package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import org.magic.magicaddons.ui.widgets.AbstractContextMenu

class ImportExportFormatContext(
    override val overlayX: Int,
    override val overlayY: Int,
    override val overlayWidth: Int,
    override val overlayHeight: Int
) : AbstractContextMenu() {

    override var hoveredElement: GuiEventListener? = null

    @JvmField
    var isFocused = false

    override fun renderOverlay(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {

    }


    override fun isFocused(): Boolean = isFocused
    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }
}