package org.magic.magicaddons.ui

import net.minecraft.client.gui.components.events.GuiEventListener

/**
 * Focus state for this mod's widgets, named away from GuiEventListener's synthetic `isFocused`
 * property: a field of that name shadows it, and the usual override then calls itself forever.
 */
interface Focusable : GuiEventListener {

    var focusedState: Boolean

    override fun isFocused(): Boolean = focusedState

    override fun setFocused(focused: Boolean) {
        focusedState = focused
    }
}
