package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.transfer.LayoutFormat
import org.magic.magicaddons.data.greenhouse.transfer.LayoutFormatType
import org.magic.magicaddons.data.greenhouse.transfer.LayoutTransferResult
import org.magic.magicaddons.data.greenhouse.transfer.SkyMutationsFormat
import org.magic.magicaddons.data.greenhouse.transfer.SkyShardsFormat
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.util.ChatUtils

class PresetPanel(
    overlayContext: OverlayContext,
    val onAssignToGreenhouse: (assignedLayout: PlotLayout?, selectedGrid: GreenhouseGrid, noRotate: Boolean) -> Unit,
    val onImported: (LayoutTransferResult.Imported, isSoftImport: Boolean) -> Unit,
    val onDelete: (PlotLayout?) -> Unit,
    val onNewPreset: () -> Unit,
    val shownPlot: () -> PlotLayout?,
    val onTurn: (quarterTurns: Int) -> Unit,
) : ActionPanel(overlayContext) {

    private val importButton = ClickableButtonWidget("Import")
    private val exportButton = ClickableButtonWidget("Export")
    private val plannerButton = ClickableButtonWidget("Planner")
    private val deleteButton = ClickableButtonWidget("Delete")
    private val newButton = ClickableButtonWidget(ClickableButtonWidget.DEFAULT_HEIGHT, ClickableButtonWidget.DEFAULT_HEIGHT, Component.literal("+"))
    private val turnLeftButton = ClickableButtonWidget(ClickableButtonWidget.DEFAULT_HEIGHT, ClickableButtonWidget.DEFAULT_HEIGHT, Component.literal("↺"))
    private val turnRightButton = ClickableButtonWidget(ClickableButtonWidget.DEFAULT_HEIGHT, ClickableButtonWidget.DEFAULT_HEIGHT, Component.literal("↻"))

    override val buttons: List<ClickableButtonWidget> =
        listOf(newButton, turnLeftButton, turnRightButton, importButton, exportButton, plannerButton, deleteButton)

    override fun onPressed(button: ClickableButtonWidget, event: MouseButtonEvent): Boolean {
        when (button) {
            newButton -> onNewPreset()
            turnLeftButton -> onTurn(-1)
            turnRightButton -> onTurn(1)
            importButton -> openImportMenu(event)
            exportButton -> openExportMenu(event)
            plannerButton -> openAssignMenu(event)
            deleteButton -> askDelete(event)
            else -> return false
        }
        return true
    }

    private fun greenhousesToAssign(): List<GreenhouseGrid> {
        val current = GreenhouseData.getCurrentGrid()
        return GreenhouseData.greenhouseGrids.sortedByDescending { it === current }
    }

    private fun openImportMenu(event: MouseButtonEvent) {
        openMenuWithOption(event, "Format:", LayoutFormatType.entries, SOFT_IMPORT_LABEL, softImport, SOFT_IMPORT_TOOLTIP) { type, soft ->
            softImport = soft
            importPreset(type, soft)
        }
    }

    private fun openExportMenu(event: MouseButtonEvent) {
        openMenuWithOption(event, "Format:", LayoutFormatType.entries, SHOWN_PLOT_LABEL, exportShownOnly, SHOWN_PLOT_TOOLTIP) { type, shownOnly ->
            exportShownOnly = shownOnly
            exportPreset(type, shownOnly)
        }
    }

    private fun openAssignMenu(event: MouseButtonEvent) {
        openMenuWithOption(event, "Assign To:", greenhousesToAssign(), NO_ROTATE_LABEL, noRotate, NO_ROTATE_TOOLTIP) { grid, noRotateChecked ->
            noRotate = noRotateChecked
            onAssignToGreenhouse(shownPlot(), grid, noRotateChecked)
        }
    }

    private fun askDelete(event: MouseButtonEvent) {
        val preset = GreenhouseData.currentPreset ?: run {
            ChatUtils.sendWithPrefix("No preset to remove.")
            return
        }
        val clickX = event.x.toInt()
        val clickY = event.y.toInt()

        if (preset.plots.size > 1) {
            openMenu(event, "Delete:", DeleteChoice.choicesOf(preset)) { confirmDelete(preset, it.plot, clickX, clickY) }
        } else {
            confirmDelete(preset, null, clickX, clickY)
        }
    }

    private fun confirmDelete(preset: GreenhouseLayout, plot: PlotLayout?, clickX: Int, clickY: Int) {
        val question = if (plot != null) {
            "Delete ${preset.plotTitle(plot)} from ${preset.displayName()}?"
        } else {
            "Delete preset ${preset.displayName()}?"
        }
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(clickX, clickY, ConfirmContext.widthFor(question), ConfirmContext.heightFor())
        overlayContext.addContext(ConfirmContext(menuX, menuY, question, overlayContext) { onDelete(plot) })
    }

    private fun importPreset(type: LayoutFormatType, soft: Boolean) {
        val format = type.format
        val clipboard = Minecraft.getInstance().keyboardHandler.clipboard

        if (!format.canImport(clipboard)) {
            ChatUtils.sendWithPrefix("Your clipboard does not hold a ${format.displayName} layout.")
            return
        }

        val result = format.import(clipboard, PlotLayout.presetId(GreenhouseData.computeNextAvailableId()))

        result.notes.forEach { ChatUtils.sendWithPrefix(it) }

        when (result) {
            is LayoutTransferResult.Failure -> ChatUtils.sendWithPrefix(result.reason)
            is LayoutTransferResult.Imported -> {
                val plants = result.plots.sumOf { it.plants.size }
                val plots = if (result.plots.size > 1) " over ${result.plots.size} plots" else ""
                ChatUtils.sendWithPrefix("Imported $plants plants$plots from ${format.displayName}")
                onImported.invoke(result, soft)
            }
            is LayoutTransferResult.Exported -> Unit
        }
    }

    private fun exportPreset(type: LayoutFormatType, shownOnly: Boolean) {
        val preset = GreenhouseData.currentPreset

        if (preset == null) {
            ChatUtils.sendWithPrefix("No preset selected.")
            return
        }

        val format = type.format
        val shown = shownPlot()
        val result = if (!shownOnly && preset.plots.size > 1 && shown != null && !format.isSinglePlot()) {
            format.exportAll(preset)
        } else {
            format.export(shown ?: preset.plots.first())
        }

        copyExportToClipboard(result, format, preset.displayName())
    }

    private fun LayoutFormat.isSinglePlot(): Boolean = this === SkyMutationsFormat || this === SkyShardsFormat

    companion object {
        private const val SOFT_IMPORT_LABEL: String = "Soft import"
        private const val SOFT_IMPORT_TOOLTIP: String = "Soft import tries to fit your imported preset onto the current preset while " +
                "destroying as few plants and replacing as little soil as possible.\nMatters greatly for smaller presets"

        private const val SHOWN_PLOT_LABEL: String = "Shown plot only"
        private const val SHOWN_PLOT_TOOLTIP: String = "Writes only the plot on show instead of every plot in the preset"

        private var softImport: Boolean = false
        private var exportShownOnly: Boolean = false
        private var noRotate: Boolean = false
    }

    private class DeleteChoice(private val label: String, val plot: PlotLayout?) {
        override fun toString(): String = label

        companion object {
            fun choicesOf(preset: GreenhouseLayout): List<DeleteChoice> =
                preset.plots.map { DeleteChoice(preset.plotTitle(it), it) } + DeleteChoice("Whole preset", null)
        }
    }
}
