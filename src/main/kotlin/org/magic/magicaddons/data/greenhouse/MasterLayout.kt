package org.magic.magicaddons.data.greenhouse

/** A preset: one to three plots, one a greenhouse, filed under one name. */
data class MasterLayout(
    val id: String, // preset_#
    var name: String? = null,
    val plots: MutableList<GreenhouseLayout> = mutableListOf()
) {
    /** The given name, or the preset number when it was never named. */
    fun displayName(): String = name
        ?: id.removePrefix("preset_").takeIf { it != id }?.let { "Preset $it" }
        ?: id

    override fun toString(): String = displayName()

    /** The id a plot gets from its place in the list, so ids never clash across presets. */
    fun plotId(index: Int): String = "${id}_p${index + 1}"

    /** What a plot is called on its bookmark: its own name, or its place in the list. */
    fun plotTitle(plot: GreenhouseLayout): String = plot.name ?: "Plot ${plots.indexOf(plot) + 1}"

    /** One more empty plot at the end of the list. */
    fun addPlot(): GreenhouseLayout = GreenhouseLayout(id = plotId(plots.size)).also { plots.add(it) }

    fun isEmpty(): Boolean = plots.all { it.elementInstances.isEmpty() }

    companion object {
        const val MAX_PLOTS: Int = 3

        fun create(number: Int): MasterLayout = MasterLayout(id = "preset_$number").apply { addPlot() }

        /** A preset around a plot that was saved on its own, as older files hold them. */
        fun of(layout: GreenhouseLayout): MasterLayout =
            MasterLayout(id = layout.id, name = layout.name, plots = mutableListOf(layout)).also { layout.name = null }
    }
}
