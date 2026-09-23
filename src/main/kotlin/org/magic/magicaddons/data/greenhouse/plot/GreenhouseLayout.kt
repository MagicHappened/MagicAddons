package org.magic.magicaddons.data.greenhouse.plot

data class GreenhouseLayout(
    val id: String, // preset_#
    var name: String? = null,
    val plots: MutableList<PlotLayout> = mutableListOf()
) {
    fun displayName(): String = name
        ?: id.removePrefix(PlotLayout.MASTER_PRESET_PREFIX).takeIf { it != id }?.let { "Preset $it" }
        ?: id

    override fun toString(): String = displayName()

    fun plotId(index: Int): String = plotId(id, index)

    fun plotTitle(plot: PlotLayout): String = plot.displayName()

    fun addPlot(): PlotLayout = PlotLayout(id = firstFreePlotId()).also { plots.add(it) }


    private fun firstFreePlotId(): String =
        generateSequence(0) { it + 1 }.map { plotId(it) }.first { id -> plots.none { it.id == id } }


    fun repairPlotIds(): Boolean {
        var repaired = false

        for (index in plots.indices) {
            val plot = plots[index]
            if (plots.subList(0, index).none { it.id == plot.id }) continue

            plots[index] = PlotLayout(
                id = firstFreePlotId(),
                name = plot.name,
                size = plot.size,
                slots = plot.slots,
                plants = plot.plants
            )
            repaired = true
        }

        return repaired
    }

    fun isEmpty(): Boolean = plots.all { it.plants.isEmpty() }

    companion object {
        const val MAX_PLOTS: Int = 3

        fun plotId(presetId: String, index: Int): String = "${presetId}_p${index + 1}"

        fun create(number: Int): GreenhouseLayout = GreenhouseLayout(id = PlotLayout.presetId(number)).apply { addPlot() }

        // for older files still having them
        fun create(layout: PlotLayout): GreenhouseLayout =
            GreenhouseLayout(id = layout.id, name = layout.name, plots = mutableListOf(layout)).also { layout.name = null }
    }
}
