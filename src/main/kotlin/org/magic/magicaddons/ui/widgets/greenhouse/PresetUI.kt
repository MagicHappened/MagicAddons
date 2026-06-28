package org.magic.magicaddons.ui.widgets.greenhouse

import blazing.chain.LZSEncoding
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
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
) : Renderable, GuiEventListener, HoverableContainer {

    var x: Int = 0
    var y: Int = 0

    override var hoveredElement: GuiEventListener? = null

    @JvmField
    var isFocused: Boolean = false

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

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused

    fun importPreset(type: ImportExportFormatContext.LayoutFormatType) {
        when (type) {
            ImportExportFormatContext.LayoutFormatType.SkyMutations -> {
                val client = Minecraft.getInstance()
                val clipboard = client.keyboardHandler.clipboard
                val encodedLayout = clipboard
                    .substringAfter("layout=", "")
                    .substringBefore("&")
                if (encodedLayout.isBlank()){
                    ChatUtils.sendWithPrefix("Invalid skymutations link.")
                    return
                }
                val decodedLayout = LZSEncoding.decompressFromEncodedURIComponent(encodedLayout)

                if (decodedLayout == null) {
                    ChatUtils.sendWithPrefix("Failed to decode SkyMutations layout.")
                    return
                }
                val jsonArray = JsonParser.parseString(decodedLayout).asJsonArray

                val assignedIdNum = GreenhouseData.computeNextAvailableId()

                val layout = GreenhouseLayout(
                    id = "preset_$assignedIdNum"
                )

                val occupiedPositions = Array(layout.size) {
                    BooleanArray(layout.size)
                }

                jsonArray.forEach { element ->


                    val entry = element.asJsonArray

                    val row = entry[0].asInt
                    val column = entry[1].asInt
                    var cropName = entry[2].asString

                    // a SEED is not a CROP skymutations smh
                    when (cropName) {
                        "Wheat Seeds" -> {
                            cropName = "Wheat"
                        }
                        "Melon Seeds" -> {
                            cropName = "Melon"
                        }
                        "Pumpkin Seeds" -> {
                            cropName = "Pumpkin"
                        }


                    }

                    val markingOrdinal = entry[3].asInt
                    if (occupiedPositions[row][column]) {
                        return@forEach
                    }

                    val marking = LayoutSlot.Marking.entries.getOrNull(markingOrdinal)

                    if (marking == null) {
                        ChatUtils.sendWithPrefix("Unknown marking ordinal: $markingOrdinal")
                        return@forEach
                    }

                    val cropDefinition = CropRegistry.all.find {
                        it.name.equals(cropName, ignoreCase = true)
                    }

                    if (cropDefinition == null) {
                        ChatUtils.sendWithPrefix("Unknown crop: $cropName")
                        return@forEach
                    }

                    val cropWidth = cropDefinition.footprint.width
                    val cropHeight = cropDefinition.footprint.height

                    var topLeftSlot: LayoutSlot? = null
                    for (offsetX in 0 until cropWidth) {
                        for (offsetY in 0 until cropHeight) {
                            try {
                                occupiedPositions[row + offsetY][column + offsetX] = true
                            } catch (e: IndexOutOfBoundsException) {
                                ChatUtils.sendWithPrefix("Malformed data for plant $cropName")
                            }
                            val slot = layout.getSlot(column+offsetX, row+offsetY)
                            slot?.placedBlock = cropDefinition.requiredSoil.firstOrNull()?.defaultBlockState()
                            slot?.slotMark = marking
                            if (offsetX == 0 && offsetY == 0) {
                                topLeftSlot = slot
                            }
                        }
                    }

                    layout.elementInstances.add(
                        GreenhouseElementInstance(
                            cropDefinition.skyblockId?.id ?: cropDefinition.name,
                            topLeftSlot ?: throw IllegalStateException("Top left slot was null for $cropName"),
                            null,
                            null,
                            cropDef = cropDefinition
                        )
                    )
                }

                ChatUtils.sendWithPrefix(
                    "Imported ${layout.elementInstances.size} greenhouse elements from skymutations format"
                )
                onAddPreset.invoke(layout)
            }
            ImportExportFormatContext.LayoutFormatType.MagicAddons -> {
                ChatUtils.sendWithPrefix("Not Yet Implemented")
            }
        }

    }

    fun exportPreset(type: ImportExportFormatContext.LayoutFormatType) {
        if (GreenhouseData.currentPreset == null) {
            ChatUtils.sendWithPrefix("No Preset Selected")
            return
        }
        ChatUtils.sendWithPrefix("exporting?") //todo change this to form a new url with the encoded layout
    }



}