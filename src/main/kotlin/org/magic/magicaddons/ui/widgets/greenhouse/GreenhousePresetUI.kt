package org.magic.magicaddons.ui.widgets.greenhouse

import blazing.chain.LZSEncoding
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.GreenhouseSlot
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.util.ChatUtils

class GreenhousePresetUI(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    val overlayContext: OverlayContext,
    val selectedPreset: GreenhouseLayout?,
    val currentGrids: List<GreenhouseGrid>,
    val onAssignedLayout: (assignedLayout: GreenhouseLayout?, selectedGrid: GreenhouseGrid) -> Unit,
    val onAddPreset: (GreenhouseLayout) -> Unit
) : Renderable, GuiEventListener, HoverableContainer {

    override var hoveredElement: GuiEventListener? = null
    var contextOpened = false

    @JvmField
    var isFocused: Boolean = false

    private val importButton = ClickableButtonWidget(
        0,
        0,
        60,
        26,
        Component.literal("Import")
    )

    private val exportButton = ClickableButtonWidget(
        0,
        0,
        60,
        26,
        Component.literal("Export")
    )

    val applyToButton = ClickableButtonWidget(
        0,
        0,
        60,
        26,
        Component.literal("Apply To:")
    )


    fun init() {

        importButton.x = x + 10
        importButton.y = y + 10

        exportButton.x = importButton.x + importButton.width + 10
        exportButton.y = importButton.y

        applyToButton.x = exportButton.x + exportButton.width + 10
        applyToButton.y = y + 10
        val font = Minecraft.getInstance().font

    }


    override fun render(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {

        importButton.render(graphics, mouseX, mouseY, delta)
        exportButton.render(graphics, mouseX, mouseY, delta)
        applyToButton.render(graphics, mouseX, mouseY, delta)
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
                { onAssignedLayout.invoke(selectedPreset, it) }

            )
            context.init()
            overlayContext.addContext(context)


            return true
        }
        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = null
        importButton.mouseMoved(mouseX, mouseY)
        exportButton.mouseMoved(mouseX, mouseY)
        applyToButton.mouseMoved(mouseX, mouseY)
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

                    val x = entry[0].asInt
                    val y = entry[1].asInt
                    var cropName = entry[2].asString

                    // a SEED is not a CROP skymutations smh
                    if (cropName == "Melon Seeds")
                        cropName = "Melon"
                    if (cropName == "Pumpkin Seeds")
                        cropName = "Pumpkin"
                    if (cropName == "Wheat Seeds")
                        cropName = "Wheat"

                    val markingOrdinal = entry[3].asInt
                    if (occupiedPositions[y][x]) {
                        return@forEach
                    }

                    val marking = GreenhouseSlot.Marking.entries.getOrNull(markingOrdinal)

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

                    var topLeftSlot: GreenhouseSlot? = null
                    for (offsetX in 0 until cropWidth) {
                        for (offsetY in 0 until cropHeight) {
                            try {
                                occupiedPositions[y + offsetY][x + offsetX] = true
                            } catch (e: IndexOutOfBoundsException) {
                                ChatUtils.sendWithPrefix("Malformed data for plant $cropName")
                            }
                            val slot = layout.getSlot(x+offsetX, y+offsetY)
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
                            null
                        )
                    )
                }

                ChatUtils.sendWithPrefix(
                    "Imported ${layout.elementInstances.size} greenhouse elements from skymutations format"
                )
                onAddPreset.invoke(layout)
            }
            ImportExportFormatContext.LayoutFormatType.MagicAddons -> {


            }
        }

    }

    fun exportPreset(type: ImportExportFormatContext.LayoutFormatType) {
        if (selectedPreset == null) {
            ChatUtils.sendWithPrefix("No Preset Selected")
            return
        }
        ChatUtils.sendWithPrefix("exporting?") //todo change this to form a new url with the encoded layout
    }



}