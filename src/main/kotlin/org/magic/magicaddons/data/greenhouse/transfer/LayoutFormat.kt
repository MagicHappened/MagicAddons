package org.magic.magicaddons.data.greenhouse.transfer

import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout


interface LayoutFormat {

    val displayName: String

    fun canImport(text: String): Boolean

    fun import(text: String, layoutId: String): LayoutTransferResult

    fun export(layout: PlotLayout): LayoutTransferResult

    fun exportAll(master: GreenhouseLayout): LayoutTransferResult = export(master.plots.first())
}

sealed interface LayoutTransferResult {

    val notes: List<String>

    data class Imported(
        val layout: PlotLayout,
        override val notes: List<String> = emptyList(),
        val extraPlots: List<PlotLayout> = emptyList(),
        val presetName: String? = null
    ) : LayoutTransferResult {
        val plots: List<PlotLayout> get() = listOf(layout) + extraPlots
        val nameForPreset: String? get() = presetName ?: layout.name
    }

    data class Exported(
        val text: String,
        override val notes: List<String> = emptyList()
    ) : LayoutTransferResult

    data class Failure(
        val reason: String,
        override val notes: List<String> = emptyList()
    ) : LayoutTransferResult
}
