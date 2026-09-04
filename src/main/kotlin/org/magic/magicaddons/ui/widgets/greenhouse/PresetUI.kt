package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.ui.widgets.EnumWidget
import org.magic.magicaddons.Common
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
    val onAddPreset: (GreenhouseLayout) -> Unit,
    val onRemovePreset: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
) : ActionPanel() {

    /** What a click on a plant does while the selector is on something other than Off. */
    enum class MarkChoice(private val label: String, val marking: LayoutSlot.Marking?, val applies: Boolean) {
        Off("Mark: off", null, false),
        Target("Mark: Target", LayoutSlot.Marking.Target, true),
        Ingredient("Mark: Ingredient", LayoutSlot.Marking.Ingredient, true),
        Unique("Mark: Unique crop", LayoutSlot.Marking.UniqueCrop, true),
        Clear("Mark: clear", null, true);

        override fun toString(): String = label
    }

    private val markSelector = EnumWidget(
        values = MarkChoice.entries,
        currentValue = MarkChoice.Off,
        overlayContext = overlayContext,
        searchable = false
    )

    val markChoice: MarkChoice get() = markSelector.currentValue ?: MarkChoice.Off

    private val undoButton = ClickableButtonWidget(ARROW_SIZE, ARROW_SIZE, Component.literal("←"))
    private val redoButton = ClickableButtonWidget(ARROW_SIZE, ARROW_SIZE, Component.literal("→"))

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


    override val buttons: List<ClickableButtonWidget> =
        listOf(importButton, exportButton, applyToButton, deleteButton, undoButton, redoButton)

    /** The action buttons wrap along the top; the mark selector and the arrows take a row under them. */
    override fun layoutIn(x: Int, y: Int, width: Int, height: Int) {
        super.layoutIn(x, y, width, height)

        val actionBottom = listOf(importButton, exportButton, applyToButton, deleteButton).maxOf { it.y + it.height }
        val rowY = actionBottom + Common.UI.SPACING

        redoButton.x = x + width - PADDING - ARROW_SIZE
        redoButton.y = rowY
        undoButton.x = redoButton.x - Common.UI.SPACING - ARROW_SIZE
        undoButton.y = rowY

        markSelector.x = x + PADDING
        markSelector.y = rowY
        markSelector.height = ARROW_SIZE
        markSelector.width = (undoButton.x - Common.UI.SPACING - markSelector.x).coerceAtLeast(60)
    }

    override fun renderContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        markSelector.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (markSelector.mouseClicked(mouseButtonEvent, doubled)) return true
        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun onPressed(button: ClickableButtonWidget, mouseButtonEvent: MouseButtonEvent): Boolean {
        if (button === undoButton) {
            onUndo()
            return true
        }
        if (button === redoButton) {
            onRedo()
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
                { onAssignedLayout.invoke(GreenhouseData.currentPreset, it) }
            )
            context.init()
            overlayContext.addContext(context)
            return true
        }
        if (button === deleteButton) {
            val preset = GreenhouseData.currentPreset ?: run {
                ChatUtils.sendWithPrefix("No preset to remove.")
                return true
            }
            val question = "Delete preset ${preset.displayName()}?"
            val (menuX, menuY) = OverlayRenderable.placeOnScreen(
                mouseButtonEvent.x.toInt(),
                mouseButtonEvent.y.toInt(),
                ConfirmContext.widthFor(question),
                ConfirmContext.HEIGHT
            )
            overlayContext.addContext(ConfirmContext(menuX, menuY, question, overlayContext) { onRemovePreset() })
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
        undoButton.mouseMoved(mouseX, mouseY)
        redoButton.mouseMoved(mouseX, mouseY)
        markSelector.mouseMoved(mouseX, mouseY)
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


    private companion object {
        /** The undo and redo arrows are square, the height of the selector beside them. */
        const val ARROW_SIZE: Int = 22
    }
}
