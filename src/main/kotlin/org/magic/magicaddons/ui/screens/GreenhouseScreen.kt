package org.magic.magicaddons.ui.screens

import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.CompletableFuture
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.Footprint
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.plot.GREENHOUSE_SIZE
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.data.greenhouse.plot.SoftImport
import org.magic.magicaddons.data.greenhouse.transfer.LayoutTransferResult
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.PlannerNeeds
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.BioanalysisAccessory
import org.magic.magicaddons.features.farming.greenhousePresets.render.PlantHighlight
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.ui.widgets.EnumWidget
import org.magic.magicaddons.ui.widgets.PickContext
import org.magic.magicaddons.ui.widgets.SliderWidget
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.greenhouse.ActionPanel
import org.magic.magicaddons.ui.widgets.greenhouse.Bookmarks
import org.magic.magicaddons.ui.widgets.greenhouse.EditLayoutContextMenu
import org.magic.magicaddons.ui.widgets.greenhouse.ElementWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhousePanel
import org.magic.magicaddons.ui.widgets.greenhouse.GridWidget
import org.magic.magicaddons.ui.widgets.greenhouse.HoverControls
import org.magic.magicaddons.ui.widgets.greenhouse.LayoutFormatType
import org.magic.magicaddons.ui.widgets.greenhouse.MarkOption
import org.magic.magicaddons.ui.widgets.greenhouse.PaletteItem
import org.magic.magicaddons.ui.widgets.greenhouse.PlantPalette
import org.magic.magicaddons.ui.widgets.greenhouse.PresetUI
import org.magic.magicaddons.ui.widgets.greenhouse.ScrollHint
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.commands.internal.farming.SetTimestalkAttribute
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.at
import org.magic.magicaddons.util.ScreenUtil.boxHeight
import org.magic.magicaddons.util.ScreenUtil.component4
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.drawShelf
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.drawTooltipLines
import org.magic.magicaddons.util.ScreenUtil.drawTooltipLinesAtCursor
import org.magic.magicaddons.util.ScreenUtil.drawWarningBadge
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.modText
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem
import org.magic.magicaddons.util.ScreenUtil.itemStackFor
import org.magic.magicaddons.util.toReadableDuration
import org.magic.magicaddons.util.toShortDuration
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

class GreenhouseScreen : MagicAddonsScreen(Component.literal("Greenhouse Screen"), "the greenhouse screen"), HoverableContainer, OverlayContext {

    override val backgroundImageName: String = Customization.GREENHOUSE_SCREEN


    enum class DisplayMode {
        Greenhouses,
        Presets
    }

    private var topMargin: Int = 0
    private var gridLeft: Int = 0
    private var gridTop: Int = 0
    private var gridSpan: Int = 0

    private var currentDisplay: DisplayMode
        get() = lastDisplay
        set(value) { lastDisplay = value }

    private val isShowingGuessedGrowth: Boolean
        get() = currentDisplay == DisplayMode.Greenhouses &&
                GreenhouseData.greenhouseGrids.getOrNull(GreenhouseData.currentGridIndex)
                    ?.let { it.state.ticksSinceLastScan > 0 } == true

    // half the size if there is not enough space
    private var drawScale: Float = 1f

    override var hoveredElement: GuiEventListener? = null

    override val overlays = mutableListOf<OverlayRenderable>()
    private var displayedGridWidget: GridWidget? = null
    private val greenhouseGridWidgets: MutableList<GridWidget> = mutableListOf()
    private val presetGridWidgets: MutableList<GridWidget> = mutableListOf()

    private val plotsButton = ClickableButtonWidget(TOGGLE_PLOTS)
    private val presetsButton = ClickableButtonWidget(TOGGLE_PRESETS)

    private val greenhousePanel = GreenhousePanel(
        onUnplan = {
            displayedGrid()?.let { GreenhouseData.unplanGreenhouse(it) }
            initGreenhouseLayout()
        },
        onPickPreset = { event -> openAssignMenu(event) },
        onTurnPlan = { turnPlan() },
        onEditPreset = { editAssignedPreset() },
        onSaveAsPreset = { saveGreenhouseAsPreset() },
        onRescan = { askRescan(it) },
        onExport = { askExport(it) }
    )

    private var actionRowX: Int = 0
    private var actionRowY: Int = 0

    private var shelfLeft: Int = 0
    private var shelfWidth: Int = 0
    private var viewShelfY: Int = 0
    private var viewShelfHeight: Int = 0
    private var predictShelfY: Int = 0
    private var predictShelfHeight: Int = 0
    private var actionShelfY: Int = 0

    private var gridWidgetBeforePrediction: GridWidget? = null

    private val predictSlider = SliderWidget { showPrediction(it) }

    private var tickTimeHovered = false

    private var tickTimePinned = false

    private var tickTimeBox: IntArray = IntArray(4)

    private var nameBoxBounds: IntArray = IntArray(4)
    private var isWarningHovered = false

    private val presetSelector = EnumWidget(
        values = emptyList<GreenhouseLayout>(),
        currentValue = null as GreenhouseLayout?,
        onRightClickValue = { clickedPreset, event ->
            clickedPreset?.let { openRenameContext(event, it.displayName()) { name -> it.name = name } }
        },
        valueChanged = { presetChanged(it) },
        overlayContext = this
    )

    private val presetUI = PresetUI(
        this,
        onAssignedLayout = { assignedLayout, selectedGrid ->
            assignPresetLayout(assignedLayout, selectedGrid)
        },
        onImported = { result, soft -> applyImport(result, soft) },
        onRemove = { removePresetLayout(it) },
        onNewPreset = { newPreset() },
        shownLayout = { displayedGridWidget?.layout },
        onTurn = { turnShownPlot(it) }
    )

    private var displayedName: String = "Error loading name."

    private var slotSize: Int = 0

    private val hoverControls = HoverControls()

    private val scrollHint = ScrollHint(SCROLL_HINT_GREENHOUSES)

    private val plotTabs = Bookmarks<PlotLayout>(
        side = Bookmarks.Side.Top,
        label = { it.displayName() },
        onPick = { layout, event ->
            if (event.button() == 1) {
                openRenameContext(event, layout.displayName()) { name -> layout.name = name }
            } else {
                showGreenhouse(layout)
            }
        }
    )

    private val teleportTab = Bookmarks<String>(
        side = Bookmarks.Side.Bottom,
        label = { it },
        onPick = { _, _ ->
            displayedGridWidget?.layout
                ?.takeIf { it.kind == PlotLayout.Kind.PLOT_PRESET }
                ?.number
                ?.let { ChatUtils.sendCommand("tptoplot $it") }
        }
    ).apply { items = listOf(TELEPORT_LABEL) }

    private val plantPalette = PlantPalette(
        this,
        onClearAll = { askClearPlot(it) },
        onUndo = { undo() },
        onRedo = { redo() },
        uniqueMissing = { uniqueMissingFromPreset(it) }
    )

    private var lastPaintedCell: Pair<Int, Int>? = null

    private sealed interface PresetPlotTab {
        data class Part(val layout: PlotLayout) : PresetPlotTab
        data object Add : PresetPlotTab
    }

    private fun presetPlotTitle(layout: PlotLayout): String =
        GreenhouseData.greenhouseLayoutFor(layout)?.plotTitle(layout) ?: layout.displayName()

    private var shownPlot: PlotLayout? = null

    private val presetPlotTabs = Bookmarks<PresetPlotTab>(
        side = Bookmarks.Side.Top,
        label = { tab -> if (tab is PresetPlotTab.Part) presetPlotTitle(tab.layout) else "+" },
        tooltip = { tab -> if (tab is PresetPlotTab.Part) "Right click to rename" else "Add a plot to this preset" },
        onPick = { tab, event ->
            when {
                tab is PresetPlotTab.Add -> addPlot()
                tab is PresetPlotTab.Part && event.button() == 1 ->
                    openRenameContext(event, presetPlotTitle(tab.layout)) { name -> tab.layout.name = name }
                tab is PresetPlotTab.Part -> {
                    shownPlot = tab.layout
                    initPresetLayout()
                }
            }
        }
    )

    private var emptyGridWidget: GridWidget? = null

    private var isPresetCleared = false

    private val cropPreviewButton = ClickableButtonWidget("Crop Preview")

    override fun onInit() {
        super.onInit()

        drawScale = DRAW_SCALES.first { it == DRAW_SCALES.last() ||
                width / it >= COMFORTABLE_WIDTH && height / it >= COMFORTABLE_HEIGHT } *
                Customization.uiScale
        width = (width / drawScale).toInt()
        height = (height / drawScale).toInt()

        initBaseLayout()

        GreenhouseData.warnUnknownValues()
    }

    private fun initBaseLayout() {
        topMargin = height / 10

        gridTop = maxOf(topMargin, NAME_TOP + boxHeight(" ") + Common.UI.SPACING + Bookmarks.REACH + BORDER_PADDING)
        val spaceBelowGrid = Bookmarks.REACH + Common.UI.SPACING + cropPreviewButton.height + Common.UI.SPACING_LARGE

        val spaceBesideGrid = TOOLBAR_WIDTH + HoverControls.TOTAL_WIDTH + Common.UI.SPACING_LARGE * 2
        val gridHeightAvailable = height - gridTop - BORDER_PADDING * 2 - spaceBelowGrid
        val gridWidthAvailable = width - spaceBesideGrid - BORDER_PADDING * 2

        slotSize = GridWidget.slotSizeFor(minOf(gridHeightAvailable, gridWidthAvailable), GREENHOUSE_SIZE)
            .coerceAtLeast(MIN_SLOT_SIZE)

        gridSpan = GridWidget.spanFor(slotSize, GREENHOUSE_SIZE)

        gridLeft = ((width - gridSpan) / 2).coerceAtLeast(TOOLBAR_WIDTH + Common.UI.SPACING_LARGE)

        layoutShelves()

        cropPreviewButton.x = (width - cropPreviewButton.width) / 2
        cropPreviewButton.y = height - cropPreviewButton.height - Common.UI.SPACING_LARGE - 2

        layoutGreenhouseWidgets()
        layoutPresetWidgets()

        when (currentDisplay) {
            DisplayMode.Greenhouses -> initGreenhouseLayout()
            DisplayMode.Presets -> initPresetLayout()
        }
    }


