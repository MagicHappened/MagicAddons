package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.ui.OverlayContext

class PickContext<T>(
    x: Int,
    y: Int,
    title: String,
    values: List<T>,
    private val context: OverlayContext,
    private val onPick: (T) -> Unit
) : ListContextMenu<T>(x, y, values, title) {

    override fun onValueSelected(value: T) {
        context.removeOverlay(this)
        onPick(value)
    }
}
