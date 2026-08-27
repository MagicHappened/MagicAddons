package org.magic.magicaddons.ui

import net.minecraft.client.gui.components.events.GuiEventListener

/**
 * Focus state for the widgets of this mod.
 *
 * [GuiEventListener] declares focus as the pair of methods `isFocused()` and `setFocused()`, which
 * kotlin also sees as a synthetic `isFocused` property. A widget that stores its focus in a field
 * of that same name shadows the property, and the usual `override fun isFocused() = isFocused` then
 * reads the method it is defining rather than the field, calling itself until the stack runs out.
 *
 * Implementing this instead names the state something else, so the shadowing cannot happen and the
 * contract lives in one place.
 */
interface Focusable : GuiEventListener {

    var focusedState: Boolean

    override fun isFocused(): Boolean = focusedState

    override fun setFocused(focused: Boolean) {
        focusedState = focused
    }
}