    private fun layoutShelves() {
        shelfLeft = Common.UI.SPACING_LARGE
        shelfWidth = (gridLeft - BORDER_PADDING - Common.UI.SPACING_LARGE - shelfLeft).coerceAtLeast(MIN_ACTION_ROW_WIDTH)
        viewShelfY = gridTop - BORDER_PADDING

        plotsButton.x = shelfLeft + ActionPanel.PADDING
        plotsButton.y = viewShelfY + shelfTitleHeight() + ActionPanel.PADDING
        presetsButton.x = plotsButton.x + plotsButton.width + Common.UI.SPACING
        presetsButton.y = plotsButton.y

        presetSelector.x = presetsButton.x + presetsButton.width + Common.UI.SPACING
        presetSelector.y = plotsButton.y
        presetSelector.height = plotsButton.height
        presetSelector.closeList()

        viewShelfHeight = shelfTitleHeight() + ActionPanel.PADDING * 2 + plotsButton.height

        predictShelfY = viewShelfY + viewShelfHeight + Common.UI.SPACING_LARGE
        predictShelfHeight = if (currentDisplay == DisplayMode.Greenhouses) {
            shelfTitleHeight() + ActionPanel.PADDING * 2 + SliderWidget.HEIGHT + Common.UI.SPACING + font.lineHeight * 2
        } else {
            0
        }

        predictSlider.x = shelfLeft + ActionPanel.PADDING
        predictSlider.y = predictShelfY + shelfTitleHeight() + ActionPanel.PADDING
        predictSlider.width = (shelfWidth - ActionPanel.PADDING * 2 - PREDICT_LABEL_WIDTH)
            .coerceAtLeast(SliderWidget.HANDLE_WIDTH * 2)
        predictSlider.range(0, MAX_PREDICT_TICKS)

        actionShelfY = predictShelfY +
                if (predictShelfHeight > 0) predictShelfHeight + Common.UI.SPACING_LARGE else 0

        actionRowX = shelfLeft
        actionRowY = actionShelfY + shelfTitleHeight()
    }

    
    private fun layoutGreenhouseWidgets() {
        plotTabs.layoutAlong(gridLeft - BORDER_PADDING, gridTop - BORDER_PADDING, gridSpan + BORDER_PADDING * 2)

        teleportTab.layoutAlong(
            gridLeft + (gridSpan - TELEPORT_WIDTH) / 2,
            gridTop + gridSpan + BORDER_PADDING,
            TELEPORT_WIDTH
        )

        greenhousePanel.layoutIn(actionRowX, actionRowY, shelfWidth)
        hoverControls.layoutAgainstGrid(gridLeft + gridSpan, gridTop, gridSpan)
    }

    private fun layoutPresetWidgets() {
        presetPlotTabs.layoutAlong(gridLeft - BORDER_PADDING, gridTop - BORDER_PADDING, gridSpan + BORDER_PADDING * 2)

        presetUI.layoutIn(actionRowX, actionRowY, shelfWidth)

        val paletteY = actionShelfY + shelfTitleHeight() + presetUI.contentHeight + UNPLANNED_LINE_HEIGHT + Common.UI.SPACING_LARGE
        val frameBottom = gridTop + gridSpan + BORDER_PADDING
        plantPalette.layout(shelfLeft, paletteY, shelfWidth, frameBottom - paletteY)

        emptyGridWidget = newGridWidget(PlotLayout(id = EMPTY_GRID_ID), turns = 0)
    }

    
    private fun newGridWidget(layout: PlotLayout, turns: Int): GridWidget =
        GridWidget(layout, slotSize).apply {
            this.turns = turns
            x = gridLeft
            y = gridTop
            targetPlan = if (layout.kind == PlotLayout.Kind.GREENHOUSE_PRESET) {
                { layout }
            } else {
                { GreenhouseData.greenhouseGrids.firstOrNull { it.layout === layout }?.let { grid -> grid.state.assignedLayout?.turnedBy(grid.state.planTurns) } }
            }
            init()
        }

    private fun initGreenhouseLayout() {
        dropPrediction()
        displayedGridWidget = null
        hoveredElement = null
        greenhouseGridWidgets.clear()
        val scannedGreenhouseCount = GreenhouseData.greenhouseGrids.count { it.state.lastScanTime != null }
        if (PlotAPI.plots.any { it.data == null }) {
            if (!LocationAPI.isOnSkyBlock) {
                ChatUtils.sendWithPrefix("Plot data is null, please join skyblock.")
            } else {
                ChatUtils.sendWithCommand(
                    "Plot data is null, please open /desk and go to \"configure plots\" to load it.",
                    "/desk"
                )
            }
            return
        }
        if (scannedGreenhouseCount != PlotAPI.plots.count { it.data?.isGreenhouse ?: throw IllegalStateException("Plot data was null after null check.") }){
            if (!warnedMissingGreenhouses){
                warnedMissingGreenhouses = true
                ChatUtils.sendWithPrefix("Not all greenhouses available, enter them to see them.")
            }
        }

        val currentPlot = PlotAPI.getCurrentPlot()?.id
        GreenhouseData.greenhouseGrids.forEachIndexed { index, grid ->
            if (grid.state.lastScanTime == null) return@forEachIndexed
            greenhouseGridWidgets.add(newGridWidget(grid.layout, gridTurns()))
            if (grid.layout.kind == PlotLayout.Kind.PLOT_PRESET && grid.layout.number == currentPlot) {
                GreenhouseData.currentGridIndex = index
            }
        }
        

        val currentLayout = GreenhouseData.greenhouseGrids
            .getOrNull(GreenhouseData.currentGridIndex)?.layout

        displayedGridWidget = greenhouseGridWidgets.find { it.layout === currentLayout }
            ?: greenhouseGridWidgets.firstOrNull()
        
        if (displayedGridWidget == null) return

        displayedName = displayedGridWidget?.layout?.displayName() ?: "Unknown Plot"

        plotTabs.items = greenhouseGridWidgets.map { it.layout }
        plotTabs.selected = displayedGridWidget?.layout

        layoutNameBox()
    }

    private fun initPresetLayout() {
        dropPrediction()
        presetGridWidgets.clear()
        displayedGridWidget = null
        hoveredElement = null

        if (GreenhouseData.currentPreset == null && !isPresetCleared) {
            GreenhouseData.currentPreset = GreenhouseData.presetGrids.firstOrNull()
        }
        val shownPreset = GreenhouseData.currentPreset?.takeUnless { isPresetCleared }
        if (shownPreset != null && shownPlot !in shownPreset.plots) shownPlot = null

        shownPreset?.plots?.forEach { plot ->
            presetGridWidgets.add(newGridWidget(plot, turns = 0))
        }
        val shown = shownPlot ?: shownPreset?.plots?.firstOrNull()
        displayedGridWidget = presetGridWidgets.find { it.layout === shown }

        presetPlotTabs.items = if (shownPreset == null) emptyList() else buildList {
            shownPreset.plots.forEach { add(PresetPlotTab.Part(it)) }
            if (shownPreset.plots.size < GreenhouseLayout.MAX_PLOTS) add(PresetPlotTab.Add)
        }
        presetPlotTabs.selected = shown?.let { PresetPlotTab.Part(it) }

        displayedName = shownPreset?.displayName() ?: "Unknown Preset"

        presetSelector.currentValue = shownPreset
        presetSelector.values = GreenhouseData.presetGrids.toList()
        relayoutPresetSelector()

        layoutNameBox()
    }

    private fun relayoutPresetSelector() {
        val widthToShelfEdge = shelfLeft + shelfWidth - ActionPanel.PADDING - Common.UI.SPACING - ScrollHint.SIZE - presetSelector.x
        presetSelector.fitToValues(widthToShelfEdge)
    }

    private fun layoutNameBox() {
        val boxHeight = boxHeight(displayedName)
        val boxWidth = font.width(displayedName) + Common.UI.TEXT_X_PAD * 2
        val left = (width - boxWidth) / 2
        nameBoxBounds = intArrayOf(left, NAME_TOP, left + boxWidth + Common.UI.SPACING + boxHeight, NAME_TOP + boxHeight)
    }

    private fun isOverNameBox(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, nameBoxBounds[0], nameBoxBounds[1], nameBoxBounds[2] - nameBoxBounds[0], nameBoxBounds[3] - nameBoxBounds[1])

    
    private fun drawNameBox(graphics: GuiGraphicsExtractor) {
        val boxHeight = nameBoxBounds[3] - nameBoxBounds[1]
        graphics.drawMultilineBoxCentered(
            displayedName,
            width / 2,
            NAME_TOP + boxHeight / 2,
            if (isShowingGuessedGrowth) Common.UI.WARNING_COLOR else null
        )
        if (isShowingGuessedGrowth) graphics.drawWarningBadge(warningBadgeX(), NAME_TOP, boxHeight)
    }

    private fun shelfTitleHeight(): Int = font.lineHeight + Common.UI.SPACING * 2

    
    private fun drawPinnedTickTime(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val lines = tickTimeTooltip().split('\n').map { Component.literal(it).visualOrderText }
        pinnedTickTimeBox = graphics.drawTooltipLines(lines, TICK_TIME_LEFT, tickTimeBox[3] + Common.UI.SPACING)

        if (isOverTickTimeLine(mouseX, mouseY, UNIQUE_LINE)) {
            drawMissingUniques(graphics, mouseX, mouseY)
        }

        if (isOverTickTimeLine(mouseX, mouseY, ATTRIBUTE_LINE)) {
            graphics.drawTooltipAtCursor(SET_ATTRIBUTE_HINT, mouseX, mouseY)
        }
    }

    private var pinnedTickTimeBox: IntArray = IntArray(4)

    private fun isOverTickTimeLine(mouseX: Int, mouseY: Int, line: Int): Boolean {
        val box = pinnedTickTimeBox
        val lineTop = box[1] + ScreenUtil.TOOLTIP_PAD + line * font.lineHeight

        return inRect(mouseX, mouseY, box[0], lineTop, box[2] - box[0], font.lineHeight)
    }
    private class UniqueLine(val crops: List<CropDefinition>, val text: String)

    private fun missingUniques(): List<UniqueLine> = GreenhouseData.getMissingUniques().map { key ->
        fun named(vararg names: String) = names.mapNotNull { name -> CropRegistry.allCrops.find { it.name == name } }
        when (key) {
            is GreenhouseData.UniqueCropKey.Def -> {
                val def = CropRegistry.findByIdOrName(key.id)
                UniqueLine(listOfNotNull(def), def?.name ?: key.id)
            }
            GreenhouseData.UniqueCropKey.Flower -> UniqueLine(named("Moonflower", "Sunflower"), "Moonflower/Sunflower")
            GreenhouseData.UniqueCropKey.Mushroom -> UniqueLine(named("Red Mushroom", "Brown Mushroom"), "Red Mushroom/Brown Mushroom")
        }
    }.sortedBy { it.text }

    private fun drawMissingUniques(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val lines = missingUniques()
        val heading = if (lines.isEmpty()) "§aEvery unique crop is growing" else "§7Missing unique crops:"
        val texts = (listOf(heading) + lines.map { it.text }).map { Component.literal(it).visualOrderText }

        graphics.drawTooltipLinesAtCursor(texts, mouseX, mouseY) { index ->
            lines.getOrNull(index - 1)?.crops?.map { itemStackFor(it) } ?: emptyList()
        }
    }

    private fun tickTimeTooltip(): String {
        val misc = GreenhouseData.miscInfo

        fun coloredOutOf(value: Int?, max: Int): String {
            value ?: return "§8?§7/$max"
            val tripled = value * 3
            val colour = when {
                tripled < max -> "§c"
                tripled < max * 2 -> "§e"
                else -> "§a"
            }
            return "$colour$value§7/$max"
        }

        val tickTime = GreenhouseTickTime.tickMs?.let { ms ->
            val seconds = ms / 1000
            "§f%dh %dm %ds".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
        } ?: "§8?"

        return listOf(
            "§7Your tick time: $tickTime",
            "§7Unique crops: " + coloredOutOf(GreenhouseData.getCurrentUniques().size, MAX_UNIQUE_CROPS),
            "§7Greenhouse speed upgrade: " + coloredOutOf(misc.cropSpeedUpgradeValue, MAX_SPEED_UPGRADE),
            "§7Greenhouse attribute: " + coloredOutOf(GreenhouseTickTime.speedAttribute(), MAX_ATTRIBUTE),
            "§7Crop growth: §f" + (misc.cropGrowthValue?.toString() ?: "§8?")
        ).joinToString("\n")
    }

