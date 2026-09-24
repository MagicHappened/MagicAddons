package org.magic.magicaddons.ui.screens

import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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


    enum class CurrentDisplay {
        Greenhouses,
        Presets
    }

    private var paddingY: Int = 0
    private var startX: Int = 0
    private var startY: Int = 0
    private var containerSize: Int = 0

    /** Plots or presets, kept past the screen so it reopens on whichever was last shown. */
    private var currentDisplay: CurrentDisplay
        get() = lastDisplay
        set(value) { lastDisplay = value }

    /** Whether the greenhouse on screen is running on guessed growth. Read while drawing, since the
     * tick that makes it stale can land with the screen open. */
    private val shouldWarn: Boolean
        get() = currentDisplay == CurrentDisplay.Greenhouses &&
                GreenhouseData.greenhouseGrids.getOrNull(GreenhouseData.currentGridIndex)
                    ?.let { it.state.ticksSinceLastScan > 0 } == true

    /** 0.5 when the window has too few gui units for the panels, so everything is drawn half size. */
    private var drawScale: Float = 1f

    override var hoveredElement: GuiEventListener? = null

    override val overlays = mutableListOf<OverlayRenderable>()
    private var displayedGridWidget: GridWidget? = null
    private val greenhouseGridWidgets: MutableList<GridWidget> = mutableListOf()
    private val presetGridWidgets: MutableList<GridWidget> = mutableListOf()

    /** The two modes side by side, the one on screen drawn pressed in so it is plain which is showing. */
    private val plotsButton = ClickableButtonWidget(TOGGLE_PLOTS)
    private val presetsButton = ClickableButtonWidget(TOGGLE_PRESETS)

    /** What the player can do to the greenhouse on screen, the other end of the Planner button. */
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

    /** Where a mode's own buttons begin, shared so the two modes line up with each other. */
    private var actionRowX: Int = 0
    private var actionRowY: Int = 0

    /** The two shelves down the left: what is shown, and what can be done to it. */
    private var shelfLeft: Int = 0
    private var shelfWidth: Int = 0
    private var viewShelfY: Int = 0
    private var viewShelfHeight: Int = 0
    private var predictShelfY: Int = 0
    private var predictShelfHeight: Int = 0
    private var actionShelfY: Int = 0

    /** The greenhouse widget a prediction stands in for, put back when the slider returns to zero. */
    private var predictionBase: GridWidget? = null

    private val predictSlider = SliderWidget { showPrediction(it) }

    /** Whether the mouse is on the next tick box, which then explains the clock. */
    private var timeHovered = false

    /** Clicked, the box keeps its breakdown on screen until clicked again. */
    private var timePinned = false

    /** The box, as drawn last, so a click can find it. */
    private var timeBox: IntArray = IntArray(4)

    /** The name box at the top, with room beside it for the warning badge: left, top, right, bottom. */
    private var nameBox: IntArray = IntArray(4)
    private var hoverWarning = false

    private val gridSelector = EnumWidget(
        values = emptyList<GreenhouseLayout>(),
        currentValue = null as GreenhouseLayout?,
        onRightClickValue = { master, event ->
            master?.let { openRenameContext(event, it.displayName()) { name -> it.name = name } }
        },
        valueChanged = { presetChanged(it) },
        overlayContext = this
    )

    private val presetUI = PresetUI(
        this,
        onAssignedLayout = { assignedLayout, selectedGrid ->
            assignPresetLayout(assignedLayout, selectedGrid)
        },
        onImported = { result, soft -> imported(result, soft) },
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
                gridWidgetChanged(layout)
            }
        }
    )

    private val teleportTab = Bookmarks<String>(
        side = Bookmarks.Side.Bottom,
        label = { it },
        onPick = { _, _ ->
            displayedGridWidget?.layout
                ?.takeIf { it.kind == PlotLayout.Kind.PRESET }
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

    private var lastPainted: Pair<Int, Int>? = null

    private sealed interface PartTab {
        data class Part(val layout: PlotLayout) : PartTab
        data object Add : PartTab
    }

    private fun partTitle(layout: PlotLayout): String =
        GreenhouseData.greenhouseLayoutFor(layout)?.plotTitle(layout) ?: layout.displayName()

    private var shownPlot: PlotLayout? = null

    private val partTabs = Bookmarks<PartTab>(
        side = Bookmarks.Side.Top,
        label = { tab -> if (tab is PartTab.Part) partTitle(tab.layout) else "+" },
        tooltip = { tab -> if (tab is PartTab.Part) "Right click to rename" else "Add a plot to this preset" },
        onPick = { tab, event ->
            when {
                tab is PartTab.Add -> addPlot()
                tab is PartTab.Part && event.button() == 1 ->
                    openRenameContext(event, partTitle(tab.layout)) { name -> tab.layout.name = name }
                tab is PartTab.Part -> {
                    shownPlot = tab.layout
                    initPresetLayout()
                }
            }
        }
    )

    private var emptyGridWidget: GridWidget? = null

    private var presetCleared = false

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
        paddingY = height / 10

        startY = maxOf(paddingY, NAME_TOP + boxHeight(" ") + Common.UI.SPACING + Bookmarks.REACH + BORDER_PADDING)
        val bottomRoom = Bookmarks.REACH + Common.UI.SPACING + cropPreviewButton.height + Common.UI.SPACING_LARGE

        val sideRoom = TOOLBAR_WIDTH + HoverControls.TOTAL_WIDTH + Common.UI.SPACING_LARGE * 2
        val heightRoom = height - startY - BORDER_PADDING * 2 - bottomRoom
        val widthRoom = width - sideRoom - BORDER_PADDING * 2

        slotSize = GridWidget.slotSizeFor(minOf(heightRoom, widthRoom), GREENHOUSE_SIZE)
            .coerceAtLeast(MIN_SLOT_SIZE)

        containerSize = GridWidget.spanFor(slotSize, GREENHOUSE_SIZE)

        startX = ((width - containerSize) / 2).coerceAtLeast(TOOLBAR_WIDTH + Common.UI.SPACING_LARGE)

        layoutShelves()

        cropPreviewButton.x = (width - cropPreviewButton.width) / 2
        cropPreviewButton.y = height - cropPreviewButton.height - Common.UI.SPACING_LARGE - 2

        layoutGreenhouseWidgets()
        layoutPresetWidgets()

        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> initGreenhouseLayout()
            CurrentDisplay.Presets -> initPresetLayout()
        }
    }


    private fun layoutShelves() {
        shelfLeft = Common.UI.SPACING_LARGE
        shelfWidth = (startX - BORDER_PADDING - Common.UI.SPACING_LARGE - shelfLeft).coerceAtLeast(MIN_ACTION_ROW_WIDTH)
        viewShelfY = startY - BORDER_PADDING

        plotsButton.x = shelfLeft + ActionPanel.PADDING
        plotsButton.y = viewShelfY + shelfTitleHeight() + ActionPanel.PADDING
        presetsButton.x = plotsButton.x + plotsButton.width + Common.UI.SPACING
        presetsButton.y = plotsButton.y

        gridSelector.x = presetsButton.x + presetsButton.width + Common.UI.SPACING
        gridSelector.y = plotsButton.y
        gridSelector.height = plotsButton.height
        gridSelector.closeList()

        viewShelfHeight = shelfTitleHeight() + ActionPanel.PADDING * 2 + plotsButton.height

        predictShelfY = viewShelfY + viewShelfHeight + Common.UI.SPACING_LARGE
        predictShelfHeight = if (currentDisplay == CurrentDisplay.Greenhouses) {
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
        plotTabs.layoutAlong(startX - BORDER_PADDING, startY - BORDER_PADDING, containerSize + BORDER_PADDING * 2)

        teleportTab.layoutAlong(
            startX + (containerSize - TELEPORT_WIDTH) / 2,
            startY + containerSize + BORDER_PADDING,
            TELEPORT_WIDTH
        )

        greenhousePanel.layoutIn(actionRowX, actionRowY, shelfWidth)
        hoverControls.layoutAgainstGrid(startX + containerSize, startY, containerSize)
    }

    private fun layoutPresetWidgets() {
        partTabs.layoutAlong(startX - BORDER_PADDING, startY - BORDER_PADDING, containerSize + BORDER_PADDING * 2)

        presetUI.layoutIn(actionRowX, actionRowY, shelfWidth)

        val paletteY = actionShelfY + shelfTitleHeight() + presetUI.contentHeight + UNPLANNED_LINE_HEIGHT + Common.UI.SPACING_LARGE
        val frameBottom = startY + containerSize + BORDER_PADDING
        plantPalette.layout(shelfLeft, paletteY, shelfWidth, frameBottom - paletteY)

        emptyGridWidget = newGridWidget(PlotLayout(id = EMPTY_GRID_ID), turns = 0)
    }

    
    private fun newGridWidget(layout: PlotLayout, turns: Int): GridWidget =
        GridWidget(layout, slotSize).apply {
            this.turns = turns
            x = startX
            y = startY
            targetPlan = if (layout.kind == PlotLayout.Kind.MASTER_PRESET) {
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
        val amountInitialized = GreenhouseData.greenhouseGrids.count { it.state.lastScanTime != null }
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
        if (amountInitialized != PlotAPI.plots.count { it.data?.isGreenhouse ?: throw IllegalStateException("Plot data was null after null check.") }){
            if (!warnedMissingGreenhouses){
                warnedMissingGreenhouses = true
                ChatUtils.sendWithPrefix("Not all greenhouses available, enter them to see them.")
            }
        }

        val currentPlot = PlotAPI.getCurrentPlot()?.id
        GreenhouseData.greenhouseGrids.forEachIndexed { index, grid ->
            if (grid.state.lastScanTime == null) return@forEachIndexed
            greenhouseGridWidgets.add(newGridWidget(grid.layout, gridTurns()))
            if (grid.layout.kind == PlotLayout.Kind.PRESET && grid.layout.number == currentPlot) {
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

        layoutName()
    }

    private fun initPresetLayout() {
        dropPrediction()
        presetGridWidgets.clear()
        displayedGridWidget = null
        hoveredElement = null

        if (GreenhouseData.currentPreset == null && !presetCleared) {
            GreenhouseData.currentPreset = GreenhouseData.presetGrids.firstOrNull()
        }
        val master = GreenhouseData.currentPreset?.takeUnless { presetCleared }
        if (master != null && shownPlot !in master.plots) shownPlot = null

        master?.plots?.forEach { plot ->
            presetGridWidgets.add(newGridWidget(plot, turns = 0))
        }
        val shown = shownPlot ?: master?.plots?.firstOrNull()
        displayedGridWidget = presetGridWidgets.find { it.layout === shown }

        partTabs.items = if (master == null) emptyList() else buildList {
            master.plots.forEach { add(PartTab.Part(it)) }
            if (master.plots.size < GreenhouseLayout.MAX_PLOTS) add(PartTab.Add)
        }
        partTabs.selected = shown?.let { PartTab.Part(it) }

        displayedName = master?.displayName() ?: "Unknown Preset"

        gridSelector.currentValue = master
        gridSelector.values = GreenhouseData.presetGrids.toList()
        relayoutSelector()

        layoutName()
    }

    private fun relayoutSelector() {
        val room = shelfLeft + shelfWidth - ActionPanel.PADDING - Common.UI.SPACING - ScrollHint.SIZE - gridSelector.x
        gridSelector.fitToValues(room)
    }

    private fun layoutName() {
        val boxHeight = boxHeight(displayedName)
        val boxWidth = font.width(displayedName) + Common.UI.TEXT_X_PAD * 2
        val left = (width - boxWidth) / 2
        nameBox = intArrayOf(left, NAME_TOP, left + boxWidth + Common.UI.SPACING + boxHeight, NAME_TOP + boxHeight)
    }

    private fun overName(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, nameBox[0], nameBox[1], nameBox[2] - nameBox[0], nameBox[3] - nameBox[1])

    
    private fun drawNameBox(graphics: GuiGraphicsExtractor) {
        val boxHeight = nameBox[3] - nameBox[1]
        graphics.drawMultilineBoxCentered(
            displayedName,
            width / 2,
            NAME_TOP + boxHeight / 2,
            if (shouldWarn) Common.UI.WARNING_COLOR else null
        )
        if (shouldWarn) graphics.drawWarningBadge(warningBadgeX(), NAME_TOP, boxHeight)
    }

    private fun shelfTitleHeight(): Int = font.lineHeight + Common.UI.SPACING * 2

    
    private fun drawPinnedClock(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val lines = clockTooltip().split('\n').map { Component.literal(it).visualOrderText }
        pinnedClockBox = graphics.drawTooltipLines(lines, TIME_LEFT, timeBox[3] + Common.UI.SPACING)

        if (overClockLine(mouseX, mouseY, UNIQUE_LINE)) {
            drawMissingUniques(graphics, mouseX, mouseY)
        }

        if (overClockLine(mouseX, mouseY, ATTRIBUTE_LINE)) {
            graphics.drawTooltipAtCursor(SET_ATTRIBUTE_HINT, mouseX, mouseY)
        }
    }

    private var pinnedClockBox: IntArray = IntArray(4)

    private fun overClockLine(mouseX: Int, mouseY: Int, line: Int): Boolean {
        val box = pinnedClockBox
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

    private fun clockTooltip(): String {
        val misc = GreenhouseData.miscInfo

        fun graded(value: Int?, max: Int): String {
            value ?: return "§8?§7/$max"
            val third = value * 3
            val colour = when {
                third < max -> "§c"
                third < max * 2 -> "§e"
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
            "§7Unique crops: " + graded(GreenhouseData.getCurrentUniques().size, MAX_UNIQUE_CROPS),
            "§7Greenhouse speed upgrade: " + graded(misc.cropSpeedUpgradeValue, MAX_SPEED_UPGRADE),
            "§7Greenhouse attribute: " + graded(GreenhouseTickTime.speedAttribute(), MAX_ATTRIBUTE),
            "§7Crop growth: §f" + (misc.cropGrowthValue?.toString() ?: "§8?")
        ).joinToString("\n")
    }

    /** Where the carried plant or soil would land, green when it fits and red when it cannot. */
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
        val fits = item !is PaletteItem.Crop || canPlace(grid.layout, item.def, sx, sy)
        graphics.fill(x1, y1, x2, y2, if (fits) DROP_OK else DROP_BLOCKED)
    }

    /** Whether the footprint fits inside the grid; whatever stands there already is replaced. */
    private fun canPlace(layout: PlotLayout, def: CropDefinition, sx: Int, sy: Int): Boolean {
        val footprint = def.footprint
        return sx + footprint.width <= layout.size && sy + footprint.height <= layout.size
    }

    /** Every plant whose footprint shares a slot with the one about to be placed. */
    private fun overlapping(layout: PlotLayout, def: CropDefinition, sx: Int, sy: Int): List<Plant> {
        val footprint = def.footprint
        return layout.plants.filter { other ->
            val ow = other.cropDef.footprint.width
            val oh = other.cropDef.footprint.height
            sx < other.slot.x + ow && other.slot.x < sx + footprint.width &&
                    sy < other.slot.y + oh && other.slot.y < sy + footprint.height
        }
    }

    /** Every plant whose footprint covers the slot, which is at most one. */
    private fun plantsCovering(layout: PlotLayout, sx: Int, sy: Int): List<Plant> =
        layout.plants.filter { plant ->
            sx in plant.slot.x until plant.slot.x + plant.cropDef.footprint.width &&
                    sy in plant.slot.y until plant.slot.y + plant.cropDef.footprint.height
        }

    /** Picks up what a cell holds, the plant over the soil, the way creative picks a block. */
    private fun pickFromCell(event: MouseButtonEvent): Boolean {
        val grid = displayedGridWidget ?: return false
        val (sx, sy) = grid.slotAt(event.x, event.y) ?: return false

        val plant = plantsCovering(grid.layout, sx, sy).firstOrNull()
        val picked = when {
            plant != null -> PaletteItem.Crop(plant.cropDef)
            else -> grid.layout.getSlot(sx, sy)?.soil?.let { PaletteItem.Soil(it) }
        } ?: return false

        plantPalette.pickUp(picked)
        return true
    }

    /** Empties a cell of the preset: the plant covering it and the soil under it. */
    private fun clearCell(sx: Int, sy: Int): Boolean {
        val grid = displayedGridWidget ?: return false
        val slot = grid.layout.getSlot(sx, sy) ?: return false
        val covering = plantsCovering(grid.layout, sx, sy)
        if (covering.isEmpty() && slot.soil == null) return false

        remember(grid.layout)
        covering.forEach { grid.noteVanishing(it) }
        grid.layout.plants.removeAll(covering)
        slot.soil = null
        grid.init()
        return true
    }

    /** Marks the plant covering a cell, so a wide plant is marked from any of its cells. */
    private fun markCell(sx: Int, sy: Int, marking: LayoutSlot.Marking?): Boolean {
        val grid = displayedGridWidget ?: return false
        val plant = plantsCovering(grid.layout, sx, sy).firstOrNull() ?: return false

        applyMark(plant, marking)
        return true
    }

    /**
     * What a click does to a cell, done again to each cell the mouse is dragged across: putting the
     * picked plant down, emptying the cell, or marking it, by which switch is on.
     */
    private fun paintUnderMouse(event: MouseButtonEvent): Boolean {
        val grid = displayedGridWidget ?: return false
        val cell = grid.slotAt(event.x, event.y) ?: return false
        if (cell == lastPainted) return true

        val picked = plantPalette.selected
        val acted = when {
            picked != null -> { placeDragged(picked, event.x, event.y); true }
            plantPalette.deleteMode -> { clearCell(cell.first, cell.second); true }
            plantPalette.markChoice.applies -> { markCell(cell.first, cell.second, plantPalette.markChoice.marking); true }
            else -> false
        }
        if (acted) lastPainted = cell

        return acted
    }

    /** Drops the carried plant or soil onto the preset at the mouse, when it fits there. With no preset, one is started. */
    private fun placeDragged(item: PaletteItem, mouseX: Double, mouseY: Double) {
        if (currentDisplay != CurrentDisplay.Presets) return
        if (displayedGridWidget == null) {
            if (emptyGridWidget?.slotAt(mouseX, mouseY) == null) return
            presetCleared = false
            addPresetLayout(GreenhouseLayout.create(GreenhouseData.computeNextAvailableId()))
        }
        val grid = displayedGridWidget ?: return
        val (sx, sy) = grid.slotAt(mouseX, mouseY) ?: return
        val slot = grid.layout.getSlot(sx, sy) ?: return

        // soil goes under whatever stands there
        if (item is PaletteItem.Soil) {
            remember(grid.layout)
            slot.soil = item.block
            grid.init()
            return
        }
        val def = (item as PaletteItem.Crop).def

        // with Merge on, a crop dropped on a plant joins that slot
        val standing = plantsCovering(grid.layout, sx, sy).firstOrNull()
        if (plantPalette.mergeMode && standing != null) {
            mergeInto(grid, standing, def)
            return
        }

        if (!canPlace(grid.layout, def, sx, sy)) return

        remember(grid.layout)
        grid.layout.plants.removeAll(overlapping(grid.layout, def, sx, sy))

        // the plant brings the first soil it accepts with it, under every slot it covers
        def.requiredSoil.firstOrNull()?.let { soil ->
            for (dx in 0 until def.footprint.width) {
                for (dy in 0 until def.footprint.height) {
                    grid.layout.getSlot(sx + dx, sy + dy)?.soil = soil
                }
            }
        }

        val instance = Plant(def.elementId, slot, null, null, cropDef = def)
        grid.layout.plants.add(instance)
        grid.justPlaced.add(instance)
        grid.init()
    }

    /** Adds [def] to the crops [standing]'s slot may hold; the plant is swapped for a copy so undo works. */
    private fun mergeInto(grid: GridWidget, standing: Plant, def: CropDefinition) {
        if (standing.cropDef.footprint != def.footprint) {
            ChatUtils.sendWithPrefix("Only crops of the same size can share a slot.")
            return
        }
        if (standing.acceptsCrop(def)) return

        remember(grid.layout)
        val merged = standing.copyForPrediction(standing.slot).also { it.presetAlternatives.add(def) }
        grid.layout.plants.remove(standing)
        grid.layout.plants.add(merged)
        merged.slot.mark = LayoutSlot.Marking.Target
        grid.justMarked.add(merged)
        grid.init()
    }

    /** Lets the player say what a plant in the plan stands for, written onto its slot. */
    private fun openMarkContext(instance: Plant, event: MouseButtonEvent) {
        val grid = displayedGridWidget ?: return

        // a merged slot is always a target; clearing the mark unmerges it
        val options = if (instance.hasAlternatives) listOf(MarkOption.Target, MarkOption.None) else MarkOption.entries
        val menu = PickContext(event.x.toInt(), event.y.toInt(), "Mark as:", options, this) { option ->
            applyMark(instance, option.marking)
        }
        menu.init()
        addContext(menu)
    }

    /** Writes a mark onto a plant's slot; clearing a merged slot's mark unmerges it. */
    private fun applyMark(instance: Plant, marking: LayoutSlot.Marking?) {
        val grid = displayedGridWidget ?: return
        if (instance.hasAlternatives && marking == LayoutSlot.Marking.Ingredient) return

        remember(grid.layout)
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

    /**
     * Whether a base crop has no plant of its kind anywhere in the preset. Sunflower and moonflower
     * count as one kind, as do the two mushrooms, the way the garden counts its uniques.
     */
    private fun uniqueMissingFromPreset(def: CropDefinition): Boolean {
        if (!def.isBaseCrop) return false

        val layout = displayedGridWidget?.layout ?: return false
        val plots = GreenhouseData.greenhouseLayoutFor(layout)?.plots ?: listOf(layout)
        val key = GreenhouseData.UniqueCropKey.from(def)

        return plots.flatMap { it.plants }.none {
            it.cropDef.isBaseCrop && GreenhouseData.UniqueCropKey.from(it.cropDef) == key
        }
    }

    /**
     * One plot as it stood before an action: placing, replacing, removing a plant or soil, or
     * marking. The arrows walk these back and forward.
     */
    private class PresetSnapshot(
        val layout: PlotLayout,
        val elements: List<Plant>,
        val slots: List<Triple<LayoutSlot, Block?, LayoutSlot.Marking?>>
    )

    private val undoStack = ArrayDeque<PresetSnapshot>()
    private val redoStack = ArrayDeque<PresetSnapshot>()

    private fun snapshot(layout: PlotLayout) = PresetSnapshot(
        layout,
        layout.plants.toList(),
        layout.slots.map { Triple(it, it.soil, it.mark) }
    )

    /** Called before every action; a new action forgets whatever had been undone. */
    private fun remember(layout: PlotLayout) {
        undoStack.addLast(snapshot(layout))
        if (undoStack.size > HISTORY_LIMIT) undoStack.removeFirst()
        redoStack.clear()
    }

    private fun restore(saved: PresetSnapshot) {
        saved.layout.plants.clear()
        saved.layout.plants.addAll(saved.elements)
        saved.slots.forEach { (slot, block, mark) ->
            slot.soil = block
            slot.mark = mark
        }
        // the plot may sit on another bookmark, or in another preset, than the one on show
        if (presetGridWidgets.none { it.layout === saved.layout }) {
            GreenhouseData.greenhouseLayoutFor(saved.layout)?.let { master ->
                GreenhouseData.currentPreset = master
                shownPlot = saved.layout
                presetCleared = false
            }
        } else if (displayedGridWidget?.layout !== saved.layout) {
            shownPlot = saved.layout
        }
        initPresetLayout()
    }

    private fun undo() {
        val saved = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshot(saved.layout))
        restore(saved)
    }

    private fun redo() {
        val saved = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshot(saved.layout))
        restore(saved)
    }

    /** Asks before emptying the plot on show of every plant, soil and mark; one arrow step brings it back. */
    private fun askClearPlot(event: MouseButtonEvent) {
        val grid = displayedGridWidget ?: return
        val question = "Clear ${GreenhouseData.fullPlotName(grid.layout)}?"
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(event.x.toInt(), event.y.toInt(), ConfirmContext.widthFor(question), ConfirmContext.HEIGHT)
        addContext(ConfirmContext(menuX, menuY, question, this) {
            remember(grid.layout)
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
            val result = type.format.export(asPreset(grid.layout))

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

    /**
     * A greenhouse as a preset of it: its soils, marks and crops, and nothing only a standing plant
     * has, such as water, stage or age.
     */
    private fun asPreset(layout: PlotLayout): PlotLayout {
        val preset = PlotLayout(id = layout.id, name = layout.displayName(), size = layout.size)

        preset.slots.forEach { slot ->
            val theirs = layout.getSlot(slot.x, slot.y)
            slot.soil = theirs?.soil
            slot.mark = theirs?.mark
        }
        layout.plants.forEach { instance ->
            val slot = preset.getSlot(instance.slot.x, instance.slot.y) ?: return@forEach
            preset.plants.add(
                Plant(instance.elementId, slot, cropDef = instance.cropDef, presetAlternatives = instance.presetAlternatives.toMutableList())
            )
        }
        return preset
    }

    /** Asks before forgetting every plant of the greenhouse on show and reading it again from nothing. */
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

    /** Stops every planner running [plot]. */
    private fun stopPlannersOn(plot: PlotLayout) {
        GreenhouseData.greenhouseGrids
            .filter { it.state.assignedLayout === plot }
            .forEach { GreenhouseData.unplanGreenhouse(it) }
    }

    /** Turns the shown plot a quarter turn, clockwise for 1; a planner running it keeps its place in the world. */
    private fun turnShownPlot(turns: Int) {
        val grid = displayedGridWidget ?: return

        remember(grid.layout)
        grid.layout.copyContentsFrom(grid.layout.turnedBy(turns))

        GreenhouseData.greenhouseGrids
            .filter { it.state.assignedLayout === grid.layout }
            .forEach { it.state.planTurns = Math.floorMod(it.state.planTurns - turns, 4) }

        grid.init()
    }

    /** A new preset with one empty plot, shown at once. */
    private fun newPreset() {
        addPresetLayout(GreenhouseLayout.create(GreenhouseData.computeNextAvailableId()))
    }

    /** Where the badge beside the name starts, so its tooltip can hang under it. */
    private fun warningBadgeX(): Int =
        (width + font.width(displayedName) + Common.UI.TEXT_X_PAD * 2) / 2 + Common.UI.SPACING

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.pose().pushMatrix()
        graphics.pose().scale(drawScale, drawScale)
        extractScaled(graphics, (mouseX / drawScale).toInt(), (mouseY / drawScale).toInt(), delta)
        graphics.pose().popMatrix()
    }

    /** Quarter turns the grid picture gets so the way the player faces is up; none unless asked for. */
    private fun gridTurns(): Int {
        if (!GreenhousePresets.turnsGridWithPlayer()) return 0
        return when (Minecraft.getInstance().player?.direction) {
            Direction.EAST -> 3
            Direction.SOUTH -> 2
            Direction.WEST -> 1
            else -> 0
        }
    }

    /** The player may turn while the screen is open, so the greenhouse grids follow before each frame; presets never turn. */
    private fun followPlayerTurn() {
        val turns = gridTurns()
        greenhouseGridWidgets
            .filter { it.turns != turns }
            .forEach {
                it.turns = turns
                it.init()
            }
    }

    /** The whole screen, in layout units. */
    private fun extractScaled(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        followPlayerTurn()

        // read before the shelves are drawn, since what the panel says decides how tall it is
        greenhousePanel.assigned = displayedGrid()?.let { grid ->
            grid.state.assignedLayout?.let { plan ->
                // the turn the plan is laid at is what decides which slot is which, so it is said
                GreenhouseData.nameInFull(plan) + if (grid.state.planTurns == 0) "" else " (turned ${grid.state.planTurns * 90}°)"
            }
        }

        // the bookmarks first, so the frame drawn next covers where they tuck under it
        if (displayedGridWidget != null) {
            when (currentDisplay) {
                CurrentDisplay.Greenhouses -> {
                    plotTabs.render(graphics)
                    hoverControls.extractRenderState(graphics, mouseX, mouseY, delta)
                    teleportTab.render(graphics)
                }
                CurrentDisplay.Presets -> partTabs.render(graphics)
            }
        }

        graphics.drawPanel(
            startX - BORDER_PADDING,
            startY - BORDER_PADDING,
            startX + containerSize + BORDER_PADDING,
            startY + containerSize + BORDER_PADDING
        )

        drawNameBox(graphics)
        drawTimeBox(graphics, mouseX, mouseY)

        displayedGridWidget?.extractRenderState(graphics, mouseX, mouseY, delta)
        if (displayedGridWidget == null && currentDisplay == CurrentDisplay.Presets) {
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

        val panel = if (currentDisplay == CurrentDisplay.Greenhouses) greenhousePanel else presetUI
        unplannedLineBox = null
        waterLastsLineBox = null
        if (panel.hasShown()) {
            val title = if (currentDisplay == CurrentDisplay.Greenhouses) SHELF_GREENHOUSE else SHELF_PRESET
            val lineTop = actionShelfY + shelfTitleHeight() + panel.contentHeight
            // without a plan nothing counts as unplanned
            val hasPlan = displayedGridWidget?.targetPlan?.invoke() != null
            val bottom = lineTop + if (hasPlan) UNPLANNED_LINE_HEIGHT else 0
            graphics.drawShelf(shelfLeft, actionShelfY, shelfLeft + shelfWidth, bottom, title)
            if (hasPlan) renderUnplannedMutationsLine(graphics, lineTop, mouseX, mouseY)
        }

        // greenhouse mode keeps the hover controls against the grid, so the shelf starts past them
        val rightShelfLeft = if (currentDisplay == CurrentDisplay.Greenhouses) {
            hoverControls.x + hoverControls.width + Common.UI.SPACING_LARGE
        } else {
            startX + containerSize + BORDER_PADDING + Common.UI.SPACING_LARGE
        }
        val rightShelfWidth = minOf(shelfWidth, width - rightShelfLeft - Common.UI.SPACING_LARGE)
        renderContentsShelf(graphics, rightShelfLeft, startY - BORDER_PADDING, rightShelfWidth.coerceAtLeast(MIN_ACTION_ROW_WIDTH), mouseX, mouseY)

        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> renderGreenhouseMode(graphics, mouseX, mouseY, delta)
            CurrentDisplay.Presets -> renderPresetMode(graphics, mouseX, mouseY, delta)
        }
        scrollHint.extractRenderState(graphics, mouseX, mouseY)

        plotsButton.pressed = currentDisplay == CurrentDisplay.Greenhouses
        presetsButton.pressed = currentDisplay == CurrentDisplay.Presets
        plotsButton.extractRenderState(graphics, mouseX, mouseY, delta)
        presetsButton.extractRenderState(graphics, mouseX, mouseY, delta)
        cropPreviewButton.extractRenderState(graphics, mouseX, mouseY, delta)

        if (hoverWarning) {
            // under the badge rather than at the cursor, so it never covers the name it is about
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
            CurrentDisplay.Greenhouses -> {
                plotTabs.renderTooltip(graphics, mouseX, mouseY)
                hoverControls.renderTooltip(graphics, mouseX, mouseY)
                teleportTab.renderTooltip(graphics, mouseX, mouseY)
            }
            CurrentDisplay.Presets -> {
                if (displayedGridWidget != null) partTabs.renderTooltip(graphics, mouseX, mouseY)
                plantPalette.renderDrag(graphics)
                plantPalette.renderTooltip(graphics, mouseX, mouseY)
            }
        }

        if (timePinned) {
            drawPinnedClock(graphics, mouseX, mouseY)
        } else if (timeHovered) {
            graphics.drawSimpleTooltip(clockTooltip(), TIME_LEFT, TIME_CENTER_Y + boxHeight(" ") / 2 + Common.UI.SPACING)
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

        // hovering the star beside a water time shows the star's own tooltip instead of the plant's
        hovered.deadTooltipAt(mouseX, mouseY)?.let {
            graphics.drawTooltipAtCursor(it, mouseX, mouseY)
            return
        }

        hovered.debtTooltipAt(mouseX, mouseY)?.let {
            graphics.drawTooltipAtCursor(it, mouseX, mouseY)
            return
        }

        hovered.chargeTooltipAt(mouseX, mouseY)?.let {
            graphics.drawTooltipAtCursor(it, mouseX, mouseY)
            return
        }
        // under the contents shelf, so neither it nor the swatches against the grid are covered
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

    private fun unplannedLineClicked(event: MouseButtonEvent): Boolean {
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

        // measured first, since the shelf behind the rows is drawn before them
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
        val counted = layout.plants
            .groupingBy { it.cropDef }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<CropDefinition, Int>> { it.value }.thenBy { it.key.name })
        if (counted.isEmpty()) return null

        val withChecklist = currentDisplay == CurrentDisplay.Greenhouses
        val frameBottom = startY + containerSize + BORDER_PADDING
        val rowsThatFit = ((frameBottom - rowsTop - ActionPanel.PADDING) / CONTENTS_ROW_HEIGHT).coerceAtLeast(1)
        val columns = if (counted.size > rowsThatFit) 2 else 1
        val rowsPerColumn = (counted.size + columns - 1) / columns
        val columnWidth = (width - ActionPanel.PADDING * 2 - Common.UI.SPACING * (columns - 1)) / columns

        val rowsBottom = rowsTop + rowsPerColumn * CONTENTS_ROW_HEIGHT + ActionPanel.PADDING
        if (!draw) return rowsBottom

        val cropIdsWithoutPinnedInfo = GreenhouseData.miscInfo.cropsWithoutInfo
        cropCountRows = counted.mapIndexed { index, (def, count) ->
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
            val nameColor = if (pinnedInfoShown) contentsColor(layout, def) else Common.UI.DISABLED_TEXT_COLOR
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
        val missing = when {
            plan == null -> "No plan to report on"
            tickMs == null -> "Needs your tick time"
            else -> null
        }
        if (missing != null || plan == null || tickMs == null) {
            text(Component.literal(missing ?: ""), innerLeft, Common.UI.TEXT_DIM_COLOR)
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

    private class RunningLayoutReport(val key: Int, val future: java.util.concurrent.CompletableFuture<PlotPrediction.Result>) {
        val result: PlotPrediction.Result? get() = future.getNow(null)
    }

    private var runningLayoutReport: RunningLayoutReport? = null

    private fun layoutReportFor(plan: PlotLayout, tickMs: Long): RunningLayoutReport? {
        val multiplier = BioanalysisAccessory.mutationWeightMultiplier()
        var key = awayTicks * 31 + multiplier.hashCode() + tickMs.hashCode() * 17
        plan.slots.forEach { key = key * 31 + (it.soil?.hashCode() ?: 0) + (it.mark?.ordinal ?: -1) }
        plan.plants.forEach { key = key * 31 + (it.slot.x * 64 + it.slot.y) * 31 + it.cropDef.name.hashCode() }

        runningLayoutReport?.takeIf { it.key == key }?.let { return it }

        runningLayoutReport?.future?.cancel(true)
        // the simulation runs off the render thread, so it gets its own copy
        val planCopy = plan.deepCopy()
        val awayTicksAtStart = awayTicks
        val started = RunningLayoutReport(key, java.util.concurrent.CompletableFuture.supplyAsync { PlotPrediction.simulateVisits(planCopy, awayTicksAtStart, multiplier) })
        runningLayoutReport = started
        return started
    }

    private fun contentsTabClicked(event: MouseButtonEvent): Boolean {
        if (event.button() != 0) return false
        val tab = contentsTabTitleBoxes.entries.firstOrNull { (_, box) -> inRect(event.x, event.y, box[0], box[1], box[2], box[3]) }?.key ?: return false
        contentsTab = tab
        return true
    }

    private val contentsCheckbox = CheckboxWidget(CONTENTS_CHECKBOX_SIZE)

    private fun cropCountRowClicked(event: MouseButtonEvent): Boolean {
        if (event.button() != 0) return false
        val row = cropCountRows.firstOrNull { inRect(event.x, event.y, it.x, it.y, it.width, CONTENTS_ROW_HEIGHT) } ?: return false

        val cropIdsWithoutPinnedInfo = GreenhouseData.miscInfo.cropsWithoutInfo
        if (!cropIdsWithoutPinnedInfo.remove(row.def.elementId)) cropIdsWithoutPinnedInfo.add(row.def.elementId)
        return true
    }

    /** A crop of the list takes the colour of the mark it wears, and plain text when it wears none. */
    private fun contentsColor(layout: PlotLayout, def: CropDefinition): Int {
        val marks = layout.plants.filter { it.cropDef == def }.mapNotNull { it.slot.mark }

        return when {
            LayoutSlot.Marking.Target in marks -> LayoutSlot.Marking.Target.color
            LayoutSlot.Marking.Ingredient in marks -> LayoutSlot.Marking.Ingredient.color
            else -> Common.UI.TEXT_COLOR
        }
    }

    /** A text colour carries no alpha. */
    private fun rgb(color: Int): Int = color and 0xFFFFFF

    /** The next tick box, top left: a button like the delete switch, washed under the mouse and framed bright while pinned. */
    private fun drawTimeBox(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val timeText = "Next tick: " + (GreenhouseData.miscInfo.nextTickTime?.toReadableDuration() ?: "unknown")
        val timeBoxWidth = font.width(timeText) + Common.UI.TEXT_X_PAD * 2
        val timeBoxHeight = boxHeight(timeText)

        timeBox = intArrayOf(TIME_LEFT, TIME_CENTER_Y - timeBoxHeight / 2, TIME_LEFT + timeBoxWidth, TIME_CENTER_Y + timeBoxHeight / 2)
        timeHovered = overTimeBox(mouseX.toDouble(), mouseY.toDouble())

        graphics.drawButtonPanel(timeBox[0], timeBox[1], timeBox[2], timeBox[3], timeHovered, pressed = timePinned)
        graphics.modText(font, Component.literal(timeText), TIME_LEFT + Common.UI.TEXT_X_PAD, TIME_CENTER_Y - font.lineHeight / 2, Common.UI.TEXT_COLOR)
    }

    private fun overTimeBox(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, timeBox[0], timeBox[1], timeBox[2] - timeBox[0], timeBox[3] - timeBox[1])

    /** What greenhouse mode draws besides the grid: the pinned fact, the scroll hint and the Unplan button. */
    private fun renderGreenhouseMode(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // read here rather than only on mouse movement: a pick is a click, and a click is
        // not a movement, so the plants kept showing the last fact
        displayedGridWidget?.pinnedInfo = HoverControls.selectedInfo
        displayedGridWidget?.cropIdsWithoutPinnedInfo = GreenhouseData.miscInfo.cropsWithoutInfo

        // at the right end of the bookmark strip, above the frame, ending where the frame ends
        scrollHint.tooltip = SCROLL_HINT_GREENHOUSES
        scrollHint.layoutAt(
            startX + containerSize + BORDER_PADDING - ScrollHint.SIZE,
            startY - BORDER_PADDING - Bookmarks.THICKNESS,
            Bookmarks.THICKNESS
        )

        // only where there is a plan to stop, since a button that does nothing is a question the
        // player has to answer every time they look at the screen
        greenhousePanel.showButtons = currentDisplay == CurrentDisplay.Greenhouses && displayedGrid() != null
        greenhousePanel.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    /** What preset mode draws besides the grid: the selector, the plants shelf, the drop target and the preset buttons. */
    private fun renderPresetMode(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        gridSelector.extractRenderState(graphics, mouseX, mouseY, delta)
        plantPalette.render(graphics, mouseX, mouseY, delta)
        renderDropTarget(graphics)

        // placed every frame, since the selector is refitted whenever its list changes
        scrollHint.tooltip = SCROLL_HINT_PRESETS
        scrollHint.layoutBeside(gridSelector.x + gridSelector.width, gridSelector.y, gridSelector.height)

        presetUI.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    /** The event in layout units. */
    private fun scaled(event: MouseButtonEvent): MouseButtonEvent = event.at(event.x / drawScale, event.y / drawScale)

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseButtonEvent = scaled(event)

        if (overlaysMouseClicked(mouseButtonEvent, doubled)) return true

        // asked before the sweep below, which had already shut this widget's list: it found it closed
        // and opened it again, so a second click never collapsed anything
        if (currentDisplay == CurrentDisplay.Presets && gridSelector.mouseClicked(mouseButtonEvent, doubled)) {
            return true
        }

        // the click landed outside every overlay, which is what closes them
        closeOverlays()

        // the next tick box pins its breakdown, and unpins it
        if (mouseButtonEvent.button() == 0 && overTimeBox(mouseButtonEvent.x, mouseButtonEvent.y)) {
            timePinned = !timePinned
            return true
        }

        if (timePinned && overClockLine(mouseButtonEvent.x.toInt(), mouseButtonEvent.y.toInt(), ATTRIBUTE_LINE)) {
            ScreenUtil.openChatThenReturn("${MainInternal.COMMAND} ${SetTimestalkAttribute.NAME} ", this)
            return true
        }

        if (unplannedLineClicked(mouseButtonEvent)) return true
        if (contentsTabClicked(mouseButtonEvent)) return true
        if (contentsTab == ContentsTab.Report && awayTicksSlider.mouseClicked(mouseButtonEvent.x, mouseButtonEvent.y)) return true

        val handled = when (currentDisplay) {
            CurrentDisplay.Greenhouses -> greenhouseClicked(mouseButtonEvent, doubled)
            CurrentDisplay.Presets -> presetClicked(mouseButtonEvent, doubled)
        }
        if (handled) return true

        // a right click on the name at the top renames what is shown: the preset, or the greenhouse
        if (mouseButtonEvent.button() == 1 && overName(mouseButtonEvent.x, mouseButtonEvent.y)) {
            when (currentDisplay) {
                CurrentDisplay.Presets -> GreenhouseData.currentPreset?.let { master ->
                    openRenameContext(mouseButtonEvent, master.displayName()) { name -> master.name = name }
                }
                CurrentDisplay.Greenhouses -> displayedGridWidget?.layout?.let { layout ->
                    openRenameContext(mouseButtonEvent, layout.displayName()) { name -> layout.name = name }
                }
            }
            return true
        }

        if (predictShelfHeight > 0 &&
            predictSlider.mouseClicked(mouseButtonEvent.x, mouseButtonEvent.y)
        ) return true

        if (cropPreviewButton.mouseClicked(mouseButtonEvent, doubled)) {
            ScreenUtil.setScreen(CropPreviewScreen(this))
            return true
        }

        if (plotsButton.mouseClicked(mouseButtonEvent, doubled)) {
            showDisplay(CurrentDisplay.Greenhouses)
            return true
        }
        if (presetsButton.mouseClicked(mouseButtonEvent, doubled)) {
            showDisplay(CurrentDisplay.Presets)
            return true
        }

        if (displayedGridWidget?.mouseClicked(mouseButtonEvent, doubled) == true) {
            return true
        }
        return super.onMouseClicked(mouseButtonEvent, doubled)
    }

    private fun highlightPlant(instance: Plant): Boolean {
        val grid = GreenhouseData.getCurrentGrid() ?: return false
        if (grid !== displayedGrid()) return false

        val scannedPlant = grid.scannedPlants.firstOrNull { it.plant === instance } ?: return false

        if (PlantHighlight.showPlant(scannedPlant)) onClose()
        return true
    }

    /** The clicks greenhouse mode takes: the Unplan button, the bookmarks and the swatches. */
    private fun greenhouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == 0) {
            val clicked = displayedGridWidget?.elementAtPos(event.x, event.y)
            if (clicked != null && highlightPlant(clicked)) return true
        }

        if (greenhousePanel.mouseClicked(event, doubled)) return true
        if (cropCountRowClicked(event)) return true

        if (displayedGridWidget != null) {
            if (plotTabs.mouseClicked(event)) return true
            if (teleportTab.mouseClicked(event)) return true
        }

        return hoverControls.mouseClicked(event)
    }

    private fun presetClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (displayedGridWidget != null && partTabs.mouseClicked(event)) return true

        // a right click puts whatever tool is held down, wherever the mouse is
        if (event.button() == 1 && plantPalette.holdsTool) {
            plantPalette.clearTools()
            return true
        }

        if (event.button() == 2 && pickFromCell(event)) return true

        if (plantPalette.mouseClicked(event, doubled)) return true

        // a picked plant lands on the slot clicked
        plantPalette.selected?.let { picked ->
            val cell = (displayedGridWidget ?: emptyGridWidget)?.slotAt(event.x, event.y)
            if (event.button() == 0 && cell != null) {
                placeDragged(picked, event.x, event.y)
                lastPainted = cell
                return true
            }
        }

        if (event.button() == 0) {
            displayedGridWidget?.slotAt(event.x, event.y)?.let { (sx, sy) ->
                // with the switch on, a click empties the cell: the plant on it and the soil under it
                if (plantPalette.deleteMode) {
                    lastPainted = sx to sy
                    return clearCell(sx, sy)
                }
                // with the mark selector on something, a click marks the plant covering the cell
                val choice = plantPalette.markChoice
                if (choice.applies) {
                    lastPainted = sx to sy
                    return markCell(sx, sy, choice.marking)
                }
            }
        }

        // a right click on a plant says what it stands for in the plan
        if (event.button() == 1) {
            (displayedGridWidget?.hoveredElement as? ElementWidget)?.let {
                openMarkContext(it.instance, event)
                return true
            }
        }

        // the preset ui is only laid out in preset mode, off screen its buttons still sit at 0,0
        // and would take clicks meant for the corner of the screen
        return presetUI.mouseClicked(event, doubled)
    }

    override fun onMouseMoved(mouseX: Double, mouseY: Double) {
        val scaledX = mouseX / drawScale
        val scaledY = mouseY / drawScale

        overlaysMouseMoved(scaledX, scaledY)
        hoveredElement = overlays.firstNotNullOfOrNull { overlay ->
            overlay.hoveredElement ?: overlay.takeIf { it.isMouseOver(scaledX, scaledY) }
        }

        // an open list covers what is under it, so nothing under it is told the mouse is there:
        // told, a plant would light up and put its tooltip over the list
        val underOverlay = hoveredElement != null
        val widgetX = if (underOverlay) OFF_SCREEN else scaledX
        val widgetY = if (underOverlay) OFF_SCREEN else scaledY

        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> {
                hoverControls.mouseMoved(widgetX, widgetY)
                plotTabs.mouseMoved(widgetX, widgetY)
                teleportTab.mouseMoved(widgetX, widgetY)
                greenhousePanel.mouseMoved(widgetX, widgetY)
            }
            CurrentDisplay.Presets -> {
                plantPalette.mouseMoved(widgetX, widgetY)
                partTabs.mouseMoved(widgetX, widgetY)
                presetUI.mouseMoved(widgetX, widgetY)
            }
        }

        displayedGridWidget?.mouseMoved(widgetX, widgetY)
        plotsButton.mouseMoved(widgetX, widgetY)
        presetsButton.mouseMoved(widgetX, widgetY)
        cropPreviewButton.mouseMoved(widgetX, widgetY)

        hoveredElement = hoveredElement
            ?: displayedGridWidget?.hoveredElement
            ?: plotsButton.takeIf { it.isMouseOver(scaledX, scaledY) }
            ?: presetsButton.takeIf { it.isMouseOver(scaledX, scaledY) }
            ?: presetUI.hoveredElement.takeIf { currentDisplay == CurrentDisplay.Presets }

        hoverWarning = overName(scaledX, scaledY) && shouldWarn
    }

    override fun onCharTyped(event: CharacterEvent): Boolean {
        if (overlaysCharTyped(event)) return true
        if (currentDisplay == CurrentDisplay.Presets && plantPalette.charTyped(event)) return true
        return super.onCharTyped(event)
    }

    override fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (predictSlider.mouseDragged(event.x / drawScale)) return true
        if (awayTicksSlider.mouseDragged(event.x / drawScale)) return true
        if (currentDisplay == CurrentDisplay.Presets) {
            // a plant dragged off the shelf comes first; otherwise the stroke paints the grid
            if (plantPalette.mouseDragged(scaled(event), dragX, dragY)) return true
            if (event.button() == 0 && paintUnderMouse(scaled(event))) return true
        }
        return super.onMouseDragged(event, dragX, dragY)
    }

    override fun onMouseReleased(event: MouseButtonEvent): Boolean {
        lastPainted = null
        predictSlider.mouseReleased()
        awayTicksSlider.mouseReleased()
        plantPalette.mouseReleased()?.let { placeDragged(it, event.x / drawScale, event.y / drawScale) }
        return super.onMouseReleased(event)
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val scaledX = mouseX / drawScale
        val scaledY = mouseY / drawScale

        // an open list takes the wheel before the screen's own scrolling does
        if (overlaysMouseScrolled(scaledX, scaledY, scrollX, scrollY)) return true

        if (scrollY == 0.0) return super.onMouseScrolled(scaledX, scaledY, scrollX, scrollY)
        if (currentDisplay == CurrentDisplay.Presets && plantPalette.mouseScrolled(scaledX, scaledY, scrollX, scrollY)) return true

        // the wheel walks the swatches in greenhouse mode and the presets in preset mode
        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> hoverControls.cycle(down = scrollY < 0)
            CurrentDisplay.Presets -> cycleDisplayedGrid(forward = scrollY < 0)
        }

        return true
    }

    /** Steps to the neighbouring greenhouse, or the neighbouring preset, wrapping. */
    private fun cycleDisplayedGrid(forward: Boolean) {
        val step = if (forward) 1 else -1
        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> {
                if (greenhouseGridWidgets.isEmpty()) return
                val index = greenhouseGridWidgets.indexOf(displayedGridWidget)
                gridWidgetChanged(greenhouseGridWidgets[Math.floorMod(index + step, greenhouseGridWidgets.size)].layout)
            }
            CurrentDisplay.Presets -> {
                val presets = GreenhouseData.presetGrids
                if (presets.isEmpty()) return
                val index = presets.indexOf(GreenhouseData.currentPreset)
                presetChanged(presets[Math.floorMod(index + step, presets.size)])
            }
        }
    }

    /** Shows a preset from its first plot; apply, export and delete all read the current preset. */
    private fun presetChanged(master: GreenhouseLayout) {
        GreenhouseData.currentPreset = master
        shownPlot = null
        presetCleared = false
        initPresetLayout()
    }

    /**
     * Puts an imported layout into the preset on show whenever it can.
     *
     * A soft import of one plot is laid over the plot on show, at whichever turn and position
     * destroys the least. Otherwise the incoming plots that hold anything go into the preset's empty
     * plots: plots with every slot unset, then plots not yet added, the one on show first. When there
     * are not enough of those, one plot replaces the plot on show and several become a new preset.
     * A plot laid into nothing is turned so its top points the way the player faces.
     */
    private fun imported(result: LayoutTransferResult.Imported, soft: Boolean) {
        val master = GreenhouseData.currentPreset
        val shown = displayedGridWidget?.layout
        val facing = facingTurns()
        val incoming = result.plots.filterNot { it.isEmpty() }.ifEmpty { result.plots.take(1) }

        if (master == null || shown == null) {
            importAsNewPreset(result, facing)
            return
        }
        if (master.name == null) master.name = result.nameForPreset

        if (soft && incoming.size == 1 && !shown.isEmpty()) {
            remember(shown)
            val fit = SoftImport.bestFit(shown, incoming.first(), facing)
            shown.copyContentsFrom(fit.layout)
            initPresetLayout()
            ChatUtils.sendWithPrefix("Imported into ${GreenhouseData.fullPlotName(shown)}")
            ChatUtils.sendWithPrefix(
                "Soft imported at ${fit.turns * 90}°, ${counted(fit.plantsRemoved, "plant")} and ${counted(fit.soilsReplaced, "soil")} replaced"
            )
            return
        }

        val unsetPlots = master.plots.filter { it.isEmpty() }.sortedByDescending { it === shown }
        val roomLeft = unsetPlots.size + (GreenhouseLayout.MAX_PLOTS - master.plots.size)

        when {
            incoming.size <= roomLeft -> {
                val targets = incoming.indices.map { index -> unsetPlots.getOrNull(index) ?: master.addPlot() }
                incoming.zip(targets).forEach { (plot, target) ->
                    remember(target)
                    target.copyContentsFrom(plot.turnedBy(facing))
                    if (target.name == null && incoming.size > 1) target.name = plot.name
                }
                shownPlot = targets.first()
                initPresetLayout()
                val into = if (targets.size == 1) GreenhouseData.fullPlotName(targets.first()) else master.displayName()
                ChatUtils.sendWithPrefix("Imported into $into")
            }
            incoming.size == 1 -> {
                remember(shown)
                shown.copyContentsFrom(incoming.first().turnedBy(facing))
                initPresetLayout()
                ChatUtils.sendWithPrefix("Imported into ${GreenhouseData.fullPlotName(shown)}")
            }
            else -> importAsNewPreset(result, facing)
        }
    }

    private fun importAsNewPreset(result: LayoutTransferResult.Imported, facing: Int) {
        val preset = GreenhouseLayout(id = result.layout.id, name = result.nameForPreset)
        result.plots.forEachIndexed { index, plot ->
            PlotLayout(id = preset.plotId(index), name = plot.name.takeIf { index > 0 || result.plots.size > 1 })
                .also { it.copyContentsFrom(plot.turnedBy(facing)) }
                .let(preset.plots::add)
        }
        addPresetLayout(preset)
    }

    private fun counted(count: Int, noun: String): String = "$count $noun" + if (count == 1) "" else "s"

    /** Quarter turns clockwise that point a plot's top the way the player faces. */
    private fun facingTurns(): Int = when (Minecraft.getInstance().player?.direction) {
        Direction.EAST -> 1
        Direction.SOUTH -> 2
        Direction.WEST -> 3
        else -> 0
    }

    override fun onKeyPressed(event: KeyEvent): Boolean {
        if (overlaysKeyPressed(event)) return true
        if (currentDisplay == CurrentDisplay.Presets && plantPalette.keyPressed(event)) return true
        return super.onKeyPressed(event)
    }

    /** The rename panel at the mouse; [apply] writes the name, then everything sized from names relays out. */
    private fun openRenameContext(buttonEvent: MouseButtonEvent, currentName: String, apply: (String) -> Unit) {
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            buttonEvent.x.toInt(),
            buttonEvent.y.toInt(),
            EditLayoutContextMenu.WIDTH,
            EditLayoutContextMenu.HEIGHT
        )

        val menu = EditLayoutContextMenu(menuX, menuY, currentName, this) { name ->
            apply(name)
            when (currentDisplay) {
                CurrentDisplay.Presets -> initPresetLayout()
                CurrentDisplay.Greenhouses -> {
                    displayedName = displayedGridWidget?.layout?.displayName() ?: displayedName
                    layoutName()
                }
            }
        }
        addContext(menu)
    }

    /** Switches to a mode, doing nothing when it is the one already showing. */
    private fun showDisplay(display: CurrentDisplay) {
        if (display == currentDisplay) return

        currentDisplay = display
        relayoutShelves()
        when (display) {
            CurrentDisplay.Greenhouses -> initGreenhouseLayout()
            CurrentDisplay.Presets -> initPresetLayout()
        }
    }

    /** Measures the shelves again and puts every mode's widgets back where the new measurements say. */
    private fun relayoutShelves() {
        layoutShelves()
        layoutGreenhouseWidgets()
        layoutPresetWidgets()
    }

    /** Redraws the greenhouse on screen as it would stand after that many more growth ticks. */
    private fun showPrediction(ticks: Int) {
        val real = predictionBase ?: displayedGridWidget ?: return

        if (ticks <= 0) {
            displayedGridWidget = real
            predictionBase = null
            return
        }

        val grid = GreenhouseData.greenhouseGrids.firstOrNull { it.layout === real.layout } ?: return

        predictionBase = real
        displayedGridWidget = newGridWidget(grid.predictedLayout(ticks), gridTurns())
    }

    /** Puts the real greenhouse back on screen, for when the screen moves to another one. */
    private fun dropPrediction() {
        predictSlider.set(0)
        predictionBase = null
    }

    /** What the slider reads beside it: the plot as it stands, or how far ahead it is being shown. */
    private fun predictLabel(): String =
        if (predictSlider.value == 0) PREDICT_NOW else "+${predictSlider.value}"

    /**
     * When the plot looks the way the slider shows it, as the chorus setting puts its absence: from
     * the tick that many ahead landing until the one after it, counted from the tick already running.
     */
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

    /** The same window on the player's own clock, as "5:52pm - 7:40pm", with the day named when it is not today. */
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

    /** Shows the greenhouse with [layout], which also becomes the current greenhouse for the rest of the mod. */
    private fun gridWidgetChanged(layout: PlotLayout) {
        dropPrediction()
        val widget = greenhouseGridWidgets.find { it.layout == layout } ?: return

        GreenhouseData.greenhouseGrids
            .indexOfFirst { it.layout === widget.layout }
            .takeIf { it >= 0 }
            ?.let { GreenhouseData.currentGridIndex = it }

        displayedGridWidget = widget
        plotTabs.selected = widget.layout
        presetCleared = false
        displayedName = widget.layout.displayName()

        layoutName()
    }

    /** Whether the greenhouse on screen has a plan running, which is what the button is for. */
    /** The preset plots to choose between, each named by its preset and its place in it. */
    private class PlotChoice(val plot: PlotLayout) {
        override fun toString(): String = GreenhouseData.nameInFull(plot)
    }

    /** Asks which preset plot this greenhouse should run, and hands the answer to the planner. */
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

    /** Lays the assigned plan a quarter turn further round on this greenhouse, for a wrong auto fit. */
    private fun turnPlan() {
        val grid = displayedGrid() ?: return
        if (grid.state.assignedLayout == null) return

        grid.state.planTurns = Math.floorMod(grid.state.planTurns + 1, 4)
        GreenhouseData.regenRender()

        // how well each turn fits what stands, so a plan that does not line up can be turned to
        val plan = grid.state.assignedLayout ?: return
        val fits = (0 until 4).joinToString("; ") { turns -> "${turns * 90}°: ${grid.configurationForLayout(plan.turnedBy(turns))}" }
        ChatUtils.sendWithPrefix("Plan turned to ${grid.state.planTurns * 90}°. In place per turn - $fits")
    }

    /** Shows the plot this greenhouse runs in preset mode, so it can be changed. */
    private fun editAssignedPreset() {
        val plot = displayedGrid()?.state?.assignedLayout ?: return
        val master = GreenhouseData.greenhouseLayoutFor(plot) ?: return

        GreenhouseData.currentPreset = master
        shownPlot = plot
        presetCleared = false
        showDisplay(CurrentDisplay.Presets)
    }

    /**
     * Keeps what stands in this greenhouse as a preset of its own. Empty slots are left unsaid
     * rather than saved as air, which a plan would then demand.
     */
    private fun saveGreenhouseAsPreset() {
        val grid = displayedGrid() ?: return
        val master = GreenhouseLayout.create(GreenhouseData.computeNextAvailableId())
        val plot = master.plots.first()

        plot.copyContentsFrom(grid.layout)
        plot.slots.forEach { slot -> if (slot.soil == Blocks.AIR) slot.soil = null }

        presetCleared = false
        addPresetLayout(master)
        showDisplay(CurrentDisplay.Presets)

        ChatUtils.sendWithPrefix("Saved ${grid.layout.displayName()} as ${master.displayName()}")
    }

    /** The greenhouse on screen is whichever the selector shows, not the one being stood in. */
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
        // turned the way it best fits what is already built
        grid.state.planTurns = grid.bestRotationFor(layout)
        grid.state.buildAnnounced = false
        PlannerNeeds.forgetSentMessage(grid)
        GreenhouseData.regenRender()

        ChatUtils.sendWithPrefix(
            "Planner active on ${grid.layout.displayName()} for ${GreenhouseData.fullPlotName(layout)}"
        )
    }

    /** Gives the current preset one more plot, shown at once. */
    private fun addPlot() {
        val master = GreenhouseData.currentPreset ?: return
        if (master.plots.size >= GreenhouseLayout.MAX_PLOTS) {
            ChatUtils.sendWithPrefix("A preset holds at most ${GreenhouseLayout.MAX_PLOTS} plots, one a greenhouse.")
            return
        }
        shownPlot = master.addPlot()
        initPresetLayout()
    }

    private fun addPresetLayout(master: GreenhouseLayout) {
        GreenhouseData.presetGrids.add(master)
        GreenhouseData.currentPreset = master
        shownPlot = null
        presetCleared = false
        initPresetLayout()
    }

    /**
     * Takes [plot] off the current preset, or with null the whole preset; the last plot takes the
     * preset with it. The preset after the removed one in the list is shown next, else the one before.
     */
    private fun removePresetLayout(plot: PlotLayout?) {
        val master = GreenhouseData.currentPreset
        if (master != null && plot != null && master.plots.size > 1) {
            master.plots.remove(plot)
            stopPlannersOn(plot)
            shownPlot = null
            initPresetLayout()
            return
        }

        val current = GreenhouseData.currentPreset ?: run {
            ChatUtils.sendWithPrefix("No preset to remove.")
            return
        }
        current.plots.forEach { stopPlannersOn(it) }
        val presets = GreenhouseData.presetGrids
        val index = presets.indexOf(current)
        presets.remove(current)
        GreenhouseData.currentPreset = presets.getOrNull(index) ?: presets.lastOrNull()

        initPresetLayout()
    }

    companion object {
        private var lastDisplay: CurrentDisplay = CurrentDisplay.Greenhouses
        /** The "not all greenhouses available" warning is sent at most once per game run. */
        private var warnedMissingGreenhouses: Boolean = false

        /** Enough for one button, however narrow the window gets. */
        private const val MIN_ACTION_ROW_WIDTH: Int = 90

        /** What the toolbar down the left of the grid needs, so the grid never sits on top of it. */
        private const val TOOLBAR_WIDTH: Int = 180

        /** Below this the item art rounds away to nothing, so the grid stops shrinking instead. */
        private const val MIN_SLOT_SIZE: Int = 8

        /** The room between the grid and its frame. */
        private const val BORDER_PADDING: Int = 6

        /** The units the panels and grid need; a smaller window is drawn at the first scale that gives them. */
        private const val COMFORTABLE_WIDTH: Int = 800
        private const val COMFORTABLE_HEIGHT: Int = 400

        /** Full size, then gui scale 3 at 1080p, then gui scale 4 at 1080p. */
        private val DRAW_SCALES: List<Float> = listOf(1f, 0.75f, 0.5f)

        /** Where the name box sits from the top of the screen. */
        private const val NAME_TOP: Int = 9

        private const val TELEPORT_WIDTH: Int = 110
        private const val TELEPORT_LABEL: String = "Teleport to Plot"

        /** The mode toggle's two labels. */
        private const val TOGGLE_PLOTS: String = "Plots"
        private const val TOGGLE_PRESETS: String = "Presets"

        /** The layout behind the empty grid, never saved. */
        private const val EMPTY_GRID_ID: String = "preset_none"

        /** The footprint a carried plant would take, seen through. */
        private const val DROP_OK: Int = 0x6000FF00
        private const val DROP_BLOCKED: Int = 0x60FF0000

        /** The next tick box, top left. */
        private const val TIME_LEFT: Int = 10
        private const val TIME_CENTER_Y: Int = 18

        private const val SHELF_VIEW: String = "View"

        /** A point no widget is at, handed to them while an open list has the mouse. */
        private const val OFF_SCREEN: Double = -1.0
        private const val SHELF_PREDICT: String = "Prediction"

        /** What the slider reads at zero, where the plot is shown as it stands. */
        private const val PREDICT_NOW: String = "Now"

        /** Room kept beside the slider for the number of ticks. */
        private const val PREDICT_LABEL_WIDTH: Int = 22

        /** What the window line reads while the tick clock has nothing to count from. */
        private const val PREDICT_WINDOW_UNKNOWN: String = "tick time unknown"

        private val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
        private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

        /** The furthest ahead the slider looks. */
        private const val MAX_PREDICT_TICKS: Int = 10
        private const val SHELF_GREENHOUSE: String = "Greenhouse"
        private const val MAX_AWAY_TICKS: Int = 20
        private const val WATER_LASTS_TOOLTIP: String =
            "How many ticks one full watering lasts before the first plant runs out. " +
                    "A plant out of water can skip growth ticks, and dies if it stays out too long."

        private var contentsTab: ContentsTab = ContentsTab.Contents
        private var awayTicks: Int = 3
        private const val UNPLANNED_LINE_HEIGHT: Int = 12
        private const val UNPLANNED_NONE: String = "Nothing can grow unplanned"
        private const val CONTENTS_ICON_SIZE: Int = 12
        private const val CONTENTS_ROW_HEIGHT: Int = CONTENTS_ICON_SIZE + 2
        private const val CONTENTS_CHECKBOX_SIZE: Int = 9
        private const val SHELF_PRESET: String = "Preset"

        /** What each part of the tick period runs up to. */
        private const val MAX_UNIQUE_CROPS: Int = 12

        /** Which line of the clock breakdown is the unique crops one, counted from the tick time line. */
        private const val UNIQUE_LINE: Int = 1
        private const val ATTRIBUTE_LINE: Int = 3

        private const val SET_ATTRIBUTE_HINT: String = "Click to open chat to set your attribute level"
        private const val MAX_SPEED_UPGRADE: Int = 9
        private const val MAX_ATTRIBUTE: Int = 10

        private const val RENAME_HINT: String = "\nRight click a name to rename it: greenhouses, presets and plots"
        private const val SCROLL_HINT_GREENHOUSES: String = "Scroll the mouse wheel to switch what the plants show$RENAME_HINT"
        private const val SCROLL_HINT_PRESETS: String = "Scroll the mouse wheel to switch preset$RENAME_HINT"

        /** How many actions the arrows can walk back. */
        private const val HISTORY_LIMIT: Int = 50
    }
}

private enum class ContentsTab(val label: String) {
    Contents("Contents"),
    Report("Report");

    companion object {
        val shown: List<ContentsTab> = listOf(Contents)
    }
}
