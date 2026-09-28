package org.magic.magicaddons.ui

import net.minecraft.client.gui.components.events.GuiEventListener

interface Focusable : GuiEventListener {

    var focusedState: Boolean

    override fun isFocused(): Boolean = focusedState

    override fun setFocused(focused: Boolean) {
        focusedState = focused
    }
}
