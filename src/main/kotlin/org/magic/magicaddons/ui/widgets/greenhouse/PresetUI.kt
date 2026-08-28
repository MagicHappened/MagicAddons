package org.magic.magicaddons.ui.widgets.greenhouse

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
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ChatUtils

class PresetUI(
    var width: Int,
    var height: Int,
    val overlayContext: OverlayContext,
    val onAssignedLayout: (assignedLayout: GreenhouseLayout?, selectedGrid: GreenhouseGrid) -> Unit,
    val onAddPreset: (GreenhouseLayout) -> Unit,
    val onRemovePreset: () -> Unit,
) : Renderable, Focusable, HoverableContainer {

    var x: Int = 0
    var y: Int = 0

    override var hoveredElement: GuiEventListener? = null

    override var focusedState: Boolean = false

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
        Component.literal("Apply")
    )

    val deleteButton = ClickableButtonWidget(
        50,
        26,
        Component.literal("Delete")
    )


    fun init() {
        importButton.x = x + 10
        importButton.y = y + 10

        exportButton.x = importButton.x + importButton.width + 10
        exportButton.y = importButton.y

        applyToButton.x = exportButton.x + exportButton.width + 10
        applyToButton.y = y + 10

        deleteButton.x = applyToButton.x + applyToButton.width + 10
        deleteButton.y = y + 10
    }


    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {

        deleteButton.extractRenderState(graphics, mouseX, mouseY, delta)
        importButton.extractRenderState(graphics, mouseX, mouseY, delta)
        exportButton.extractRenderState(graphics, mouseX, mouseY, delta)
        applyToButton.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (importButton.mouseClicked(mouseButtonEvent, doubled)) {
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
        if (exportButton.mouseClicked(mouseButtonEvent, doubled)) {
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
        if (applyToButton.mouseClicked(mouseButtonEvent, doubled)) {
            val context = ApplyToContext(
                mouseButtonEvent.x.toInt(),
                mouseButtonEvent.y.toInt(),
                overlayContext,
                { onAssignedLayout.invoke(GreenhouseData.currentPreset, it) }
            )
            context.init()
            overlayContext.addContext(context)
            return true
        }
        if (deleteButton.mouseClicked(mouseButtonEvent, doubled)) {
            onRemovePreset()
            return true
        }

        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = null
        importButton.mouseMoved(mouseX, mouseY)
        exportButton.mouseMoved(mouseX, mouseY)
        applyToButton.mouseMoved(mouseX, mouseY)
        deleteButton.mouseMoved(mouseX, mouseY)
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
                ChatUtils.sendWithPrefix(
                    "Imported ${result.layout.elementInstances.size} plants from ${format.displayName}"
                )
                onAddPreset.invoke(result.layout)
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
        val result = format.export(preset)

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

    /** The format behind a menu entry. */
    private fun formatFor(type: ImportExportFormatContext.LayoutFormatType): LayoutFormat =
        when (type) {
            ImportExportFormatContext.LayoutFormatType.SkyMutations -> SkyMutationsFormat
            ImportExportFormatContext.LayoutFormatType.SkyShards -> SkyShardsFormat
            ImportExportFormatContext.LayoutFormatType.MagicAddons -> MagicAddonsFormat
        }

}