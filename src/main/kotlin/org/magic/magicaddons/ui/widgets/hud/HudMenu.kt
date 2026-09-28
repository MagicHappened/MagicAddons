package org.magic.magicaddons.ui.widgets.hud

import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.ListContextMenu

class HudMenu(
    x: Int,
    y: Int,
    title: String,
    entries: List<Entry>,
    private val context: OverlayContext
) : ListContextMenu<HudMenu.Entry>(x, y, entries, title) {

    class Entry(val label: String, val action: () -> Unit) {
        override fun toString(): String = label
    }

    override val rowHeight: Int = 16

    override fun onValueSelected(value: Entry) {
        context.removeOverlay(this)
        value.action()
    }
}
