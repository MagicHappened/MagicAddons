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

    val importsAsNewPreset: Boolean get() = false

    val carriesSteps: Boolean get() = false

    fun canImport(text: String): Boolean

    fun import(text: String, layoutId: String): LayoutTransferResult

    fun export(layout: PlotLayout): LayoutTransferResult

    fun exportAll(master: GreenhouseLayout): LayoutTransferResult = export(master.plots.first())
}

enum class LayoutFormatType(val format: LayoutFormat) {
    MagicAddons(MagicAddonsFormat),
    SkyLayouts(SkyLayoutsFormat),
    SkyShards(SkyShardsFormat),
    SkyShardsFlows(SkyShardsFlowsFormat),
    SkyMutations(SkyMutationsFormat);

    override fun toString(): String = format.displayName
}

sealed interface LayoutTransferResult {

    data class Imported(
        val layout: PlotLayout,
        val extraPlots: List<PlotLayout> = emptyList(),
        val presetName: String? = null
    ) : LayoutTransferResult {
        val plots: List<PlotLayout> get() = listOf(layout) + extraPlots
        val nameForPreset: String? get() = presetName ?: layout.name
    }

    data class Exported(val text: String) : LayoutTransferResult

    data class Failure(val reason: String) : LayoutTransferResult
}
