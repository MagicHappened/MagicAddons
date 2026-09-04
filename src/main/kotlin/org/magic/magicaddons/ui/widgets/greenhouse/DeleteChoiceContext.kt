package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.MasterLayout
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.AbstractSelectorContextMenu

/** Which plot of a preset the Delete button is about, or the whole preset. */
class DeleteChoiceContext(
    override val overlayX: Int,
    override val overlayY: Int,
    private val overlayContext: OverlayContext,
    master: MasterLayout,
    private val onChoice: (GreenhouseLayout?) -> Unit
) : AbstractSelectorContextMenu<DeleteChoiceContext.Choice>(choicesOf(master), "Delete:", withSearch = false) {

    /** A plot, or null for the preset with every plot in it. */
    class Choice(private val label: String, val plot: GreenhouseLayout?) {
        override fun toString(): String = label
    }

    override fun onValueSelected(value: Choice) {
        overlayContext.removeOverlay(this)
        onChoice(value.plot)
    }

    private companion object {
        fun choicesOf(master: MasterLayout): List<Choice> =
            master.plots.map { Choice(master.plotTitle(it), it) } + Choice("Whole preset", null)
    }
}
