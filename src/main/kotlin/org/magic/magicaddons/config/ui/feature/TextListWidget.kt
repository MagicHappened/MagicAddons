package org.magic.magicaddons.config.ui.feature

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element

class TextListWidget : Drawable, Element {

    override fun render(
        context: DrawContext?,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float
    ) {
        TODO("Not yet implemented")
    }

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused

}