    private fun renderDropTarget(graphics: GuiGraphicsExtractor) {
        val item = plantPalette.carried ?: return
        val grid = displayedGridWidget ?: emptyGridWidget ?: return
        val mouse = Minecraft.getInstance().mouseHandler
        val window = Minecraft.getInstance().window
        val mouseX = mouse.xpos() * window.guiScaledWidth / window.screenWidth / drawScale
        val mouseY = mouse.ypos() * window.guiScaledHeight / window.screenHeight / drawScale
        val (sx, sy) = grid.slotAt(mouseX, mouseY) ?: return

        val footprint = (item as? PaletteItem.Crop)?.def?.footprint ?: Footprint(1, 1)
        val (x1, y1, x2, y2) = grid.footprintRect(sx, sy, footprint)
        val fits = item !is PaletteItem.Crop || fitsInsideGrid(grid.layout, item.def, sx, sy)
        graphics.fill(x1, y1, x2, y2, if (fits) DROP_OK else DROP_BLOCKED)
    }

    private fun fitsInsideGrid(layout: PlotLayout, def: CropDefinition, sx: Int, sy: Int): Boolean {
        val footprint = def.footprint
        return sx + footprint.width <= layout.size && sy + footprint.height <= layout.size
    }

    private fun plantsOverlapping(layout: PlotLayout, def: CropDefinition, sx: Int, sy: Int): List<Plant> {
        val footprint = def.footprint
        return layout.plants.filter { other ->
            val otherWidth = other.cropDef.footprint.width
            val otherHeight = other.cropDef.footprint.height
            sx < other.slot.x + otherWidth && other.slot.x < sx + footprint.width &&
                    sy < other.slot.y + otherHeight && other.slot.y < sy + footprint.height
        }
    }

    private fun pickFromCell(event: MouseButtonEvent): Boolean {
        val grid = displayedGridWidget ?: return false
        val (sx, sy) = grid.slotAt(event.x, event.y) ?: return false

        val plant = grid.layout.plantCovering(sx, sy)
        val picked = when {
            plant != null -> PaletteItem.Crop(plant.cropDef)
            else -> grid.layout.getSlot(sx, sy)?.soil?.let { PaletteItem.Soil(it) }
        } ?: return false

        plantPalette.pickUp(picked)
        return true
    }

    private fun clearCell(sx: Int, sy: Int): Boolean {
        val grid = displayedGridWidget ?: return false
        val slot = grid.layout.getSlot(sx, sy) ?: return false
        if (grid.layout.plantCovering(sx, sy) == null && slot.soil == null) return false

        saveUndoSnapshot(grid.layout)
        removePlantCovering(grid, sx, sy)
        slot.soil = null
        grid.init()
        return true
    }

    private fun removePlantCovering(grid: GridWidget, sx: Int, sy: Int) {
        val covering = grid.layout.plantCovering(sx, sy) ?: return
        grid.noteVanishing(covering)
        grid.layout.plants.remove(covering)
    }

    private fun markCell(sx: Int, sy: Int, marking: LayoutSlot.Marking?): Boolean {
        val grid = displayedGridWidget ?: return false
        val plant = grid.layout.plantCovering(sx, sy) ?: return false

        applyMark(plant, marking)
        return true
    }

    private fun paintUnderMouse(event: MouseButtonEvent): Boolean {
        val grid = displayedGridWidget ?: return false
        val cell = grid.slotAt(event.x, event.y) ?: return false
        if (cell == lastPaintedCell) return true

        val picked = plantPalette.selected
        val acted = when {
            picked != null -> { placePaletteItem(picked, event.x, event.y); true }
            plantPalette.deleteMode -> { clearCell(cell.first, cell.second); true }
            plantPalette.markChoice.applies -> { markCell(cell.first, cell.second, plantPalette.markChoice.marking); true }
            else -> false
        }
        if (acted) lastPaintedCell = cell

        return acted
    }

    private fun placePaletteItem(item: PaletteItem, mouseX: Double, mouseY: Double) {
        if (currentDisplay != DisplayMode.Presets) return
        if (displayedGridWidget == null) {
            if (emptyGridWidget?.slotAt(mouseX, mouseY) == null) return
            isPresetCleared = false
            addPresetLayout(GreenhouseLayout.create(GreenhouseData.computeNextAvailableId()))
        }
        val grid = displayedGridWidget ?: return
        val (sx, sy) = grid.slotAt(mouseX, mouseY) ?: return
        val slot = grid.layout.getSlot(sx, sy) ?: return

        if (item is PaletteItem.Soil) {
            saveUndoSnapshot(grid.layout)
            removePlantCovering(grid, sx, sy)
            slot.soil = item.block
            grid.init()
            return
        }
        val def = (item as PaletteItem.Crop).def

        val standing = grid.layout.plantCovering(sx, sy)
        if (plantPalette.mergeMode && standing != null) {
            mergeInto(grid, standing, def)
            return
        }

        if (!fitsInsideGrid(grid.layout, def, sx, sy)) return

        saveUndoSnapshot(grid.layout)
        grid.layout.plants.removeAll(plantsOverlapping(grid.layout, def, sx, sy))

        def.requiredSoil.firstOrNull()?.let { soil ->
            def.footprint.cellsFrom(sx, sy).forEach { (cellX, cellY) -> grid.layout.getSlot(cellX, cellY)?.soil = soil }
        }

        val instance = Plant(def.elementId, slot, null, null, cropDef = def)
        grid.layout.plants.add(instance)
        grid.justPlaced.add(instance)
        grid.init()
    }

    private fun mergeInto(grid: GridWidget, standing: Plant, def: CropDefinition) {
        if (standing.cropDef.footprint != def.footprint) {
            ChatUtils.sendWithPrefix("Only crops of the same size can share a slot.")
            return
        }
        if (standing.acceptsCrop(def)) return

        saveUndoSnapshot(grid.layout)
        val merged = standing.copyForPrediction(standing.slot).also { it.presetAlternatives.add(def) }
        grid.layout.plants.remove(standing)
        grid.layout.plants.add(merged)
        merged.slot.mark = LayoutSlot.Marking.Target
        grid.justMarked.add(merged)
        grid.init()
    }

    private fun openMarkContext(instance: Plant, event: MouseButtonEvent) {
        val grid = displayedGridWidget ?: return

        val options = if (instance.hasAlternatives) listOf(MarkOption.Target, MarkOption.None) else MarkOption.entries
        val menu = PickContext(event.x.toInt(), event.y.toInt(), "Mark as:", options, this) { option ->
            applyMark(instance, option.marking)
        }
        menu.init()
        addContext(menu)
    }

    private fun applyMark(instance: Plant, marking: LayoutSlot.Marking?) {
        val grid = displayedGridWidget ?: return
        if (instance.hasAlternatives && marking == LayoutSlot.Marking.Ingredient) return

        saveUndoSnapshot(grid.layout)
        if (instance.hasAlternatives && marking == null) {
            val single = instance.copyForPrediction(instance.slot).also { it.presetAlternatives.clear() }
            grid.layout.plants.remove(instance)
            grid.layout.plants.add(single)
            single.slot.mark = null
            grid.justMarked.add(single)
            grid.init()
            return
        }

        instance.slot.mark = marking
        grid.justMarked.add(instance)
        grid.init()
    }

    private fun uniqueMissingFromPreset(def: CropDefinition): Boolean {
        if (!def.isBaseCrop) return false

        val layout = displayedGridWidget?.layout ?: return false
        val plots = GreenhouseData.greenhouseLayoutFor(layout)?.plots ?: listOf(layout)
        val key = GreenhouseData.UniqueCropKey.from(def)

        return plots.flatMap { it.plants }.none {
            it.cropDef.isBaseCrop && GreenhouseData.UniqueCropKey.from(it.cropDef) == key
        }
    }

    private sealed interface HistoryStep {
        class PlotContents(
            val layout: PlotLayout,
            val plants: List<Plant>,
            val slots: List<Triple<LayoutSlot, Block?, LayoutSlot.Marking?>>
        ) : HistoryStep

        class PresetRemoved(
            val preset: GreenhouseLayout,
            val index: Int,
            val presetList: List<GreenhouseLayout>
        ) : HistoryStep

        class PlotRemoved(val preset: GreenhouseLayout, val plot: PlotLayout, val index: Int) : HistoryStep
    }

    private fun snapshot(layout: PlotLayout) = HistoryStep.PlotContents(
        layout,
        layout.plants.toList(),
        layout.slots.map { Triple(it, it.soil, it.mark) }
    )

    private fun saveUndoSnapshot(layout: PlotLayout) = saveHistoryStep(snapshot(layout))

    private fun saveHistoryStep(step: HistoryStep) {
        undoStack.addLast(step)
        if (undoStack.size > HISTORY_LIMIT) undoStack.removeFirst()
        redoStack.clear()
    }

    private fun isStepLive(step: HistoryStep): Boolean = when (step) {
        is HistoryStep.PlotContents -> GreenhouseData.greenhouseLayoutFor(step.layout) != null
        is HistoryStep.PresetRemoved -> step.presetList === GreenhouseData.presetGrids
        is HistoryStep.PlotRemoved -> GreenhouseData.presetGrids.any { it === step.preset }
    }

    private fun takeLiveStep(stack: ArrayDeque<HistoryStep>): HistoryStep? {
        while (true) {
            val step = stack.removeLastOrNull() ?: return null
            if (isStepLive(step)) return step
        }
    }

    private fun revertStep(step: HistoryStep): HistoryStep = when (step) {
        is HistoryStep.PlotContents -> restoreContents(step)
        is HistoryStep.PresetRemoved -> step.also { reinsertPreset(it) }
        is HistoryStep.PlotRemoved -> step.also { reinsertPlot(it) }
    }

    private fun reapplyStep(step: HistoryStep): HistoryStep = when (step) {
        is HistoryStep.PlotContents -> restoreContents(step)
        is HistoryStep.PresetRemoved -> removePreset(step.preset)
        is HistoryStep.PlotRemoved -> removePlot(step.preset, step.plot)
    }

    private fun restoreContents(saved: HistoryStep.PlotContents): HistoryStep.PlotContents {
        val current = snapshot(saved.layout)
        saved.layout.plants.clear()
        saved.layout.plants.addAll(saved.plants)
        saved.slots.forEach { (slot, block, mark) ->
            slot.soil = block
            slot.mark = mark
        }
        if (presetGridWidgets.none { it.layout === saved.layout }) {
            GreenhouseData.greenhouseLayoutFor(saved.layout)?.let { parentPreset ->
                GreenhouseData.currentPreset = parentPreset
                shownPlot = saved.layout
                isPresetCleared = false
            }
        } else if (displayedGridWidget?.layout !== saved.layout) {
            shownPlot = saved.layout
        }
        return current
    }

