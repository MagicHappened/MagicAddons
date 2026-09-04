package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.AbstractSelectorContextMenu

/** The list of formats a layout can be imported from or exported to. */
class ImportExportFormatContext(
    override val overlayX: Int,
    override val overlayY: Int,
    private val overlayContext: OverlayContext,
    val formatSelected: (LayoutFormatType) -> Unit
) : AbstractSelectorContextMenu<ImportExportFormatContext.LayoutFormatType>(LayoutFormatType.entries, "Format:", withSearch = false) {

    enum class LayoutFormatType {
        SkyMutations,
        SkyShards,
        SkyLayouts,
        MagicAddons
    }

    override fun onValueSelected(value: LayoutFormatType) {
        // the format has been picked, the list has nothing left to say
        overlayContext.removeOverlay(this)
        formatSelected(value)
    }
}
