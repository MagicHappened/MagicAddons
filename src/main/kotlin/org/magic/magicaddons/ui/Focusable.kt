package org.magic.magicaddons.ui

import net.minecraft.client.gui.components.events.GuiEventListener

/**
 * Focus state for this mod's widgets, named from GuiEventListener's `isFocused`
 */
interface Focusable : GuiEventListener {

    var focusedState: Boolean

    override fun isFocused(): Boolean = focusedState

    override fun setFocused(focused: Boolean) {
        focusedState = focused
    }
}
