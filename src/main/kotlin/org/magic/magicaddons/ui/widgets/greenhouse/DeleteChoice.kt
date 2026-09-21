package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout

/** A plot the Delete button may be about, or with a null plot the whole preset. */
class DeleteChoice(private val label: String, val plot: PlotLayout?) {
    override fun toString(): String = label

    companion object {
        fun choicesOf(master: GreenhouseLayout): List<DeleteChoice> =
            master.plots.map { DeleteChoice(master.plotTitle(it), it) } + DeleteChoice("Whole preset", null)
    }
}
