package org.magic.magicaddons.data.greenhouse

/** A preset: one to three plots, one a greenhouse, filed under one name. */
data class MasterLayout(
    val id: String, // preset_#
    var name: String? = null,
    val plots: MutableList<GreenhouseLayout> = mutableListOf()
) {
    /** The given name, or the preset number when it was never named. */
    fun displayName(): String = name
        ?: id.removePrefix(GreenhouseLayout.PRESET_PREFIX).takeIf { it != id }?.let { "Preset $it" }
        ?: id

    override fun toString(): String = displayName()

    /** The id a plot gets from its place in the list, so ids never clash across presets. */
    fun plotId(index: Int): String = plotId(id, index)

    /** What a plot is called on its bookmark: its own name, or its place in the list. */
    fun plotTitle(plot: GreenhouseLayout): String = plot.name ?: "Plot ${plots.indexOf(plot) + 1}"

    /** One more empty plot at the end of the list. */
    fun addPlot(): GreenhouseLayout = GreenhouseLayout(id = freePlotId()).also { plots.add(it) }

    /**
     * The lowest plot id no plot here carries. Removing a plot leaves a gap in the numbering, and
     * the next plot takes the gap rather than the id of the plot now last in the list.
     */
    private fun freePlotId(): String =
        generateSequence(0) { it + 1 }.map { plotId(it) }.first { id -> plots.none { it.id == id } }

    /**
     * Gives every plot sharing an id with an earlier one a free id of its own. A file written while
     * a new plot could take the id of the plot last in the list holds such pairs, and a greenhouse
     * assigned one of them was resolving to whichever came first.
     */
    fun repairPlotIds(): Boolean {
        var repaired = false

        for (index in plots.indices) {
            val plot = plots[index]
            if (plots.subList(0, index).none { it.id == plot.id }) continue

            plots[index] = GreenhouseLayout(
                id = freePlotId(),
                name = plot.name,
                size = plot.size,
                slots = plot.slots,
                elementInstances = plot.elementInstances
            )
            repaired = true
        }

        return repaired
    }

    fun isEmpty(): Boolean = plots.all { it.elementInstances.isEmpty() }

    companion object {
        const val MAX_PLOTS: Int = 3

        /** The id of the plot at [index] of the preset with id [presetId]. */
        fun plotId(presetId: String, index: Int): String = "${presetId}_p${index + 1}"

        fun create(number: Int): MasterLayout = MasterLayout(id = GreenhouseLayout.presetId(number)).apply { addPlot() }

        /** A preset around a plot that was saved on its own, as older files hold them. */
        fun of(layout: GreenhouseLayout): MasterLayout =
            MasterLayout(id = layout.id, name = layout.name, plots = mutableListOf(layout)).also { layout.name = null }
    }
}
