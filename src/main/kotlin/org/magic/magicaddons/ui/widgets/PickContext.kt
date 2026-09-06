package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.ui.OverlayContext

/** A short titled list with no search; picking a value closes the list and hands it to [onPick]. */
class PickContext<T>(
    x: Int,
    y: Int,
    title: String,
    values: List<T>,
    private val context: OverlayContext,
    private val onPick: (T) -> Unit
) : AbstractSelectorContextMenu<T>(x, y, values, title, withSearch = false) {

    override fun onValueSelected(value: T) {
        context.removeOverlay(this)
        onPick(value)
    }
}
