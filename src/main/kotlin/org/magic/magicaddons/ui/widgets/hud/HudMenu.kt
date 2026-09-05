package org.magic.magicaddons.ui.widgets.hud

import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.AbstractSelectorContextMenu

/** The right click menu of the hud editor: a few named actions under the thing's name. */
class HudMenu(
    override val overlayX: Int,
    override val overlayY: Int,
    title: String,
    entries: List<Entry>,
    private val context: OverlayContext
) : AbstractSelectorContextMenu<HudMenu.Entry>(entries, title, withSearch = false) {

    class Entry(val label: String, val action: () -> Unit) {
        override fun toString(): String = label
    }

    override val rowHeight: Int = 16

    override fun onValueSelected(value: Entry) {
        context.removeOverlay(this)
        value.action()
    }

    companion object {
        fun widthFor(title: String, entries: List<Entry>): Int {
            val font = net.minecraft.client.Minecraft.getInstance().font
            return (entries.maxOfOrNull { font.width(it.label) } ?: 0).coerceAtLeast(font.width(title)) + 8
        }

        fun heightFor(entries: List<Entry>): Int = 16 + entries.size * 16
    }
}
