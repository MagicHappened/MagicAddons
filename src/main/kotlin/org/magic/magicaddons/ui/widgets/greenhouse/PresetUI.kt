package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.MasterLayout
import org.magic.magicaddons.data.greenhouse.transfer.SkyLayoutsFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.transfer.LayoutFormat
import org.magic.magicaddons.data.greenhouse.transfer.ShareCodeFormat
import org.magic.magicaddons.data.greenhouse.transfer.LayoutTransferResult
import org.magic.magicaddons.data.greenhouse.transfer.SkyMutationsFormat
import org.magic.magicaddons.data.greenhouse.transfer.SkyShardsFormat
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.ui.widgets.PickContext
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ChatUtils

class PresetUI(
    val overlayContext: OverlayContext,
    val onAssignedLayout: (assignedLayout: GreenhouseLayout?, selectedGrid: GreenhouseGrid) -> Unit,
    val onImported: (LayoutTransferResult.Imported) -> Unit,
    /** Takes a plot off the preset, or with null the whole preset. */
    val onRemove: (GreenhouseLayout?) -> Unit,
    /** Starts a preset with one empty plot. */
    val onNewPreset: () -> Unit,
    /** What the Delete button is about: the shown plot of a master layout, or the preset itself. */
    val shownLayout: () -> GreenhouseLayout?,
) : ActionPanel() {

    private val importButton = ClickableButtonWidget("Import")
    private val exportButton = ClickableButtonWidget("Export")
    private val applyToButton = ClickableButtonWidget("Planner")
    private val deleteButton = ClickableButtonWidget("Delete")
    private val newButton = ClickableButtonWidget(ClickableButtonWidget.HEIGHT, ClickableButtonWidget.HEIGHT, Component.literal("+"))

    override val buttons: List<ClickableButtonWidget> =
        listOf(newButton, importButton, exportButton, applyToButton, deleteButton)

    override fun onPressed(button: ClickableButtonWidget, event: MouseButtonEvent): Boolean {
        when (button) {
            newButton -> onNewPreset()
            importButton -> openFormatMenu(event) { importPreset(it) }
            exportButton -> openFormatMenu(event) { exportPreset(it) }
            applyToButton -> openMenu(event, "Assign To:", assignTargets()) { onAssignedLayout(shownLayout(), it) }
            deleteButton -> askDelete(event)
            else -> return false
        }
        return true
    }

    /** The greenhouses to assign to, the one being stood in first and the rest in their own order. */
    private fun assignTargets(): List<GreenhouseGrid> {
        val current = GreenhouseData.getCurrentGrid()
        return GreenhouseData.greenhouseGrids.sortedByDescending { it === current }
    }

    /** The list of formats at the mouse; the picked one goes to [onPick]. */
    private fun openFormatMenu(event: MouseButtonEvent, onPick: (LayoutFormatType) -> Unit) =
        openMenu(event, "Format:", LayoutFormatType.entries, onPick)

    private fun <T> openMenu(event: MouseButtonEvent, title: String, values: List<T>, onPick: (T) -> Unit) {
        val menu = PickContext(event.x.toInt(), event.y.toInt(), title, values, overlayContext, onPick)
        menu.init()
        overlayContext.addContext(menu)
    }

    /** A preset of several plots is asked which; a preset of one goes straight to the question. */
    private fun askDelete(event: MouseButtonEvent) {
        val master = GreenhouseData.currentPreset ?: run {
            ChatUtils.sendWithPrefix("No preset to remove.")
            return
        }
        val clickX = event.x.toInt()
        val clickY = event.y.toInt()

        if (master.plots.size > 1) {
            openMenu(event, "Delete:", DeleteChoice.choicesOf(master)) { confirmDelete(master, it.plot, clickX, clickY) }
        } else {
            confirmDelete(master, null, clickX, clickY)
        }
    }

    /** The yes or no before anything is deleted; no, or a click elsewhere, deletes nothing. */
    private fun confirmDelete(master: MasterLayout, plot: GreenhouseLayout?, clickX: Int, clickY: Int) {
        val question = if (plot != null) {
            "Delete ${master.plotTitle(plot)} from ${master.displayName()}?"
        } else {
            "Delete preset ${master.displayName()}?"
        }
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(clickX, clickY, ConfirmContext.widthFor(question), ConfirmContext.HEIGHT)
        overlayContext.addContext(ConfirmContext(menuX, menuY, question, overlayContext) { onRemove(plot) })
    }

    private fun importPreset(type: LayoutFormatType) {
        val format = formatFor(type)
        val clipboard = Minecraft.getInstance().keyboardHandler.clipboard

        if (!format.canImport(clipboard)) {
            ChatUtils.sendWithPrefix("Your clipboard does not hold a ${format.displayName} layout.")
            return
        }

        val result = format.import(clipboard, GreenhouseLayout.presetId(GreenhouseData.computeNextAvailableId()))

        result.notes.forEach { ChatUtils.sendWithPrefix(it) }

        when (result) {
            is LayoutTransferResult.Failure -> ChatUtils.sendWithPrefix(result.reason)
            is LayoutTransferResult.Imported -> {
                val plants = result.plots.sumOf { it.elementInstances.size }
                val plots = if (result.plots.size > 1) " over ${result.plots.size} plots" else ""
                ChatUtils.sendWithPrefix("Imported $plants plants$plots from ${format.displayName}")
                onImported.invoke(result)
            }
            is LayoutTransferResult.Exported -> Unit
        }
    }

    private fun exportPreset(type: LayoutFormatType) {
        val preset = GreenhouseData.currentPreset

        if (preset == null) {
            ChatUtils.sendWithPrefix("No preset selected.")
            return
        }

        val format = formatFor(type)
        val shown = shownLayout()
        val result = if (preset.plots.size > 1 && shown != null && !format.isSinglePlot()) {
            format.exportAll(preset)
        } else {
            format.export(shown ?: preset.plots.first())
        }

        result.notes.forEach { ChatUtils.sendWithPrefix(it) }

        when (result) {
            is LayoutTransferResult.Failure -> ChatUtils.sendWithPrefix(result.reason)
            is LayoutTransferResult.Exported -> {
                Minecraft.getInstance().keyboardHandler.clipboard = result.text
                ChatUtils.sendWithPrefix(
                    "Copied a ${format.displayName} layout for ${preset.displayName()} to your clipboard"
                )
            }
            is LayoutTransferResult.Imported -> Unit
        }
    }

    /** Whether a format writes one plot only, in which case the shown plot is what goes out. */
    private fun LayoutFormat.isSinglePlot(): Boolean = this === SkyMutationsFormat || this === SkyShardsFormat

    /** The format behind a menu entry. */
    private fun formatFor(type: LayoutFormatType): LayoutFormat =
        when (type) {
            LayoutFormatType.SkyMutations -> SkyMutationsFormat
            LayoutFormatType.SkyShards -> SkyShardsFormat
            LayoutFormatType.SkyLayouts -> SkyLayoutsFormat
            LayoutFormatType.MagicAddons -> ShareCodeFormat
        }
}
