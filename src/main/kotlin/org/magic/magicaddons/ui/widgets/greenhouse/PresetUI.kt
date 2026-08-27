package org.magic.magicaddons.ui.widgets.greenhouse

import blazing.chain.LZSEncoding
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.ui.Focusable
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
                    cropName = SKYMUTATIONS_NAMES.entries
                        .firstOrNull { it.value == cropName }?.key
                        ?: cropName

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

                    // a crop hanging off the edge is one broken entry, not one broken entry per
                    // cell, so the whole crop is dropped on the first cell that would miss
                    if (row + cropHeight > layout.size || column + cropWidth > layout.size) {
                        ChatUtils.sendWithPrefix("Malformed data for plant $cropName")
                        return@forEach
                    }

                    var topLeftSlot: LayoutSlot? = null
                    for (offsetX in 0 until cropWidth) {
                        for (offsetY in 0 until cropHeight) {
                            occupiedPositions[row + offsetY][column + offsetX] = true
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
        val preset = GreenhouseData.currentPreset

        if (preset == null) {
            ChatUtils.sendWithPrefix("No Preset Selected")
            return
        }

        when (type) {
            ImportExportFormatContext.LayoutFormatType.SkyMutations -> {
                val entries = JsonArray()
                val unsupported = mutableSetOf<String>()

                preset.elementInstances.forEach { instance ->
                    val cropDefinition = instance.cropDef
                    val exportName = SKYMUTATIONS_NAMES[cropDefinition.name] ?: cropDefinition.name

                    // the site drops names it does not know, so a crop it never lists is left out of
                    // the link instead of turning into a silently missing plant
                    if (cropDefinition.name in NOT_ON_SKYMUTATIONS) {
                        unsupported.add(cropDefinition.name)
                        return@forEach
                    }

                    val slot = instance.slot
                    val marking = slot.slotMark ?: LayoutSlot.Marking.Ingredient

                    // skymutations stores one entry per covered cell, a bigger crop is the same name
                    // repeated over its footprint
                    for (offsetY in 0 until cropDefinition.footprint.height) {
                        for (offsetX in 0 until cropDefinition.footprint.width) {
                            val entry = JsonArray()
                            entry.add(slot.y + offsetY)
                            entry.add(slot.x + offsetX)
                            entry.add(exportName)
                            entry.add(marking.ordinal)
                            entries.add(entry)
                        }
                    }
                }

                val encodedLayout = LZSEncoding.compressToEncodedURIComponent(entries.toString())
                Minecraft.getInstance().keyboardHandler.clipboard = "$SKYMUTATIONS_URL$encodedLayout"

                if (unsupported.isNotEmpty()) {
                    ChatUtils.sendWithPrefix(
                        "Left out of the link, skymutations has no ${unsupported.joinToString(", ")}"
                    )
                }

                ChatUtils.sendWithPrefix(
                    "Copied a skymutations link for ${preset.displayName()} to your clipboard"
                )
            }
            ImportExportFormatContext.LayoutFormatType.MagicAddons -> {
                ChatUtils.sendWithPrefix("Not Yet Implemented")
            }
        }
    }

    companion object {
        /** Where a shared skymutations layout lives, the encoded layout is appended to it. */
        private const val SKYMUTATIONS_URL: String = "https://skymutations.eu/greenhouse?layout="

        /**
         * Crops this mod names differently than skymutations does, keyed by the name used here.
         * Skymutations names the three vanilla crops after the seed they are planted from.
         */
        private val SKYMUTATIONS_NAMES: Map<String, String> = mapOf(
            "Wheat" to "Wheat Seeds",
            "Melon" to "Melon Seeds",
            "Pumpkin" to "Pumpkin Seeds",
            "Dead Plant" to "Dead Plants"
        )

        /**
         * Crops this mod knows that skymutations has no entry for at all. Helianthus is planted in
         * the greenhouse but the site only lists the condensed version, which is a different item.
         */
        private val NOT_ON_SKYMUTATIONS: Set<String> = setOf(
            "Cropie",
            "Squash",
            "Helianthus",
            "DevourerRoots"
        )
    }



}