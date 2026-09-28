package org.magic.magicaddons.data.greenhouse.transfer

import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout


fun PlotLayout.placeImportedPlant(definition: CropDefinition, x: Int, y: Int, mark: LayoutSlot.Marking?) {
    val originSlot = getSlot(x, y) ?: return
    definition.footprint.cellsFrom(x, y).forEach { (cellX, cellY) ->
        getSlot(cellX, cellY)?.let { slot ->
            slot.soil = definition.requiredSoil.firstOrNull()
            slot.mark = mark
        }
    }
    plants.add(Plant(definition.elementId, originSlot, cropDef = definition))
}

interface LayoutFormat {

    val displayName: String

    fun canImport(text: String): Boolean

    fun import(text: String, layoutId: String): LayoutTransferResult

    fun export(layout: PlotLayout): LayoutTransferResult

    fun exportAll(master: GreenhouseLayout): LayoutTransferResult = export(master.plots.first())
}

enum class LayoutFormatType(val format: LayoutFormat) {
    MagicAddons(MagicAddonsFormat),
    SkyLayouts(SkyLayoutsFormat),
    SkyShards(SkyShardsFormat),
    SkyMutations(SkyMutationsFormat)
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
