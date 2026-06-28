package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import org.magic.magicaddons.ui.HoverableContainer

//todo implement width height etc.
// make selectable for perm show (but only 1)
// need to store? after implement see feedback
class HoverControls() : Renderable, GuiEventListener, HoverableContainer {
    override var hoveredElement: GuiEventListener? = null

    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0


    @JvmField
    var isFocused: Boolean = false

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {

    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused

}