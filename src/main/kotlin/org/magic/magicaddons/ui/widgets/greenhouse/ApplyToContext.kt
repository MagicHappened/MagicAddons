package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.AbstractSelectorContextMenu

/** The list of greenhouses a preset can be assigned to. */
class ApplyToContext(
    override var overlayX: Int,
    override var overlayY: Int,
    private val overlayContext: OverlayContext,
    val gridSelected: (grid: GreenhouseGrid) -> Unit
) : AbstractSelectorContextMenu<GreenhouseGrid>(GreenhouseData.greenhouseGrids, "Assign To:") {

    override fun onValueSelected(value: GreenhouseGrid) {
        overlayContext.removeOverlay(this)
        gridSelected(value)
    }
}
