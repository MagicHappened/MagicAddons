package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.MasterLayout
import org.magic.magicaddons.data.greenhouse.transfer.SkyLayoutsFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.transfer.LayoutFormat
import org.magic.magicaddons.data.greenhouse.transfer.MagicAddonsFormat
import org.magic.magicaddons.data.greenhouse.transfer.LayoutTransferResult
import org.magic.magicaddons.data.greenhouse.transfer.SkyMutationsFormat
import org.magic.magicaddons.data.greenhouse.transfer.SkyShardsFormat
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ConfirmContext
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

    private val importButton = ClickableButtonWidget(
        50,
        26,
        Component.literal("Import")
    )

    private val exportButton = ClickableButtonWidget(
        50,
        26,
        Component.literal("Export")
    )

    val applyToButton = ClickableButtonWidget(
        50,
        26,
        Component.literal("Planner")
    )

    val deleteButton = ClickableButtonWidget(
        50,
        26,
        Component.literal("Delete")
    )


    private val newButton = ClickableButtonWidget(26, 26, Component.literal("+"))

    override val buttons: List<ClickableButtonWidget> =
        listOf(newButton, importButton, exportButton, applyToButton, deleteButton)

    override fun onPressed(button: ClickableButtonWidget, mouseButtonEvent: MouseButtonEvent): Boolean {
        if (button === newButton) {
            onNewPreset()
            return true
        }
        if (button === importButton) {
            val context = ImportExportFormatContext(
                mouseButtonEvent.x.toInt(),
                mouseButtonEvent.y.toInt(),
                overlayContext,
                {
                    importPreset(it)
                }
            )
            context.init()
            overlayContext.addContext(context)
            return true
        }
        if (button === exportButton) {
            val context = ImportExportFormatContext(
                mouseButtonEvent.x.toInt(),
                mouseButtonEvent.y.toInt(),
                overlayContext,
                { exportPreset(it)}
            )
            context.init()
            overlayContext.addContext(context)
            return true
        }
        if (button === applyToButton) {
            val context = ApplyToContext(
                mouseButtonEvent.x.toInt(),
                mouseButtonEvent.y.toInt(),
                overlayContext,
                { onAssignedLayout.invoke(shownLayout(), it) }
            )
            context.init()
            overlayContext.addContext(context)
            return true
        }
        if (button === deleteButton) {
            val master = GreenhouseData.currentPreset ?: run {
                ChatUtils.sendWithPrefix("No preset to remove.")
                return true
            }
            val clickX = mouseButtonEvent.x.toInt()
            val clickY = mouseButtonEvent.y.toInt()

            // a preset of several plots is asked which; a preset of one goes straight to the question
            if (master.plots.size > 1) {
                val (menuX, menuY) = OverlayRenderable.placeOnScreen(clickX, clickY, CHOICE_WIDTH, CHOICE_HEIGHT)
                val choice = DeleteChoiceContext(menuX, menuY, overlayContext, master) { plot ->
                    confirmDelete(master, plot, clickX, clickY)
                }
                choice.init()
                overlayContext.addContext(choice)
            } else {
                confirmDelete(master, null, clickX, clickY)
            }
            return true
        }

        return false
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

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = null
        importButton.mouseMoved(mouseX, mouseY)
        exportButton.mouseMoved(mouseX, mouseY)
        applyToButton.mouseMoved(mouseX, mouseY)
        deleteButton.mouseMoved(mouseX, mouseY)
        newButton.mouseMoved(mouseX, mouseY)
        if (hoveredElement == null) {
            if (importButton.isMouseOver(mouseX, mouseY)) {
                hoveredElement = importButton
            }
        }
        if (hoveredElement == null) {
            if (exportButton.isMouseOver(mouseX, mouseY)) {
                hoveredElement = exportButton
            }
        }
        if (hoveredElement == null) {
            if (applyToButton.isMouseOver(mouseX, mouseY)) {
                hoveredElement = applyToButton
            }
        }
        if (hoveredElement == null) {
            if (deleteButton.isMouseOver(mouseX, mouseY)) {
                hoveredElement = deleteButton
            }
        }
    }



    fun importPreset(type: ImportExportFormatContext.LayoutFormatType) {
        val format = formatFor(type)
        val clipboard = Minecraft.getInstance().keyboardHandler.clipboard

        if (!format.canImport(clipboard)) {
            ChatUtils.sendWithPrefix("Your clipboard does not hold a ${format.displayName} layout.")
            return
        }

        val result = format.import(clipboard, "preset_${GreenhouseData.computeNextAvailableId()}")

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

    fun exportPreset(type: ImportExportFormatContext.LayoutFormatType) {
        val preset = GreenhouseData.currentPreset

        if (preset == null) {
            ChatUtils.sendWithPrefix("No Preset Selected")
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
    private fun formatFor(type: ImportExportFormatContext.LayoutFormatType): LayoutFormat =
        when (type) {
            ImportExportFormatContext.LayoutFormatType.SkyMutations -> SkyMutationsFormat
            ImportExportFormatContext.LayoutFormatType.SkyShards -> SkyShardsFormat
            ImportExportFormatContext.LayoutFormatType.SkyLayouts -> SkyLayoutsFormat
            ImportExportFormatContext.LayoutFormatType.MagicAddons -> MagicAddonsFormat
        }



    private companion object {
        /** About what the plot list takes, for keeping it on screen. */
        const val CHOICE_WIDTH: Int = 120
        const val CHOICE_HEIGHT: Int = 100
    }
}