    private fun reinsertPreset(step: HistoryStep.PresetRemoved) {
        val presets = GreenhouseData.presetGrids
        presets.add(step.index.coerceAtMost(presets.size), step.preset)
        GreenhouseData.currentPreset = step.preset
        shownPlot = null
        isPresetCleared = false
    }

    private fun reinsertPlot(step: HistoryStep.PlotRemoved) {
        step.preset.plots.add(step.index.coerceAtMost(step.preset.plots.size), step.plot)
        GreenhouseData.currentPreset = step.preset
        shownPlot = step.plot
        isPresetCleared = false
    }

    private fun undo() {
        val step = takeLiveStep(undoStack) ?: return
        redoStack.addLast(revertStep(step))
        initPresetLayout()
    }

    private fun redo() {
        val step = takeLiveStep(redoStack) ?: return
        undoStack.addLast(reapplyStep(step))
        initPresetLayout()
    }

    private fun askClearPlot(event: MouseButtonEvent) {
        val grid = displayedGridWidget ?: return
        val question = "Clear ${GreenhouseData.fullPlotName(grid.layout)}?"
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(event.x.toInt(), event.y.toInt(), ConfirmContext.widthFor(question), ConfirmContext.HEIGHT)
        addContext(ConfirmContext(menuX, menuY, question, this) {
            saveUndoSnapshot(grid.layout)
            grid.layout.plants.clear()
            grid.layout.slots.forEach {
                it.soil = null
                it.mark = null
            }
            stopPlannersOn(grid.layout)
            grid.init()
        })
    }

    private fun askExport(event: MouseButtonEvent) {
        val grid = displayedGrid() ?: return
        val menu = PickContext(event.x.toInt(), event.y.toInt(), "Format:", LayoutFormatType.entries, this) { type ->
            val result = type.format.export(presetCopyOf(grid.layout))

            result.notes.forEach { ChatUtils.sendWithPrefix(it) }

            when (result) {
                is LayoutTransferResult.Failure -> ChatUtils.sendWithPrefix(result.reason)
                is LayoutTransferResult.Exported -> {
                    Minecraft.getInstance().keyboardHandler.clipboard = result.text
                    ChatUtils.sendWithPrefix(
                        "Copied a ${type.format.displayName} layout for ${grid.layout.displayName()} to your clipboard"
                    )
                }
                is LayoutTransferResult.Imported -> Unit
            }
        }
        menu.init()
        addContext(menu)
    }

    private fun presetCopyOf(layout: PlotLayout): PlotLayout {
        val preset = PlotLayout(id = layout.id, name = layout.displayName(), size = layout.size)

        preset.slots.forEach { slot ->
            val greenhouseSlot = layout.getSlot(slot.x, slot.y)
            slot.soil = greenhouseSlot?.soil
            slot.mark = greenhouseSlot?.mark
        }
        layout.plants.forEach { plant ->
            val slot = preset.getSlot(plant.slot.x, plant.slot.y) ?: return@forEach
            preset.plants.add(
                Plant(plant.elementId, slot, cropDef = plant.cropDef, presetAlternatives = plant.presetAlternatives.toMutableList())
            )
        }
        return preset
    }

    private fun askRescan(event: MouseButtonEvent) {
        val grid = displayedGrid() ?: return
        val question = "Forcibly rescan ${grid.layout.displayName()}?"
        val warning = "This will cause placed mutation to possibly be detected as harvestable until " +
                "clicked with diagnostic tool, or not detected at all and possibly other problems, Proceed?"
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            event.x.toInt(),
            event.y.toInt(),
            ConfirmContext.widthFor(question, warning),
            ConfirmContext.heightFor(question, warning)
        )
        addContext(ConfirmContext(menuX, menuY, question, this, warning) {
            GreenhouseData.rescanFromScratch(grid)
            initGreenhouseLayout()
        })
    }

    private fun stopPlannersOn(plot: PlotLayout) {
        GreenhouseData.greenhouseGrids
            .filter { it.state.assignedLayout === plot }
            .forEach { GreenhouseData.unplanGreenhouse(it) }
    }

    private fun turnShownPlot(turns: Int) {
        val grid = displayedGridWidget ?: return

        saveUndoSnapshot(grid.layout)
        grid.layout.copyContentsFrom(grid.layout.turnedBy(turns))

        GreenhouseData.greenhouseGrids
            .filter { it.state.assignedLayout === grid.layout }
            .forEach { it.state.planTurns = Math.floorMod(it.state.planTurns - turns, 4) }

        grid.init()
    }

    private fun newPreset() {
        addPresetLayout(GreenhouseLayout.create(GreenhouseData.computeNextAvailableId()))
    }

    private fun warningBadgeX(): Int =
        (width + font.width(displayedName) + Common.UI.TEXT_X_PAD * 2) / 2 + Common.UI.SPACING

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.pose().pushMatrix()
        graphics.pose().scale(drawScale, drawScale)
        extractInLayoutUnits(graphics, (mouseX / drawScale).toInt(), (mouseY / drawScale).toInt(), delta)
        graphics.pose().popMatrix()
    }

    private fun gridTurns(): Int =
        if (!GreenhousePresets.turnsGridWithPlayer()) 0 else Math.floorMod(-facingTurns(), 4)

    private fun followPlayerTurn() {
        val turns = gridTurns()
        greenhouseGridWidgets
            .filter { it.turns != turns }
            .forEach {
                it.turns = turns
                it.init()
            }
    }

    private fun extractInLayoutUnits(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        followPlayerTurn()

        greenhousePanel.assigned = displayedGrid()?.let { grid ->
            grid.state.assignedLayout?.let { plan ->
                GreenhouseData.nameInFull(plan) + if (grid.state.planTurns == 0) "" else " (turned ${grid.state.planTurns * 90}°)"
            }
        }

        if (displayedGridWidget != null) {
            when (currentDisplay) {
                DisplayMode.Greenhouses -> {
                    plotTabs.render(graphics)
                    hoverControls.extractRenderState(graphics, mouseX, mouseY, delta)
                    teleportTab.render(graphics)
                }
                DisplayMode.Presets -> presetPlotTabs.render(graphics)
            }
        }

        graphics.drawPanel(
            gridLeft - BORDER_PADDING,
            gridTop - BORDER_PADDING,
            gridLeft + gridSpan + BORDER_PADDING,
            gridTop + gridSpan + BORDER_PADDING
        )

        drawNameBox(graphics)
        drawTickTimeBox(graphics, mouseX, mouseY)

        displayedGridWidget?.extractRenderState(graphics, mouseX, mouseY, delta)
        if (displayedGridWidget == null && currentDisplay == DisplayMode.Presets) {
            emptyGridWidget?.extractRenderState(graphics, mouseX, mouseY, delta)
        }

        graphics.drawShelf(shelfLeft, viewShelfY, shelfLeft + shelfWidth, viewShelfY + viewShelfHeight, SHELF_VIEW)

        if (predictShelfHeight > 0) {
            graphics.drawShelf(
                shelfLeft,
                predictShelfY,
                shelfLeft + shelfWidth,
                predictShelfY + predictShelfHeight,
                SHELF_PREDICT
            )
            predictSlider.render(graphics, mouseX, mouseY)
            graphics.text(
                font,
                Component.literal(predictLabel()),
                predictSlider.x + predictSlider.width + Common.UI.SPACING,
                predictSlider.y + (SliderWidget.HEIGHT - font.lineHeight) / 2 + 1,
                Common.UI.TEXT_COLOR,
                false
            )
            graphics.text(
                font,
                Component.literal(predictWindow()),
                predictSlider.x,
                predictSlider.y + SliderWidget.HEIGHT + Common.UI.SPACING,
                Common.UI.TEXT_COLOR,
                false
            )
            graphics.text(
                font,
                Component.literal(predictWindowClock()),
                predictSlider.x,
                predictSlider.y + SliderWidget.HEIGHT + Common.UI.SPACING + font.lineHeight,
                Common.UI.TEXT_COLOR,
                false
            )
        }

        val panel = if (currentDisplay == DisplayMode.Greenhouses) greenhousePanel else presetUI
        unplannedLineBox = null
        waterLastsLineBox = null
        if (panel.hasShown()) {
            val title = if (currentDisplay == DisplayMode.Greenhouses) SHELF_GREENHOUSE else SHELF_PRESET
            val lineTop = actionShelfY + shelfTitleHeight() + panel.contentHeight
            val hasPlan = displayedGridWidget?.targetPlan?.invoke() != null
            val bottom = lineTop + if (hasPlan) UNPLANNED_LINE_HEIGHT else 0
            graphics.drawShelf(shelfLeft, actionShelfY, shelfLeft + shelfWidth, bottom, title)
            if (hasPlan) renderUnplannedMutationsLine(graphics, lineTop, mouseX, mouseY)
        }

        val rightShelfLeft = if (currentDisplay == DisplayMode.Greenhouses) {
            hoverControls.x + hoverControls.width + Common.UI.SPACING_LARGE
        } else {
            gridLeft + gridSpan + BORDER_PADDING + Common.UI.SPACING_LARGE
        }
        val rightShelfWidth = minOf(shelfWidth, width - rightShelfLeft - Common.UI.SPACING_LARGE)
        renderContentsShelf(graphics, rightShelfLeft, gridTop - BORDER_PADDING, rightShelfWidth.coerceAtLeast(MIN_ACTION_ROW_WIDTH), mouseX, mouseY)

        when (currentDisplay) {
            DisplayMode.Greenhouses -> renderGreenhouseMode(graphics, mouseX, mouseY, delta)
            DisplayMode.Presets -> renderPresetMode(graphics, mouseX, mouseY, delta)
        }
        scrollHint.extractRenderState(graphics, mouseX, mouseY)

        plotsButton.pressed = currentDisplay == DisplayMode.Greenhouses
        presetsButton.pressed = currentDisplay == DisplayMode.Presets
        plotsButton.extractRenderState(graphics, mouseX, mouseY, delta)
        presetsButton.extractRenderState(graphics, mouseX, mouseY, delta)
        cropPreviewButton.extractRenderState(graphics, mouseX, mouseY, delta)

        if (isWarningHovered) {
            graphics.drawSimpleTooltip(
                """
                    The displayed greenhouse uses prediction based data.
                    Enter it to update its state.
                    """.trimIndent(),
                warningBadgeX(),
                NAME_TOP + boxHeight(displayedName) + Common.UI.SPACING
            )
        }

        renderOverlays(graphics, mouseX, mouseY, delta)

        when (currentDisplay) {
            DisplayMode.Greenhouses -> {
                plotTabs.renderTooltip(graphics, mouseX, mouseY)
                hoverControls.renderTooltip(graphics, mouseX, mouseY)
                teleportTab.renderTooltip(graphics, mouseX, mouseY)
            }
            DisplayMode.Presets -> {
                if (displayedGridWidget != null) presetPlotTabs.renderTooltip(graphics, mouseX, mouseY)
                plantPalette.renderDrag(graphics)
                plantPalette.renderTooltip(graphics, mouseX, mouseY)
            }
        }

        if (tickTimePinned) {
            drawPinnedTickTime(graphics, mouseX, mouseY)
        } else if (tickTimeHovered) {
            graphics.drawSimpleTooltip(tickTimeTooltip(), TICK_TIME_LEFT, TICK_TIME_CENTER_Y + boxHeight(" ") / 2 + Common.UI.SPACING)
        }

        waterLastsLineBox?.let { (lineX, lineTop, lineWidth, lineHeight) ->
            if (mouseX in lineX until lineX + lineWidth && mouseY in lineTop until lineTop + lineHeight) {
                graphics.drawTooltipAtCursor(WATER_LASTS_TOOLTIP, mouseX, mouseY)
                return
            }
        }

        displayedGridWidget?.unplannedTooltipAt(mouseX.toDouble(), mouseY.toDouble())?.let { lines ->
            graphics.drawTooltipLinesAtCursor(lines.map { it.visualOrderText }, mouseX, mouseY)
            return
        }

        val hovered = hoveredElement as? ElementWidget ?: return

        (hovered.deadTooltipAt(mouseX, mouseY)
            ?: hovered.debtTooltipAt(mouseX, mouseY)
            ?: hovered.chargeTooltipAt(mouseX, mouseY))?.let {
            graphics.drawTooltipAtCursor(it, mouseX, mouseY)
            return
        }
        hovered.renderTooltip(graphics, contentsShelfLeft, contentsShelfBottom + Common.UI.SPACING)
    }

    private var unplannedLineBox: IntArray? = null
    private var waterLastsLineBox: IntArray? = null

    private var unplannedMutationsPinned: Boolean = false

    private fun renderUnplannedMutationsLine(graphics: GuiGraphicsExtractor, lineTop: Int, mouseX: Int, mouseY: Int) {
        val grid = displayedGridWidget ?: return
        val count = grid.unplannedMutationSpots().size
        val lineX = shelfLeft + ActionPanel.PADDING
        val lineWidth = shelfWidth - ActionPanel.PADDING * 2

        val (text, color) = when (count) {
            0 -> UNPLANNED_NONE to Common.UI.TEXT_DIM_COLOR
            1 -> "1 cell can grow unplanned" to GridWidget.UNPLANNED_MARK_COLOR
            else -> "$count cells can grow unplanned" to GridWidget.UNPLANNED_MARK_COLOR
        }
        val hovered = count > 0 && inRect(mouseX, mouseY, lineX, lineTop, lineWidth, UNPLANNED_LINE_HEIGHT)
        if (count == 0) unplannedMutationsPinned = false
        grid.showUnplannedMutations = hovered || unplannedMutationsPinned

        if (hovered || unplannedMutationsPinned) graphics.fill(lineX, lineTop, lineX + lineWidth, lineTop + UNPLANNED_LINE_HEIGHT, Common.UI.HOVER_WASH)
        graphics.modText(font, Component.literal(text), lineX, lineTop + (UNPLANNED_LINE_HEIGHT - font.lineHeight) / 2 + 1, color)
        unplannedLineBox = intArrayOf(lineX, lineTop, lineWidth, UNPLANNED_LINE_HEIGHT)
    }

    private fun toggleUnplannedPinOnClick(event: MouseButtonEvent): Boolean {
        val line = unplannedLineBox ?: return false
        if (event.button() != 0 || !inRect(event.x, event.y, line[0], line[1], line[2], line[3])) return false
        if (displayedGridWidget?.unplannedMutationSpots().isNullOrEmpty()) return false

        unplannedMutationsPinned = !unplannedMutationsPinned
        return true
    }

    private class CropCountRow(val def: CropDefinition, val x: Int, val y: Int, val width: Int)

    private var cropCountRows: List<CropCountRow> = emptyList()

    private var contentsTabTitleBoxes: Map<ContentsTab, IntArray> = emptyMap()

    private var contentsShelfBottom: Int = 0
    private var contentsShelfLeft: Int = 0

    private val awayTicksSlider = SliderWidget { awayTicks = it }

    private fun renderContentsShelf(graphics: GuiGraphicsExtractor, left: Int, top: Int, width: Int, mouseX: Int, mouseY: Int) {
        cropCountRows = emptyList()
        contentsTabTitleBoxes = emptyMap()
        contentsShelfBottom = top
        contentsShelfLeft = left
        val layout = displayedGridWidget?.layout ?: return

        val rowsTop = top + shelfTitleHeight()

        val shelfBottom = when (contentsTab) {
            ContentsTab.Contents -> renderCropCountRows(graphics, layout, left, rowsTop, width, mouseX, mouseY, draw = false) ?: return
            ContentsTab.Report -> renderLayoutReport(graphics, left, rowsTop, width, mouseX, mouseY, draw = false)
        }

        graphics.drawShelf(left, top, left + width, shelfBottom, "")
        renderContentsShelfTabs(graphics, left, top, mouseX, mouseY)

        when (contentsTab) {
            ContentsTab.Contents -> renderCropCountRows(graphics, layout, left, rowsTop, width, mouseX, mouseY, draw = true)
            ContentsTab.Report -> renderLayoutReport(graphics, left, rowsTop, width, mouseX, mouseY, draw = true)
        }
        contentsShelfBottom = shelfBottom
    }

    private fun renderContentsShelfTabs(graphics: GuiGraphicsExtractor, left: Int, top: Int, mouseX: Int, mouseY: Int) {
        var tabX = left + Common.UI.TEXT_X_PAD
        val tabTitleBoxes = mutableMapOf<ContentsTab, IntArray>()

        ContentsTab.shown.forEachIndexed { index, tab ->
            if (index > 0) {
                graphics.modText(font, Component.literal(" | "), tabX, top + Common.UI.SPACING, Common.UI.TEXT_DIM_COLOR)
                tabX += font.width(" | ")
            }
            val box = intArrayOf(tabX, top, font.width(tab.label), shelfTitleHeight())
            val hovered = inRect(mouseX, mouseY, box[0], box[1], box[2], box[3])
            val color = if (tab == contentsTab || hovered) Common.UI.TEXT_COLOR else Common.UI.TEXT_DIM_COLOR
            graphics.modText(font, Component.literal(tab.label), tabX, top + Common.UI.SPACING, color)
            tabTitleBoxes[tab] = box
            tabX += box[2]
        }
        contentsTabTitleBoxes = tabTitleBoxes
    }

    private fun renderCropCountRows(
        graphics: GuiGraphicsExtractor,
        layout: PlotLayout,
        left: Int,
        rowsTop: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        draw: Boolean
    ): Int? {
        val plantCountsByCrop = layout.plants
            .groupingBy { it.cropDef }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<CropDefinition, Int>> { it.value }.thenBy { it.key.name })
        if (plantCountsByCrop.isEmpty()) return null

        val withChecklist = currentDisplay == DisplayMode.Greenhouses
        val frameBottom = gridTop + gridSpan + BORDER_PADDING
        val rowsThatFit = ((frameBottom - rowsTop - ActionPanel.PADDING) / CONTENTS_ROW_HEIGHT).coerceAtLeast(1)
        val columns = if (plantCountsByCrop.size > rowsThatFit) 2 else 1
        val rowsPerColumn = (plantCountsByCrop.size + columns - 1) / columns
        val columnWidth = (width - ActionPanel.PADDING * 2 - Common.UI.SPACING * (columns - 1)) / columns

        val rowsBottom = rowsTop + rowsPerColumn * CONTENTS_ROW_HEIGHT + ActionPanel.PADDING
        if (!draw) return rowsBottom

        val cropIdsWithoutPinnedInfo = GreenhouseData.miscInfo.cropsWithoutInfo
        cropCountRows = plantCountsByCrop.mapIndexed { index, (def, count) ->
            val rowX = left + ActionPanel.PADDING + (index / rowsPerColumn) * (columnWidth + Common.UI.SPACING)
            val rowY = rowsTop + (index % rowsPerColumn) * CONTENTS_ROW_HEIGHT
            val pinnedInfoShown = !withChecklist || def.elementId !in cropIdsWithoutPinnedInfo

            if (withChecklist && inRect(mouseX, mouseY, rowX, rowY, columnWidth, CONTENTS_ROW_HEIGHT)) {
                graphics.fill(rowX, rowY, rowX + columnWidth, rowY + CONTENTS_ROW_HEIGHT, Common.UI.HOVER_WASH)
            }

            graphics.renderFakeItem(itemStackFor(def), rowX + 1, rowY + 1, CONTENTS_ICON_SIZE, CONTENTS_ICON_SIZE)

            var textRight = rowX + columnWidth
            if (withChecklist) {
                contentsCheckbox.checked = pinnedInfoShown
                contentsCheckbox.x = rowX + columnWidth - CONTENTS_CHECKBOX_SIZE - 1
                contentsCheckbox.y = rowY + (CONTENTS_ROW_HEIGHT - CONTENTS_CHECKBOX_SIZE) / 2
                contentsCheckbox.render(graphics)
                textRight = contentsCheckbox.x
            }

            val textX = rowX + CONTENTS_ICON_SIZE + Common.UI.SPACING
            val countText = " x$count"
            val nameWidthLeft = textRight - Common.UI.SPACING - textX - font.width(countText)
            val nameColor = if (pinnedInfoShown) markColorFor(layout, def) else Common.UI.DISABLED_TEXT_COLOR
            val label = Component.literal(font.plainSubstrByWidth(def.name, nameWidthLeft.coerceAtLeast(0))).withColor(rgb(nameColor))
                .append(Component.literal(countText).withStyle(ChatFormatting.GRAY))
            graphics.modText(font, label, textX, rowY + (CONTENTS_ROW_HEIGHT - font.lineHeight) / 2 + 1, Common.UI.TEXT_COLOR)

            CropCountRow(def, rowX, rowY, columnWidth)
        }.takeIf { withChecklist }.orEmpty()

        return rowsBottom
    }

    private fun targetPlanOfDisplayedGrid(): PlotLayout? = displayedGridWidget?.targetPlan?.invoke()

    private fun renderLayoutReport(graphics: GuiGraphicsExtractor, left: Int, rowsTop: Int, width: Int, mouseX: Int, mouseY: Int, draw: Boolean): Int {
        val innerLeft = left + ActionPanel.PADDING
        val innerWidth = width - ActionPanel.PADDING * 2
        var lineY = rowsTop
        val tickMs = GreenhouseTickTime.tickMs

        fun text(value: Component, textX: Int, color: Int) {
            if (draw) graphics.modText(font, value, textX, lineY, color)
        }

        fun line(label: String, value: Component) {
            text(Component.literal(label), innerLeft, Common.UI.TEXT_DIM_COLOR)
            text(value, innerLeft + innerWidth - font.width(value), Common.UI.TEXT_COLOR)
            lineY += font.lineHeight + Common.UI.SPACING_SMALL
        }

        val awayTime = tickMs?.let { (awayTicks * it).toShortDuration() } ?: "tick time unknown"
        text(Component.literal("Away $awayTicks ticks, $awayTime"), innerLeft, Common.UI.TEXT_COLOR)
        lineY += font.lineHeight + Common.UI.SPACING

        awayTicksSlider.x = innerLeft
        awayTicksSlider.y = lineY
        awayTicksSlider.width = innerWidth
        awayTicksSlider.range(1, MAX_AWAY_TICKS)
        awayTicksSlider.show(awayTicks)
        if (draw) awayTicksSlider.render(graphics, mouseX, mouseY)
        lineY += SliderWidget.HEIGHT + Common.UI.SPACING

        val plan = targetPlanOfDisplayedGrid()
        val unavailableReason = when {
            plan == null -> "No plan to report on"
            tickMs == null -> "Needs your tick time"
            else -> null
        }
        if (unavailableReason != null || plan == null || tickMs == null) {
            text(Component.literal(unavailableReason ?: ""), innerLeft, Common.UI.TEXT_DIM_COLOR)
            return lineY + font.lineHeight + ActionPanel.PADDING
        }

        val report = layoutReportFor(plan, tickMs)
        val result = report?.result
        val targets = result?.targetSpotsByCrop?.entries?.joinToString(", ") { "${it.key} x${it.value}" }
        val noTargets = plan.plants.none { it.slot.mark == LayoutSlot.Marking.Target }

        line("Targets", Component.literal(if (noTargets) "none marked" else targets ?: "..."))
        line("Per visit", Component.literal(result?.let { "%.1f harvested".format(it.harvestedPerVisit) } ?: "..."))
        line("Per 50 ticks (${(50 * tickMs).toShortDuration()})", Component.literal(result?.let { "%.0f harvested".format(it.harvestedPerFiftyTicks) } ?: "..."))

        val ticksUntilDry = result?.ticksUntilFirstPlantDry
        val waterLasts = when {
            result == null -> Component.literal("...")
            ticksUntilDry == null -> Component.literal("forever")
            else -> Component.literal("$ticksUntilDry ticks").withColor(rgb(if (ticksUntilDry < awayTicks) Common.UI.DANGER_COLOR else Common.UI.TEXT_COLOR))
        }
        if (draw) waterLastsLineBox = intArrayOf(innerLeft, lineY, innerWidth, font.lineHeight)
        line("Water lasts", waterLasts)

        val unplanned = displayedGridWidget?.unplannedMutationSpots()?.size ?: 0
        line("Unplanned", if (unplanned == 0) Component.literal("none") else Component.literal("$unplanned cells").withColor(rgb(GridWidget.UNPLANNED_MARK_COLOR)))

        val blocked = result?.blockedTargetCount ?: 0
        line("Blocked", if (blocked == 0) Component.literal("none") else Component.literal("$blocked targets").withColor(rgb(Common.UI.DANGER_COLOR)))

        return lineY + ActionPanel.PADDING - Common.UI.SPACING_SMALL
    }

    private class RunningLayoutReport(val key: Int, val future: CompletableFuture<PlotPrediction.Result>) {
        val result: PlotPrediction.Result? get() = future.getNow(null)
    }

    private var runningLayoutReport: RunningLayoutReport? = null

    private fun layoutReportFor(plan: PlotLayout, tickMs: Long): RunningLayoutReport? {
        val multiplier = BioanalysisAccessory.mutationWeightMultiplier()
        var reportInputsHash = awayTicks * 31 + multiplier.hashCode() + tickMs.hashCode() * 17
        plan.slots.forEach { reportInputsHash = reportInputsHash * 31 + (it.soil?.hashCode() ?: 0) + (it.mark?.ordinal ?: -1) }
        plan.plants.forEach { reportInputsHash = reportInputsHash * 31 + (it.slot.x * 64 + it.slot.y) * 31 + it.cropDef.name.hashCode() }

        runningLayoutReport?.takeIf { it.key == reportInputsHash }?.let { return it }

        runningLayoutReport?.future?.cancel(true)
        val planCopy = plan.freshCopy()
        val awayTicksAtStart = awayTicks
        val started = RunningLayoutReport(reportInputsHash, CompletableFuture.supplyAsync { PlotPrediction.simulateVisits(planCopy, awayTicksAtStart, multiplier) })
        runningLayoutReport = started
        return started
    }

    private fun selectContentsTabOnClick(event: MouseButtonEvent): Boolean {
        if (event.button() != 0) return false
        val tab = contentsTabTitleBoxes.entries.firstOrNull { (_, box) -> inRect(event.x, event.y, box[0], box[1], box[2], box[3]) }?.key ?: return false
        contentsTab = tab
        return true
    }

    private val contentsCheckbox = CheckboxWidget(CONTENTS_CHECKBOX_SIZE)

    private fun togglePinnedInfoOnClick(event: MouseButtonEvent): Boolean {
        if (event.button() != 0) return false
        val row = cropCountRows.firstOrNull { inRect(event.x, event.y, it.x, it.y, it.width, CONTENTS_ROW_HEIGHT) } ?: return false

        val cropIdsWithoutPinnedInfo = GreenhouseData.miscInfo.cropsWithoutInfo
        if (!cropIdsWithoutPinnedInfo.remove(row.def.elementId)) cropIdsWithoutPinnedInfo.add(row.def.elementId)
        return true
    }

    private fun markColorFor(layout: PlotLayout, def: CropDefinition): Int {
        val marks = layout.plants.filter { it.cropDef == def }.mapNotNull { it.slot.mark }

        return when {
            LayoutSlot.Marking.Target in marks -> LayoutSlot.Marking.Target.color
            LayoutSlot.Marking.Ingredient in marks -> LayoutSlot.Marking.Ingredient.color
            else -> Common.UI.TEXT_COLOR
        }
    }

    private fun rgb(color: Int): Int = color and 0xFFFFFF

    private fun drawTickTimeBox(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val tickTimeText = "Next tick: " + (GreenhouseData.miscInfo.nextTickTime?.toReadableDuration() ?: "unknown")
        val tickTimeBoxWidth = font.width(tickTimeText) + Common.UI.TEXT_X_PAD * 2
        val tickTimeBoxHeight = boxHeight(tickTimeText)

        tickTimeBox = intArrayOf(TICK_TIME_LEFT, TICK_TIME_CENTER_Y - tickTimeBoxHeight / 2, TICK_TIME_LEFT + tickTimeBoxWidth, TICK_TIME_CENTER_Y + tickTimeBoxHeight / 2)
        tickTimeHovered = isOverTickTimeBox(mouseX.toDouble(), mouseY.toDouble())

        graphics.drawButtonPanel(tickTimeBox[0], tickTimeBox[1], tickTimeBox[2], tickTimeBox[3], tickTimeHovered, pressed = tickTimePinned)
        graphics.modText(font, Component.literal(tickTimeText), TICK_TIME_LEFT + Common.UI.TEXT_X_PAD, TICK_TIME_CENTER_Y - font.lineHeight / 2, Common.UI.TEXT_COLOR)
    }

    private fun isOverTickTimeBox(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, tickTimeBox[0], tickTimeBox[1], tickTimeBox[2] - tickTimeBox[0], tickTimeBox[3] - tickTimeBox[1])

    private fun renderGreenhouseMode(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        displayedGridWidget?.pinnedInfo = HoverControls.selectedInfo
        displayedGridWidget?.cropIdsWithoutPinnedInfo = GreenhouseData.miscInfo.cropsWithoutInfo

        scrollHint.tooltip = SCROLL_HINT_GREENHOUSES
        scrollHint.layoutAt(
            gridLeft + gridSpan + BORDER_PADDING - ScrollHint.SIZE,
            gridTop - BORDER_PADDING - Bookmarks.THICKNESS,
            Bookmarks.THICKNESS
        )

        greenhousePanel.showButtons = currentDisplay == DisplayMode.Greenhouses && displayedGrid() != null
        greenhousePanel.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    private fun renderPresetMode(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        presetSelector.extractRenderState(graphics, mouseX, mouseY, delta)
        plantPalette.render(graphics, mouseX, mouseY, delta)
        renderDropTarget(graphics)

        scrollHint.tooltip = SCROLL_HINT_PRESETS
        scrollHint.layoutBeside(presetSelector.x + presetSelector.width, presetSelector.y, presetSelector.height)

        presetUI.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    private fun inLayoutUnits(event: MouseButtonEvent): MouseButtonEvent = event.at(event.x / drawScale, event.y / drawScale)

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val layoutEvent = inLayoutUnits(event)

        if (overlaysMouseClicked(layoutEvent, doubled)) return true

        if (currentDisplay == DisplayMode.Presets && presetSelector.mouseClicked(layoutEvent, doubled)) {
            return true
        }

        closeOverlays()

        if (layoutEvent.button() == 0 && isOverTickTimeBox(layoutEvent.x, layoutEvent.y)) {
            tickTimePinned = !tickTimePinned
            return true
        }

        if (tickTimePinned && isOverTickTimeLine(layoutEvent.x.toInt(), layoutEvent.y.toInt(), ATTRIBUTE_LINE)) {
            ScreenUtil.openChatThenReturn("${MainInternal.COMMAND} ${SetTimestalkAttribute.NAME} ", this)
            return true
        }

        if (toggleUnplannedPinOnClick(layoutEvent)) return true
        if (selectContentsTabOnClick(layoutEvent)) return true
        if (contentsTab == ContentsTab.Report && awayTicksSlider.mouseClicked(layoutEvent.x, layoutEvent.y)) return true

        val handled = when (currentDisplay) {
            DisplayMode.Greenhouses -> handleGreenhouseClick(layoutEvent, doubled)
            DisplayMode.Presets -> handlePresetClick(layoutEvent, doubled)
        }
        if (handled) return true

        if (layoutEvent.button() == 1 && isOverNameBox(layoutEvent.x, layoutEvent.y)) {
            when (currentDisplay) {
                DisplayMode.Presets -> GreenhouseData.currentPreset?.let { shownPreset ->
                    openRenameContext(layoutEvent, shownPreset.displayName()) { name -> shownPreset.name = name }
                }
                DisplayMode.Greenhouses -> displayedGridWidget?.layout?.let { layout ->
                    openRenameContext(layoutEvent, layout.displayName()) { name -> layout.name = name }
                }
            }
            return true
        }

        if (predictShelfHeight > 0 &&
            predictSlider.mouseClicked(layoutEvent.x, layoutEvent.y)
        ) return true

        if (cropPreviewButton.mouseClicked(layoutEvent, doubled)) {
            ScreenUtil.setScreen(CropPreviewScreen(this))
            return true
        }

        if (plotsButton.mouseClicked(layoutEvent, doubled)) {
            showDisplay(DisplayMode.Greenhouses)
            return true
        }
        if (presetsButton.mouseClicked(layoutEvent, doubled)) {
            showDisplay(DisplayMode.Presets)
            return true
        }

        if (displayedGridWidget?.mouseClicked(layoutEvent, doubled) == true) {
            return true
        }
        return super.onMouseClicked(layoutEvent, doubled)
    }

    private fun highlightPlant(instance: Plant): Boolean {
        val grid = GreenhouseData.getCurrentGrid() ?: return false
        if (grid !== displayedGrid()) return false

        val scannedPlant = grid.scannedPlants.firstOrNull { it.plant === instance } ?: return false

        if (PlantHighlight.showPlant(scannedPlant)) onClose()
        return true
    }

    private fun handleGreenhouseClick(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == 0) {
            val clicked = displayedGridWidget?.elementAtPos(event.x, event.y)
            if (clicked != null && highlightPlant(clicked)) return true
        }

        if (greenhousePanel.mouseClicked(event, doubled)) return true
        if (togglePinnedInfoOnClick(event)) return true

        if (displayedGridWidget != null) {
            if (plotTabs.mouseClicked(event)) return true
            if (teleportTab.mouseClicked(event)) return true
        }

        return hoverControls.mouseClicked(event)
    }

    private fun handlePresetClick(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (displayedGridWidget != null && presetPlotTabs.mouseClicked(event)) return true

        if (event.button() == 1 && plantPalette.holdsTool) {
            plantPalette.clearTools()
            return true
        }

        if (event.button() == 2 && pickFromCell(event)) return true

        if (plantPalette.mouseClicked(event, doubled)) return true

        plantPalette.selected?.let { picked ->
            val cell = (displayedGridWidget ?: emptyGridWidget)?.slotAt(event.x, event.y)
            if (event.button() == 0 && cell != null) {
                placePaletteItem(picked, event.x, event.y)
                lastPaintedCell = cell
                return true
            }
        }

        if (event.button() == 0) {
            displayedGridWidget?.slotAt(event.x, event.y)?.let { (sx, sy) ->
                if (plantPalette.deleteMode) {
                    lastPaintedCell = sx to sy
                    return clearCell(sx, sy)
                }
                val choice = plantPalette.markChoice
                if (choice.applies) {
                    lastPaintedCell = sx to sy
                    return markCell(sx, sy, choice.marking)
                }
            }
        }

        if (event.button() == 1) {
            (displayedGridWidget?.hoveredElement as? ElementWidget)?.let {
                openMarkContext(it.instance, event)
                return true
            }
        }

        return presetUI.mouseClicked(event, doubled)
    }

    override fun onMouseMoved(mouseX: Double, mouseY: Double) {
        val layoutX = mouseX / drawScale
        val layoutY = mouseY / drawScale

        overlaysMouseMoved(layoutX, layoutY)
        hoveredElement = overlays.firstNotNullOfOrNull { overlay ->
            overlay.hoveredElement ?: overlay.takeIf { it.isMouseOver(layoutX, layoutY) }
        }

        val underOverlay = hoveredElement != null
        val widgetX = if (underOverlay) OFF_SCREEN else layoutX
        val widgetY = if (underOverlay) OFF_SCREEN else layoutY

        when (currentDisplay) {
            DisplayMode.Greenhouses -> {
                hoverControls.mouseMoved(widgetX, widgetY)
                plotTabs.mouseMoved(widgetX, widgetY)
                teleportTab.mouseMoved(widgetX, widgetY)
                greenhousePanel.mouseMoved(widgetX, widgetY)
            }
            DisplayMode.Presets -> {
                plantPalette.mouseMoved(widgetX, widgetY)
                presetPlotTabs.mouseMoved(widgetX, widgetY)
                presetUI.mouseMoved(widgetX, widgetY)
            }
        }

        displayedGridWidget?.mouseMoved(widgetX, widgetY)
        plotsButton.mouseMoved(widgetX, widgetY)
        presetsButton.mouseMoved(widgetX, widgetY)
        cropPreviewButton.mouseMoved(widgetX, widgetY)

        hoveredElement = hoveredElement
            ?: displayedGridWidget?.hoveredElement
            ?: plotsButton.takeIf { it.isMouseOver(layoutX, layoutY) }
            ?: presetsButton.takeIf { it.isMouseOver(layoutX, layoutY) }
            ?: presetUI.hoveredElement.takeIf { currentDisplay == DisplayMode.Presets }

        isWarningHovered = isOverNameBox(layoutX, layoutY) && isShowingGuessedGrowth
    }

    override fun onCharTyped(event: CharacterEvent): Boolean {
        if (overlaysCharTyped(event)) return true
        if (currentDisplay == DisplayMode.Presets && plantPalette.charTyped(event)) return true
        return super.onCharTyped(event)
    }

    override fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (predictSlider.mouseDragged(event.x / drawScale)) return true
        if (awayTicksSlider.mouseDragged(event.x / drawScale)) return true
        if (currentDisplay == DisplayMode.Presets) {
            if (plantPalette.mouseDragged(inLayoutUnits(event), dragX, dragY)) return true
            if (event.button() == 0 && paintUnderMouse(inLayoutUnits(event))) return true
        }
        return super.onMouseDragged(event, dragX, dragY)
    }

    override fun onMouseReleased(event: MouseButtonEvent): Boolean {
        lastPaintedCell = null
        predictSlider.mouseReleased()
        awayTicksSlider.mouseReleased()
        plantPalette.mouseReleased()?.let { placePaletteItem(it, event.x / drawScale, event.y / drawScale) }
        return super.onMouseReleased(event)
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val layoutX = mouseX / drawScale
        val layoutY = mouseY / drawScale

        if (overlaysMouseScrolled(layoutX, layoutY, scrollX, scrollY)) return true

        if (scrollY == 0.0) return super.onMouseScrolled(layoutX, layoutY, scrollX, scrollY)
        if (currentDisplay == DisplayMode.Presets && plantPalette.mouseScrolled(layoutX, layoutY, scrollX, scrollY)) return true

        when (currentDisplay) {
            DisplayMode.Greenhouses -> hoverControls.cycle(down = scrollY < 0)
            DisplayMode.Presets -> cycleDisplayedGrid(forward = scrollY < 0)
        }

        return true
    }

    private fun cycleDisplayedGrid(forward: Boolean) {
        val step = if (forward) 1 else -1
        when (currentDisplay) {
            DisplayMode.Greenhouses -> {
                if (greenhouseGridWidgets.isEmpty()) return
                val index = greenhouseGridWidgets.indexOf(displayedGridWidget)
                showGreenhouse(greenhouseGridWidgets[Math.floorMod(index + step, greenhouseGridWidgets.size)].layout)
            }
            DisplayMode.Presets -> {
                val presets = GreenhouseData.presetGrids
                if (presets.isEmpty()) return
                val index = presets.indexOf(GreenhouseData.currentPreset)
                presetChanged(presets[Math.floorMod(index + step, presets.size)])
            }
        }
    }

    private fun presetChanged(pickedPreset: GreenhouseLayout) {
        GreenhouseData.currentPreset = pickedPreset
        shownPlot = null
        isPresetCleared = false
        initPresetLayout()
    }

    private fun applyImport(result: LayoutTransferResult.Imported, soft: Boolean) {
        val shownPreset = GreenhouseData.currentPreset
        val shown = displayedGridWidget?.layout
        val incoming = result.plots.filterNot { it.isEmpty() }.ifEmpty { result.plots.take(1) }

        if (shownPreset == null || shown == null) {
            importAsNewPreset(result)
            return
        }
        if (shownPreset.name == null) shownPreset.name = result.nameForPreset

        if (soft) {
            saveUndoSnapshot(shown)
            val preferredTurns = if (shown.isEmpty()) 0 else facingTurns()
            val placement = SoftImport.bestImportPlacement(shown, incoming.first(), preferredTurns)
            shown.copyContentsFrom(placement.mergedLayout)
            initPresetLayout()
            ChatUtils.sendWithPrefix("Imported into ${GreenhouseData.fullPlotName(shown)}")
            ChatUtils.sendWithPrefix(
                "Soft imported at ${placement.turns * 90}°, ${counted(placement.plantsRemoved, "plant")} and ${counted(placement.soilsReplaced, "soil")} replaced"
            )
            val dropped = incoming.size - 1
            if (dropped > 0) {
                ChatUtils.sendWithPrefix(
                    "${counted(dropped, "further plot")} in that code ${if (dropped == 1) "was" else "were"} not imported"
                )
            }
            return
        }

        val unsetPlots = shownPreset.plots.filter { it.isEmpty() }.sortedByDescending { it === shown }
        val freePlotCount = unsetPlots.size + (GreenhouseLayout.MAX_PLOTS - shownPreset.plots.size)

        when {
            incoming.size <= freePlotCount -> {
                val targets = incoming.indices.map { index -> unsetPlots.getOrNull(index) ?: shownPreset.addPlot() }
                incoming.zip(targets).forEach { (plot, target) ->
                    saveUndoSnapshot(target)
                    target.copyContentsFrom(plot)
                    if (target.name == null && incoming.size > 1) target.name = plot.name
                }
                shownPlot = targets.first()
                initPresetLayout()
                val into = if (targets.size == 1) GreenhouseData.fullPlotName(targets.first()) else shownPreset.displayName()
                ChatUtils.sendWithPrefix("Imported into $into")
            }
            incoming.size == 1 -> {
                saveUndoSnapshot(shown)
                shown.copyContentsFrom(incoming.first())
                initPresetLayout()
                ChatUtils.sendWithPrefix("Imported into ${GreenhouseData.fullPlotName(shown)}")
            }
            else -> importAsNewPreset(result)
        }
    }

    private fun importAsNewPreset(result: LayoutTransferResult.Imported) {
        val preset = GreenhouseLayout(id = result.layout.id, name = result.nameForPreset)
        result.plots.forEachIndexed { index, plot ->
            PlotLayout(id = preset.plotId(index), name = plot.name.takeIf { index > 0 || result.plots.size > 1 })
                .also { it.copyContentsFrom(plot) }
                .let(preset.plots::add)
        }
        addPresetLayout(preset)
    }

    private fun counted(count: Int, noun: String): String = "$count $noun" + if (count == 1) "" else "s"

    private fun facingTurns(): Int = when (Minecraft.getInstance().player?.direction) {
        Direction.EAST -> 1
        Direction.SOUTH -> 2
        Direction.WEST -> 3
        else -> 0
    }

    override fun onKeyPressed(event: KeyEvent): Boolean {
        if (overlaysKeyPressed(event)) return true
        if (currentDisplay == DisplayMode.Presets && plantPalette.keyPressed(event)) return true
        return super.onKeyPressed(event)
    }

    private fun openRenameContext(event: MouseButtonEvent, currentName: String, apply: (String) -> Unit) {
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            event.x.toInt(),
            event.y.toInt(),
            EditLayoutContextMenu.WIDTH,
            EditLayoutContextMenu.HEIGHT
        )

        val menu = EditLayoutContextMenu(menuX, menuY, currentName, this) { name ->
            apply(name)
            when (currentDisplay) {
                DisplayMode.Presets -> initPresetLayout()
                DisplayMode.Greenhouses -> {
                    displayedName = displayedGridWidget?.layout?.displayName() ?: displayedName
                    layoutNameBox()
                }
            }
        }
        addContext(menu)
    }

    private fun showDisplay(display: DisplayMode) {
        if (display == currentDisplay) return

        currentDisplay = display
        relayoutShelves()
        when (display) {
            DisplayMode.Greenhouses -> initGreenhouseLayout()
            DisplayMode.Presets -> initPresetLayout()
        }
    }

    private fun relayoutShelves() {
        layoutShelves()
        layoutGreenhouseWidgets()
        layoutPresetWidgets()
    }

    private fun showPrediction(ticks: Int) {
        val beforePrediction = gridWidgetBeforePrediction ?: displayedGridWidget ?: return

        if (ticks <= 0) {
            displayedGridWidget = beforePrediction
            gridWidgetBeforePrediction = null
            return
        }

        val grid = GreenhouseData.greenhouseGrids.firstOrNull { it.layout === beforePrediction.layout } ?: return

        gridWidgetBeforePrediction = beforePrediction
        displayedGridWidget = newGridWidget(grid.predictedLayout(ticks), gridTurns())
    }

    private fun dropPrediction() {
        predictSlider.set(0)
        gridWidgetBeforePrediction = null
    }

    private fun predictLabel(): String =
        if (predictSlider.value == 0) PREDICT_NOW else "+${predictSlider.value}"

    private fun predictWindowMs(): LongRange? {
        val ticks = predictSlider.value
        val tickMs = GreenhouseTickTime.tickMs ?: return null
        val remaining = GreenhouseTickTime.remainingTickMs() ?: return null

        return (remaining + (ticks - 1) * tickMs)..(remaining + ticks * tickMs)
    }

    private fun predictWindow(): String {
        if (predictSlider.value == 0) return ""
        val window = predictWindowMs() ?: return PREDICT_WINDOW_UNKNOWN

        return "in ${window.first.toShortDuration()} - ${window.last.toShortDuration()}"
    }

    private fun predictWindowClock(): String {
        if (predictSlider.value == 0) return ""
        val window = predictWindowMs() ?: return ""

        val now = ZonedDateTime.now()
        fun clockTime(msFromNow: Long): String {
            val at = now.plus(Duration.ofMillis(msFromNow))
            val time = at.format(CLOCK_FORMAT).lowercase()

            return if (at.toLocalDate() == now.toLocalDate()) time else "${at.format(DAY_FORMAT)} $time"
        }

        return "${clockTime(window.first)} - ${clockTime(window.last)}"
    }

    private fun showGreenhouse(layout: PlotLayout) {
        dropPrediction()
        val widget = greenhouseGridWidgets.find { it.layout == layout } ?: return

        GreenhouseData.greenhouseGrids
            .indexOfFirst { it.layout === widget.layout }
            .takeIf { it >= 0 }
            ?.let { GreenhouseData.currentGridIndex = it }

        displayedGridWidget = widget
        plotTabs.selected = widget.layout
        isPresetCleared = false
        displayedName = widget.layout.displayName()

        layoutNameBox()
    }

    private class PlotChoice(val plot: PlotLayout) {
        override fun toString(): String = GreenhouseData.nameInFull(plot)
    }

    private fun openAssignMenu(event: MouseButtonEvent) {
        val grid = displayedGrid() ?: return
        val choices = GreenhouseData.presetGrids.flatMap { it.plots }.map { PlotChoice(it) }

        if (choices.isEmpty()) {
            ChatUtils.sendWithPrefix("There are no presets to assign yet.")
            return
        }

        val menu = PickContext(event.x.toInt(), event.y.toInt(), "Assign:", choices, this) {
            assignPresetLayout(it.plot, grid)
        }
        menu.init()
        addContext(menu)
    }

    private fun turnPlan() {
        val grid = displayedGrid() ?: return
        if (grid.state.assignedLayout == null) return

        grid.state.planTurns = Math.floorMod(grid.state.planTurns + 1, 4)
        GreenhouseData.regenRender()

        val plan = grid.state.assignedLayout ?: return
        val planConfiguration = grid.configurationForLayout(plan.turnedBy(grid.state.planTurns))
        ChatUtils.sendWithPrefix("Plan turned to ${grid.state.planTurns * 90}°. $planConfiguration")
    }

    private fun editAssignedPreset() {
        val plot = displayedGrid()?.state?.assignedLayout ?: return
        val parentPreset = GreenhouseData.greenhouseLayoutFor(plot) ?: return

        GreenhouseData.currentPreset = parentPreset
        shownPlot = plot
        isPresetCleared = false
        showDisplay(DisplayMode.Presets)
    }

    private fun saveGreenhouseAsPreset() {
        val grid = displayedGrid() ?: return
        val savedPreset = GreenhouseLayout.create(GreenhouseData.computeNextAvailableId())
        val plot = savedPreset.plots.first()

        plot.copyContentsFrom(grid.layout)
        plot.slots.forEach { slot -> if (slot.soil == Blocks.AIR) slot.soil = null }

        isPresetCleared = false
        addPresetLayout(savedPreset)
        showDisplay(DisplayMode.Presets)

        ChatUtils.sendWithPrefix("Saved ${grid.layout.displayName()} as ${savedPreset.displayName()}")
    }

    private fun displayedGrid(): GreenhouseGrid? {
        val layout = displayedGridWidget?.layout ?: return null

        return GreenhouseData.greenhouseGrids.firstOrNull { it.layout === layout }
    }

    private fun assignPresetLayout(layout: PlotLayout?, grid: GreenhouseGrid) {
        if (layout == null) {
            ChatUtils.sendWithPrefix(
                "No such plan to run on ${grid.layout.displayName()}"
            )
            return
        }
        grid.state.assignedLayout = layout
        grid.state.planTurns = grid.bestRotationFor(layout)
        grid.state.buildAnnounced = false
        PlannerNeeds.forgetSentMessage(grid)
        GreenhouseData.regenRender()

        ChatUtils.sendWithPrefix(
            "Planner active on ${grid.layout.displayName()} for ${GreenhouseData.fullPlotName(layout)}"
        )
    }

    private fun addPlot() {
        val shownPreset = GreenhouseData.currentPreset ?: return
        if (shownPreset.plots.size >= GreenhouseLayout.MAX_PLOTS) {
            ChatUtils.sendWithPrefix("A preset holds at most ${GreenhouseLayout.MAX_PLOTS} plots, one a greenhouse.")
            return
        }
        shownPlot = shownPreset.addPlot()
        initPresetLayout()
    }

    private fun addPresetLayout(preset: GreenhouseLayout) {
        GreenhouseData.presetGrids.add(preset)
        GreenhouseData.currentPreset = preset
        shownPlot = null
        isPresetCleared = false
        initPresetLayout()
    }

    private fun removePresetLayout(plot: PlotLayout?) {
        val shownPreset = GreenhouseData.currentPreset
        if (shownPreset != null && plot != null && shownPreset.plots.size > 1) {
            saveHistoryStep(removePlot(shownPreset, plot))
            initPresetLayout()
            return
        }

        val current = GreenhouseData.currentPreset ?: run {
            ChatUtils.sendWithPrefix("No preset to remove.")
            return
        }
        saveHistoryStep(removePreset(current))
        initPresetLayout()
    }

    private fun removePlot(preset: GreenhouseLayout, plot: PlotLayout): HistoryStep.PlotRemoved {
        val index = preset.plots.indexOfFirst { it === plot }
        preset.plots.removeAt(index)
        stopPlannersOn(plot)
        shownPlot = null
        return HistoryStep.PlotRemoved(preset, plot, index)
    }

    private fun removePreset(preset: GreenhouseLayout): HistoryStep.PresetRemoved {
        preset.plots.forEach { stopPlannersOn(it) }
        val presets = GreenhouseData.presetGrids
        val index = presets.indexOfFirst { it === preset }
        presets.removeAt(index)
        GreenhouseData.currentPreset = presets.getOrNull(index) ?: presets.lastOrNull()
        return HistoryStep.PresetRemoved(preset, index, presets)
    }

    companion object {
        private var lastDisplay: DisplayMode = DisplayMode.Greenhouses
        private val undoStack = ArrayDeque<HistoryStep>()
        private val redoStack = ArrayDeque<HistoryStep>()
        private var warnedMissingGreenhouses: Boolean = false
        private var contentsTab: ContentsTab = ContentsTab.Contents
        private var awayTicks: Int = 3

        private const val MIN_ACTION_ROW_WIDTH: Int = 90
        private const val TOOLBAR_WIDTH: Int = 180
        private const val MIN_SLOT_SIZE: Int = 8
        private const val BORDER_PADDING: Int = 6
        private const val COMFORTABLE_WIDTH: Int = 800
        private const val COMFORTABLE_HEIGHT: Int = 400
        private val DRAW_SCALES: List<Float> = listOf(1f, 0.75f, 0.5f)
        private const val NAME_TOP: Int = 9
        private const val TICK_TIME_LEFT: Int = 10
        private const val TICK_TIME_CENTER_Y: Int = 18
        private const val TELEPORT_WIDTH: Int = 110
        private const val PREDICT_LABEL_WIDTH: Int = 22
        private const val UNPLANNED_LINE_HEIGHT: Int = 12
        private const val CONTENTS_ICON_SIZE: Int = 12
        private const val CONTENTS_ROW_HEIGHT: Int = CONTENTS_ICON_SIZE + 2
        private const val CONTENTS_CHECKBOX_SIZE: Int = 9
        private const val UNIQUE_LINE: Int = 1
        private const val ATTRIBUTE_LINE: Int = 3
        private const val OFF_SCREEN: Double = -1.0

        private const val MAX_PREDICT_TICKS: Int = 10
        private const val MAX_AWAY_TICKS: Int = 20
        private const val HISTORY_LIMIT: Int = 50

        private const val MAX_UNIQUE_CROPS: Int = 12
        private const val MAX_SPEED_UPGRADE: Int = 9
        private const val MAX_ATTRIBUTE: Int = 10

        private const val DROP_OK: Int = 0x6000FF00
        private const val DROP_BLOCKED: Int = 0x60FF0000

        private const val EMPTY_GRID_ID: String = "preset_none"
        private const val TOGGLE_PLOTS: String = "Plots"
        private const val TOGGLE_PRESETS: String = "Presets"
        private const val TELEPORT_LABEL: String = "Teleport to Plot"
        private const val SHELF_VIEW: String = "View"
        private const val SHELF_PREDICT: String = "Prediction"
        private const val SHELF_GREENHOUSE: String = "Greenhouse"
        private const val SHELF_PRESET: String = "Preset"
        private const val PREDICT_NOW: String = "Now"
        private const val PREDICT_WINDOW_UNKNOWN: String = "tick time unknown"
        private val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
        private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        private const val UNPLANNED_NONE: String = "Nothing can grow unplanned"
        private const val WATER_LASTS_TOOLTIP: String =
            "How many ticks one full watering lasts before the first plant runs out. " +
                    "A plant out of water can skip growth ticks, and dies if it stays out too long."
        private const val SET_ATTRIBUTE_HINT: String = "Click to open chat to set your attribute level"
        private const val RENAME_HINT: String = "\nRight click a name to rename it: greenhouses, presets and plots"
        private const val SCROLL_HINT_GREENHOUSES: String = "Scroll the mouse wheel to switch what the plants show$RENAME_HINT"
        private const val SCROLL_HINT_PRESETS: String = "Scroll the mouse wheel to switch preset$RENAME_HINT"
    }
}

private enum class ContentsTab(val label: String) {
    Contents("Contents"),
    Report("Report");

    companion object {
        val shown: List<ContentsTab> = listOf(Contents)
    }
}
