package org.magic.magicaddons.ui.screens

import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.transfer.LayoutTransferResult
import org.magic.magicaddons.data.greenhouse.MasterLayout
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.toReadableDuration
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.greenhouse.EditLayoutContextMenu
import org.magic.magicaddons.ui.widgets.EnumWidget
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.greenhouse.ElementWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GridWidget
import org.magic.magicaddons.ui.widgets.greenhouse.ActionPanel
import org.magic.magicaddons.ui.widgets.greenhouse.Bookmarks
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.ui.widgets.greenhouse.MarkContext
import org.magic.magicaddons.ui.widgets.greenhouse.PlantPalette
import org.magic.magicaddons.ui.widgets.greenhouse.HoverControls
import org.magic.magicaddons.ui.widgets.greenhouse.ScrollHint
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhousePanel
import org.magic.magicaddons.ui.widgets.greenhouse.PresetUI
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.boxHeight
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.drawWarningBadge
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import org.magic.magicaddons.util.compat.McCompat

class GreenhouseScreen(title: Component) : Screen(title), HoverableContainer, OverlayContext {

    enum class CurrentDisplay {
        Greenhouses,
        Presets
    }

    private val gridSize = 10

    private var paddingY: Int = 40
    private var startX: Int = 0
    private var startY: Int = 0
    private var containerSize: Int = 400



    //todo preset buttons: add crop (searchable list of every crop), remove crop, render highlight
    //todo custom layers as well as the base ones. For now ingredients cancel break, target does not
    //todo warn in the base feature when not all unique crops are detected
    //todo infer stages between ticks, and mark an inferred board yellow rather than showing it as read

    var currentDisplay = CurrentDisplay.Greenhouses

    /** Whether the greenhouse on screen is running on guessed growth. Read while drawing, since the
     * tick that makes it stale can land with the screen open. */
    private val shouldWarn: Boolean
        get() = currentDisplay == CurrentDisplay.Greenhouses &&
                (GreenhouseData.greenhouseGrids.getOrNull(GreenhouseData.currentGridIndex)
                    ?.state?.pendingGrowthTicks ?: 0) > 0
    var borderPadding: Int = 6

    /** 0.5 when the window has too few gui units for the panels, so everything is drawn half size. */
    private var drawScale: Float = 1f

    override var hoveredElement: GuiEventListener? = null

    override val overlays = mutableListOf<OverlayRenderable>()
    private var displayedGridWidget: GridWidget? = null
    private val greenhouseGridWidgets: MutableList<GridWidget> = mutableListOf()
    private val presetGridWidgets: MutableList<GridWidget> = mutableListOf()
    private val currentDisplayToggle = ClickableButtonWidget(
        60,
        26,
        Component.literal("Plots")
    )

    /** What the player can do to the greenhouse on screen, the other end of the Planner button. */
    private val greenhousePanel = GreenhousePanel(
        onUnplan = {
            displayedGrid()?.let { GreenhouseData.unplanGreenhouse(it) }
            initGreenhouseLayout()
        }
    )

    /** Where a mode's own buttons begin, shared so the two modes line up with each other. */
    private var actionRowX: Int = 0
    private var actionRowY: Int = 0

    /** The two shelves down the left: what is shown, and what can be done to it. */
    private var shelfLeft: Int = 0
    private var shelfWidth: Int = 0
    private var viewShelfY: Int = 0
    private var viewShelfHeight: Int = 0
    private var actionShelfY: Int = 0

    /** Whether the mouse is on the next tick box, which then explains the clock. */
    private var timeHovered = false

    private var dynamicNameDisplay: ClickableButtonWidget? = null
    private var hoverWarning = false

    private val gridSelector = EnumWidget(
        values = emptyList<MasterLayout>(),
        currentValue = null as MasterLayout?,
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
        onImported = { imported(it) },
        onRemove = { removePresetLayout(it) },
        shownLayout = { displayedGridWidget?.layout }
    )



    var displayedName: String = "Error loading name."

    var slotSize: Int = 20

    private val hoverControls = HoverControls()

    /** Sits beside the selector to say the wheel turns it too. */
    private val scrollHint = ScrollHint(SCROLL_HINT_GREENHOUSES)

    /** The greenhouses as bookmarks along the top of the grid; a right click renames one. */
    private val plotTabs = Bookmarks<GreenhouseLayout>(
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

    /** One bookmark under the frame that walks the player to the plot on screen. */
    private val teleportTab = Bookmarks<String>(
        side = Bookmarks.Side.Bottom,
        label = { it },
        onPick = { _, _ ->
            displayedGridWidget?.layout?.id?.removePrefix("plot_")?.let { ChatUtils.sendCommand("tptoplot $it") }
        }
    ).apply { items = listOf(TELEPORT_LABEL) }

    /** The plants a preset can be built from, under the preset shelf. */
    private val plantPalette = PlantPalette(this, onClearAll = { clearCanvas() }, onUndo = { undo() }, onRedo = { redo() })

    /** A bookmark along the top of a preset: one of its plots, or the one that adds a plot. */
    private sealed interface PartTab {
        data class Part(val layout: GreenhouseLayout, val title: String) : PartTab
        data object Add : PartTab
    }

    /** Which plot of the current preset is on show; null for its first. */
    private var shownPlot: GreenhouseLayout? = null

    /** The plots of the preset as bookmarks along the top, with a + at the end to add one. */
    private val partTabs = Bookmarks<PartTab>(
        side = Bookmarks.Side.Top,
        label = { tab -> if (tab is PartTab.Part) tab.title else "+" },
        tooltip = { tab -> if (tab is PartTab.Part) "Right click to rename" else "Add a plot to this preset" },
        onPick = { tab, event ->
            when {
                tab is PartTab.Add -> addPlot()
                tab is PartTab.Part && event.button() == 1 ->
                    openRenameContext(event, tab.title) { name -> tab.layout.name = name }
                tab is PartTab.Part -> {
                    shownPlot = tab.layout
                    initPresetLayout()
                }
            }
        }
    )

    /** Grid lines with nothing on them, shown while there is no preset, so a plant has somewhere to land. */
    private var emptyGridWidget: GridWidget? = null

    /** Whether the preset canvas was cleared: an empty grid with nothing picked, until a plant lands. */
    private var presetCleared = false

    private val cropPreviewButton = ClickableButtonWidget(
        100,
        22,
        Component.literal("Crop Preview")
    )


    override fun init() {
        super.init()

        // a cramped window is laid out as if it had more units, then drawn smaller to fit them in
        drawScale = DRAW_SCALES.first { it == DRAW_SCALES.last() ||
                width / it >= COMFORTABLE_WIDTH && height / it >= COMFORTABLE_HEIGHT }
        width = (width / drawScale).toInt()
        height = (height / drawScale).toInt()

        initBaseLayout()

        // opening this screen is the player asking about their greenhouses, which is the one moment a
        // missing number is worth interrupting them for
        if (!GreenhouseData.miscInfo.shouldIgnoreWarning) {
            GreenhouseData.warnUnknownValues()
        }
    }
    fun initBaseLayout(){

        paddingY = height / 10

        // the name box and the bookmarks sit above the grid, the teleport and preview buttons under
        // it, and the toolbar down the left and the bookmarks plus a tooltip down the right beside
        // it, so the grid takes whichever axis runs out first
        startY = maxOf(paddingY, NAME_TOP + boxHeight(" ") + Common.UI.SPACING + Bookmarks.REACH + borderPadding)
        val bottomRoom = Bookmarks.REACH + Common.UI.SPACING + cropPreviewButton.height + Common.UI.SPACING_LARGE

        val sideRoom = TOOLBAR_WIDTH + HoverControls.TOTAL_WIDTH + Common.UI.SPACING_LARGE * 2
        val heightRoom = height - startY - borderPadding * 2 - bottomRoom
        val widthRoom = width - sideRoom - borderPadding * 2

        // the room decides the slot and the slot decides everything else, so the division
        // happens once and every other measurement is counted up from its answer
        slotSize = GridWidget.slotSizeFor(minOf(heightRoom, widthRoom), gridSize)
            .coerceAtLeast(MIN_SLOT_SIZE)

        containerSize = GridWidget.spanFor(slotSize, gridSize)

        // never left of the toolbar, however narrow the window gets
        startX = ((width - containerSize) / 2).coerceAtLeast(TOOLBAR_WIDTH + Common.UI.SPACING_LARGE)

        plotTabs.layoutAlong(startX - borderPadding, startY - borderPadding, containerSize + borderPadding * 2)
        partTabs.layoutAlong(startX - borderPadding, startY - borderPadding, containerSize + borderPadding * 2)

        // hung off the bottom of the frame, centred
        teleportTab.layoutAlong(
            startX + (containerSize - TELEPORT_WIDTH) / 2,
            startY + containerSize + borderPadding,
            TELEPORT_WIDTH
        )

        // the view shelf holds the mode toggle and, for presets, the selector; the action shelf
        // under it holds whatever the mode can do
        shelfLeft = Common.UI.SPACING_LARGE
        shelfWidth = (startX - borderPadding - Common.UI.SPACING_LARGE - shelfLeft).coerceAtLeast(MIN_ACTION_ROW_WIDTH)
        viewShelfY = startY - borderPadding

        currentDisplayToggle.x = shelfLeft + ActionPanel.PADDING
        currentDisplayToggle.y = viewShelfY + shelfTitleHeight() + ActionPanel.PADDING

        gridSelector.x = currentDisplayToggle.x + currentDisplayToggle.width + Common.UI.SPACING
        gridSelector.y = currentDisplayToggle.y
        gridSelector.height = currentDisplayToggle.height
        gridSelector.closeList()

        viewShelfHeight = shelfTitleHeight() + ActionPanel.PADDING * 2 + currentDisplayToggle.height
        actionShelfY = viewShelfY + viewShelfHeight + Common.UI.SPACING_LARGE

        // one row under the title, offered to whichever mode is on screen, so the two put their
        // buttons in the same place without either working out where that is
        actionRowX = shelfLeft
        actionRowY = actionShelfY + shelfTitleHeight()

        val rowWidth = shelfWidth
        val rowHeight = height - actionRowY - Common.UI.SPACING_LARGE

        greenhousePanel.layoutIn(actionRowX, actionRowY, rowWidth, rowHeight)
        presetUI.layoutIn(actionRowX, actionRowY, rowWidth, rowHeight)

        // the plants shelf takes what is left under the preset buttons, ending with the grid's frame
        val paletteY = actionShelfY + shelfTitleHeight() + presetUI.contentHeight + Common.UI.SPACING_LARGE
        val frameBottom = startY + containerSize + borderPadding
        plantPalette.layout(shelfLeft, paletteY, shelfWidth, frameBottom - paletteY)

        emptyGridWidget = GridWidget(GreenhouseLayout(id = EMPTY_GRID_ID), slotSize).apply {
            widgetX = startX
            widgetY = startY
            widgetWidth = containerSize
            widgetHeight = containerSize
            init()
        }

        hoverControls.layoutAgainstGrid(startX + containerSize, startY, containerSize)

        // bottom centre, in the margin the grid already leaves under itself
        cropPreviewButton.x = (width - cropPreviewButton.width) / 2
        cropPreviewButton.y = height - cropPreviewButton.height - Common.UI.SPACING_LARGE - 2
        
        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> {
                initGreenhouseLayout()
            }
            CurrentDisplay.Presets -> {
                initPresetLayout()
            }
        }



    }

    fun initGreenhouseLayout(){
        displayedGridWidget = null
        hoveredElement = null
        greenhouseGridWidgets.clear()
        currentDisplayToggle.message = Component.literal("Plots")
        val amountInitialized = GreenhouseData.greenhouseGrids.count { it.state.lastUpdateTimestamp != null }
        if (PlotAPI.plots.any { it.data == null }) {
            if (!GreenhouseData.miscInfo.shouldIgnoreWarning) {
                if (!LocationAPI.isOnSkyBlock) {
                    ChatUtils.sendWithPrefix("Plot data is null, please join skyblock.")
                } else {
                    ChatUtils.sendWithCommand(
                        "Plot data is null, please open /desk and go to \"configure plots\" to load it.",
                        "/desk"
                    )
                }
            }
            return
        }
        if (amountInitialized != PlotAPI.plots.count { it.data?.isGreenhouse ?: throw IllegalStateException("Plot data was null after null check.") }){
            if (!GreenhouseData.miscInfo.shouldIgnoreWarning && !warnedMissingGreenhouses){
                warnedMissingGreenhouses = true
                ChatUtils.sendWithCommand(
                    "Not all greenhouses available, enter them to see them. (IGNORE)",
                    "/MagicAddons internal ignoreFarmingWarnings"
                )
            }
        }

        GreenhouseData.greenhouseGrids.forEachIndexed { index, grid ->
            if (grid.state.lastUpdateTimestamp == null) return@forEachIndexed
            val gridWidget = GridWidget(grid.layout, slotSize).apply {
                widgetX = startX
                widgetY = startY
                widgetWidth = containerSize
                widgetHeight = containerSize
                init()
            }
            if ("plot_${PlotAPI.getCurrentPlot()?.id}" == grid.layout.id)
                GreenhouseData.currentGridIndex = index
            greenhouseGridWidgets.add(gridWidget)
        }
        // currentGridIndex counts greenhouses, the widget list skips the ones never scanned, so the
        // index has to go through the grid it names rather than straight into the widgets
        val currentLayout = GreenhouseData.greenhouseGrids
            .getOrNull(GreenhouseData.currentGridIndex)?.layout

        displayedGridWidget = greenhouseGridWidgets.find { it.layout === currentLayout }
            ?: greenhouseGridWidgets.firstOrNull()
        // No greenhouse scanned yet: nothing to show, the warning above already covers it.
        if (displayedGridWidget == null) return

        displayedName = displayedGridWidget?.layout?.displayName() ?: "Unknown Plot"




        plotTabs.items = greenhouseGridWidgets.map { it.layout }
        plotTabs.selected = displayedGridWidget?.layout

        initDynamicName()
    }

    fun initPresetLayout(){
        presetGridWidgets.clear()
        displayedGridWidget = null
        hoveredElement = null


        if (GreenhouseData.currentPreset == null && !presetCleared) {
            GreenhouseData.currentPreset = GreenhouseData.presetGrids.firstOrNull()
        }
        val master = GreenhouseData.currentPreset?.takeUnless { presetCleared }
        if (master != null && shownPlot !in master.plots) shownPlot = null

        master?.plots?.forEach { plot ->
            val gridWidget = GridWidget(plot, slotSize).apply {
                widgetX = startX
                widgetY = startY
                widgetWidth = containerSize
                widgetHeight = containerSize
                init()
            }
            presetGridWidgets.add(gridWidget)
        }
        val shown = shownPlot ?: master?.plots?.firstOrNull()
        displayedGridWidget = presetGridWidgets.find { it.layout === shown }

        partTabs.items = if (master == null) emptyList() else buildList {
            master.plots.forEach { add(PartTab.Part(it, master.plotTitle(it))) }
            if (master.plots.size < MasterLayout.MAX_PLOTS) add(PartTab.Add)
        }
        partTabs.selected = shown?.let { PartTab.Part(it, master?.plotTitle(it) ?: "") }

        displayedName = master?.displayName() ?: "Unknown Preset"

        gridSelector.currentValue = master
        gridSelector.values = GreenhouseData.presetGrids.toList()
        relayoutSelector()
        currentDisplayToggle.message = Component.literal("Presets")


        initDynamicName()

    }

    /**
     * Sizes the selector to whatever it lists now, which changes when a layout is renamed. The
     * widget measures itself; the screen only knows how much room there is.
     */
    private fun relayoutSelector() {
        val room = shelfLeft + shelfWidth - ActionPanel.PADDING - Common.UI.SPACING - ScrollHint.SIZE - gridSelector.x
        gridSelector.fitToValues(room)
    }

    fun initDynamicName(){
        val boxHeight = boxHeight(displayedName)
        val widgetWidth = font.width(displayedName) + Common.UI.TEXT_X_PAD * 2
        val screenWidth = width
        val nameTop = NAME_TOP
        dynamicNameDisplay = ClickableButtonWidget(
            widgetWidth + Common.UI.SPACING + boxHeight,
            boxHeight,
            {
                it.drawMultilineBoxCentered(
                    displayedName,
                    screenWidth / 2,
                    nameTop + boxHeight / 2,
                    if (shouldWarn) Common.UI.WARNING_COLOR else null
                )
                // the badge sits beside the name, as tall as its box
                if (shouldWarn) {
                    it.drawWarningBadge(warningBadgeX(), nameTop, boxHeight)
                }
            },
            false
        )
        dynamicNameDisplay?.x = (screenWidth-widgetWidth) / 2
        dynamicNameDisplay?.y = nameTop
    }

    /** A titled panel down the left; its contents are drawn by whoever owns them. */
    private fun drawShelf(graphics: GuiGraphicsExtractor, title: String, top: Int, height: Int) {
        graphics.drawPanel(shelfLeft, top, shelfLeft + shelfWidth, top + height)
        graphics.text(
            font,
            Component.literal(title),
            shelfLeft + Common.UI.TEXT_X_PAD,
            top + Common.UI.SPACING,
            Common.UI.TEXT_DIM_COLOR,
            false
        )
    }

    private fun shelfTitleHeight(): Int = font.lineHeight + Common.UI.SPACING * 2

    /** Everything the tick period is made of, each level coloured by how far along it is. */
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

        val tickTime = GreenhouseData.currentGrowthTickMs()?.let { ms ->
            val seconds = ms / 1000
            "§f%dh %dm %ds".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
        } ?: "§8?"

        return listOf(
            "§7Your tick time: $tickTime",
            "§7Unique crops: " + graded(GreenhouseData.getCurrentUniques().size, MAX_UNIQUE_CROPS),
            "§7Greenhouse speed upgrade: " + graded(misc.cropSpeedUpgradeValue, MAX_SPEED_UPGRADE),
            "§7Greenhouse attribute: " + graded(GreenhouseData.greenhouseSpeedAttribute(), MAX_ATTRIBUTE),
            "§7Crop growth: §f" + (misc.cropGrowthValue?.toString() ?: "§8?")
        ).joinToString("\n")
    }

    /** Where the carried plant would land, green when it fits and red when it cannot. */
    private fun renderDropTarget(graphics: GuiGraphicsExtractor) {
        val def = plantPalette.carried ?: return
        val grid = displayedGridWidget ?: emptyGridWidget ?: return
        val mouse = Minecraft.getInstance().mouseHandler
        val window = Minecraft.getInstance().window
        val mouseX = mouse.xpos() * window.guiScaledWidth / window.screenWidth / drawScale
        val mouseY = mouse.ypos() * window.guiScaledHeight / window.screenHeight / drawScale
        val (sx, sy) = grid.slotAt(mouseX, mouseY) ?: return

        val (x1, y1, x2, y2) = grid.footprintRect(sx, sy, def.footprint)
        val colour = if (canPlace(grid.layout, def, sx, sy)) DROP_OK else DROP_BLOCKED
        graphics.fill(x1, y1, x2, y2, colour)
    }

    /** Whether a plant of that size fits at the slot without leaving the grid or covering another. */
    /** Whether the footprint fits inside the grid; whatever stands there already is replaced. */
    private fun canPlace(layout: GreenhouseLayout, def: CropDefinition, sx: Int, sy: Int): Boolean {
        val footprint = def.footprint
        return sx + footprint.width <= layout.size && sy + footprint.height <= layout.size
    }

    /** Every plant whose footprint shares a slot with the one about to be placed. */
    private fun overlapping(layout: GreenhouseLayout, def: CropDefinition, sx: Int, sy: Int): List<GreenhouseElementInstance> {
        val footprint = def.footprint
        return layout.elementInstances.filter { other ->
            val ow = other.cropDef.footprint.width
            val oh = other.cropDef.footprint.height
            sx < other.slot.x + ow && other.slot.x < sx + footprint.width &&
                    sy < other.slot.y + oh && other.slot.y < sy + footprint.height
        }
    }

    /** Drops the carried plant onto the preset at the mouse, when it fits there. With no preset, one is started. */
    private fun placeDragged(def: CropDefinition, mouseX: Double, mouseY: Double) {
        if (currentDisplay != CurrentDisplay.Presets) return
        if (displayedGridWidget == null) {
            if (emptyGridWidget?.slotAt(mouseX, mouseY) == null) return
            presetCleared = false
            addPresetLayout(MasterLayout.create(GreenhouseData.computeNextAvailableId()))
        }
        val grid = displayedGridWidget ?: return
        val (sx, sy) = grid.slotAt(mouseX, mouseY) ?: return
        if (!canPlace(grid.layout, def, sx, sy)) return
        val slot = grid.layout.getSlot(sx, sy) ?: return

        remember(grid.layout)
        grid.layout.elementInstances.removeAll(overlapping(grid.layout, def, sx, sy))

        // the plant brings the first soil it accepts with it, under every slot it covers
        def.requiredSoil.firstOrNull()?.let { soil ->
            for (dx in 0 until def.footprint.width) {
                for (dy in 0 until def.footprint.height) {
                    grid.layout.getSlot(sx + dx, sy + dy)?.placedBlock = soil.defaultBlockState()
                }
            }
        }

        val instance = GreenhouseElementInstance(def.skyblockId?.id ?: def.name, slot, null, null, cropDef = def)
        grid.layout.elementInstances.add(instance)
        grid.justPlaced.add(instance)
        grid.init()
    }

    /** Lets the player say what a plant in the plan stands for, written onto its slot. */
    private fun openMarkContext(instance: GreenhouseElementInstance, event: MouseButtonEvent) {
        val grid = displayedGridWidget ?: return
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(event.x.toInt(), event.y.toInt(), MARK_MENU_WIDTH, MARK_MENU_HEIGHT)

        val options = MarkContext.Option.entries.filter {
            it != MarkContext.Option.Unique || canBeUnique(grid.layout, instance)
        }
        val menu = MarkContext(menuX, menuY, this, options) { marking -> applyMark(instance, marking) }
        menu.init()
        addContext(menu)
    }

    /** Takes a plant off the preset, for the Delete switch. */
    private fun removeFromPreset(instance: GreenhouseElementInstance) {
        val grid = displayedGridWidget ?: return
        remember(grid.layout)
        grid.layout.elementInstances.remove(instance)
        grid.init()
    }

    /** Writes a mark onto a plant's slot; unique crop only where it would count as one. */
    private fun applyMark(instance: GreenhouseElementInstance, marking: LayoutSlot.Marking?) {
        val grid = displayedGridWidget ?: return
        if (marking == LayoutSlot.Marking.UniqueCrop && !canBeUnique(grid.layout, instance)) {
            ChatUtils.sendWithPrefix("${instance.cropDef.name} would not count as a unique crop here.")
            return
        }
        remember(grid.layout)
        instance.slot.slotMark = marking
        grid.init()
    }

    /**
     * A base crop counts as unique once across the whole preset: another plant of the same kind
     * already marked unique, on any plot, means this one would add nothing. Mutations never count.
     */
    private fun canBeUnique(layout: GreenhouseLayout, instance: GreenhouseElementInstance): Boolean {
        if (!instance.cropDef.isBaseCrop) return false
        val key = GreenhouseData.UniqueCropKey.from(instance.cropDef)
        val plots = GreenhouseData.masterOf(layout)?.plots ?: listOf(layout)
        return plots.flatMap { it.elementInstances }.none {
            it !== instance &&
                    it.slot.slotMark == LayoutSlot.Marking.UniqueCrop &&
                    GreenhouseData.UniqueCropKey.from(it.cropDef) == key
        }
    }

    /**
     * One plot as it stood before an action: placing, replacing, removing a plant or soil, or
     * marking. The arrows walk these back and forward.
     */
    private class PresetSnapshot(
        val layout: GreenhouseLayout,
        val elements: List<GreenhouseElementInstance>,
        val slots: List<Triple<LayoutSlot, BlockState?, LayoutSlot.Marking?>>
    )

    private val undoStack = ArrayDeque<PresetSnapshot>()
    private val redoStack = ArrayDeque<PresetSnapshot>()

    private fun snapshot(layout: GreenhouseLayout) = PresetSnapshot(
        layout,
        layout.elementInstances.toList(),
        layout.slots.map { Triple(it, it.placedBlock, it.slotMark) }
    )

    /** Called before every action; a new action forgets whatever had been undone. */
    private fun remember(layout: GreenhouseLayout) {
        undoStack.addLast(snapshot(layout))
        if (undoStack.size > HISTORY_LIMIT) undoStack.removeFirst()
        redoStack.clear()
    }

    private fun restore(saved: PresetSnapshot) {
        saved.layout.elementInstances.clear()
        saved.layout.elementInstances.addAll(saved.elements)
        saved.slots.forEach { (slot, block, mark) ->
            slot.placedBlock = block
            slot.slotMark = mark
        }
        // the plot may sit on another bookmark, or in another preset, than the one on show
        if (presetGridWidgets.none { it.layout === saved.layout }) {
            GreenhouseData.masterOf(saved.layout)?.let { master ->
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

    /** Puts the preset down and shows an empty grid; the preset itself keeps everything it had. */
    private fun clearCanvas() {
        GreenhouseData.currentPreset = null
        presetCleared = true
        initPresetLayout()
    }

    /** Where the badge beside the name starts, so its tooltip can hang under it. */
    private fun warningBadgeX(): Int =
        (width + font.width(displayedName) + Common.UI.TEXT_X_PAD * 2) / 2 + Common.UI.SPACING

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.pose().pushMatrix()
        graphics.pose().scale(drawScale, drawScale)
        extractScaled(graphics, (mouseX / drawScale).toInt(), (mouseY / drawScale).toInt(), delta)
        graphics.pose().popMatrix()
    }

    /** The whole screen, in layout units. */
    private fun extractScaled(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // the bookmarks first, so the frame drawn next covers where they tuck under it
        if (currentDisplay == CurrentDisplay.Greenhouses && displayedGridWidget != null) {
            plotTabs.render(graphics)
            hoverControls.extractRenderState(graphics, mouseX, mouseY, delta)
            teleportTab.render(graphics)
        }
        if (currentDisplay == CurrentDisplay.Presets && displayedGridWidget != null) {
            partTabs.render(graphics)
        }

        // background
        graphics.drawPanel(
            startX - borderPadding,
            startY - borderPadding,
            startX + containerSize + borderPadding,
            startY + containerSize + borderPadding
        )

        dynamicNameDisplay?.extractRenderState(graphics,mouseX,mouseY,delta)

        val timeText = "Next tick: " + (GreenhouseData.miscInfo.nextTickTime?.toReadableDuration() ?: "unknown")
        val timeBoxWidth = font.width(timeText) + Common.UI.TEXT_X_PAD * 2
        val timeBoxHeight = boxHeight(timeText)

        graphics.drawMultilineBoxCentered(timeText, TIME_LEFT + timeBoxWidth / 2, TIME_CENTER_Y)
        timeHovered = mouseX in TIME_LEFT until TIME_LEFT + timeBoxWidth &&
                mouseY in TIME_CENTER_Y - timeBoxHeight / 2 until TIME_CENTER_Y + timeBoxHeight / 2

        // presets are a plan rather than a greenhouse that exists, no plant in one has a stage,
        // a water level or an age to report
        displayedGridWidget?.extractRenderState(graphics, mouseX, mouseY, delta)
        if (displayedGridWidget == null && currentDisplay == CurrentDisplay.Presets) {
            emptyGridWidget?.extractRenderState(graphics, mouseX, mouseY, delta)
        }

        drawShelf(graphics, SHELF_VIEW, viewShelfY, viewShelfHeight)

        val panel = if (currentDisplay == CurrentDisplay.Greenhouses) greenhousePanel else presetUI
        if (panel.hasShown()) {
            val title = if (currentDisplay == CurrentDisplay.Greenhouses) SHELF_GREENHOUSE else SHELF_PRESET
            drawShelf(graphics, title, actionShelfY, shelfTitleHeight() + panel.contentHeight)
        }

        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> {
                // read here rather than only on mouse movement: a pick is a click, and a click is
                // not a movement, so the plants kept showing the last fact
                displayedGridWidget?.pinnedInfo = hoverControls.selectedInfo

                // at the right end of the bookmark strip, above the frame, ending where the frame ends
                scrollHint.tooltip = SCROLL_HINT_GREENHOUSES
                scrollHint.layoutAt(
                    startX + containerSize + borderPadding - ScrollHint.SIZE,
                    startY - borderPadding - Bookmarks.THICKNESS,
                    Bookmarks.THICKNESS
                )
            }
            CurrentDisplay.Presets -> {
                gridSelector.extractRenderState(graphics, mouseX, mouseY, delta)
                plantPalette.render(graphics, mouseX, mouseY, delta)
                renderDropTarget(graphics)

                // placed every frame, since the selector is refitted whenever its list changes
                scrollHint.tooltip = SCROLL_HINT_PRESETS
                scrollHint.layoutBeside(gridSelector.x + gridSelector.width, gridSelector.y, gridSelector.height)
            }
        }
        scrollHint.extractRenderState(graphics, mouseX, mouseY)

        currentDisplayToggle.extractRenderState(graphics, mouseX, mouseY, delta)
        cropPreviewButton.extractRenderState(graphics, mouseX, mouseY, delta)

        // only where there is a plan to stop, since a button that does nothing is a question the
        // player has to answer every time they look at the screen
        greenhousePanel.showUnplan = plannerRunning()

        if (currentDisplay == CurrentDisplay.Greenhouses) {
            greenhousePanel.extractRenderState(graphics, mouseX, mouseY, delta)
        }

        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> {

            }
            CurrentDisplay.Presets -> {
                presetUI.extractRenderState(graphics, mouseX, mouseY, delta)
            }
        }
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

        overlays.asReversed().forEach {
            it.renderOverlay(graphics, mouseX, mouseY, delta)
        }

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

        if (timeHovered) {
            graphics.drawSimpleTooltip(clockTooltip(), TIME_LEFT, TIME_CENTER_Y + boxHeight(" ") / 2 + Common.UI.SPACING)
        }

        val hovered = hoveredElement
        if (hovered !is ElementWidget) return

        // hovering the star beside a water time shows the star's own tooltip instead of the plant's
        hovered.deadTooltipAt(mouseX, mouseY)?.let {
            graphics.drawSimpleTooltip(it, mouseX + 7, mouseY + 12)
            return
        }

        hovered.debtTooltipAt(mouseX, mouseY)?.let {
            graphics.drawSimpleTooltip(it, mouseX + 7, mouseY + 12)
            return
        }
        // clears the swatches sitting against the right edge of the grid rather than covering them
        hovered.renderTooltip(
            graphics,
            hoverControls.x + hoverControls.width + Common.UI.SPACING_LARGE,
            startY)

    }


    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, a)
        }
        graphics.fill(0, 0, width, height, Common.UI.SCREEN_DIM_COLOR)
        McCompat.extractDeferredSubtitles(this.minecraft)
    }

    /** The event in layout units. */
    private fun scaled(event: MouseButtonEvent): MouseButtonEvent =
        MouseButtonEvent(event.x / drawScale, event.y / drawScale, MouseButtonInfo(event.button(), event.modifiers()))

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseButtonEvent = scaled(event)

        // a handler may open or close an overlay, so the list being walked is a copy of it
        overlays.toList().forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) {
                return true
            }
        }

        // asked before the sweep below, which had already shut this widget's list: it found it closed
        // and opened it again, so a second click never collapsed anything
        if (currentDisplay == CurrentDisplay.Presets && gridSelector.mouseClicked(mouseButtonEvent, doubled)) {
            return true
        }

        // the click landed outside every overlay, which is what closes them
        closeOverlays()

        // the wheel's other job: a middle click in greenhouse mode makes it walk the swatches
        // instead of the plots, and another puts it back. Remembered past the screen, not to disk
        if (currentDisplay == CurrentDisplay.Greenhouses && mouseButtonEvent.button() == 2) {
            scrollPicksInfo = !scrollPicksInfo
            return true
        }

        if (currentDisplay == CurrentDisplay.Greenhouses &&
            greenhousePanel.mouseClicked(mouseButtonEvent, doubled)
        ) {
            return true
        }

        if (currentDisplay == CurrentDisplay.Greenhouses && displayedGridWidget != null) {
            if (plotTabs.mouseClicked(mouseButtonEvent)) return true
            if (teleportTab.mouseClicked(mouseButtonEvent)) return true
        }

        if (currentDisplay == CurrentDisplay.Presets && displayedGridWidget != null && partTabs.mouseClicked(mouseButtonEvent)) {
            return true
        }

        if (currentDisplay == CurrentDisplay.Presets) {
            // a right click puts a picked plant down, wherever the mouse is
            if (mouseButtonEvent.button() == 1 && plantPalette.selected != null) {
                plantPalette.dropSelection()
                return true
            }

            if (plantPalette.mouseClicked(mouseButtonEvent, doubled)) return true

            // a picked plant lands on the slot clicked
            plantPalette.selected?.let { picked ->
                val overSlot = (displayedGridWidget ?: emptyGridWidget)?.slotAt(mouseButtonEvent.x, mouseButtonEvent.y) != null
                if (mouseButtonEvent.button() == 0 && overSlot) {
                    placeDragged(picked, mouseButtonEvent.x, mouseButtonEvent.y)
                    return true
                }
            }

            if (mouseButtonEvent.button() == 0) {
                // with the switch on, a click on bare soil takes the soil off the preset
                if (plantPalette.deleteMode && displayedGridWidget?.hoveredElement == null) {
                    displayedGridWidget?.let { grid ->
                        val (sx, sy) = grid.slotAt(mouseButtonEvent.x, mouseButtonEvent.y) ?: return@let
                        val slot = grid.layout.getSlot(sx, sy) ?: return@let
                        if (slot.placedBlock != null && !slot.placedBlock!!.isAir) {
                            remember(grid.layout)
                            slot.placedBlock = Blocks.AIR.defaultBlockState()
                            grid.init()
                            return true
                        }
                    }
                }
                (displayedGridWidget?.hoveredElement as? ElementWidget)?.let { element ->
                    // with the switch on, a click on a plant takes it off the preset
                    if (plantPalette.deleteMode) {
                        removeFromPreset(element.instance)
                        return true
                    }
                    // with the mark selector on something, a click on a plant marks it
                    val choice = plantPalette.markChoice
                    if (choice.applies) {
                        applyMark(element.instance, choice.marking)
                        return true
                    }
                }
            }

            // a right click on a plant says what it stands for in the plan
            if (mouseButtonEvent.button() == 1) {
                (displayedGridWidget?.hoveredElement as? ElementWidget)?.let {
                    openMarkContext(it.instance, mouseButtonEvent)
                    return true
                }
            }
        }

        // a right click on the name at the top renames what is shown: the preset, or the greenhouse
        if (mouseButtonEvent.button() == 1 && dynamicNameDisplay?.isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y) == true) {
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

        if (cropPreviewButton.mouseClicked(mouseButtonEvent, doubled)) {
            ScreenUtil.setScreen(CropPreviewScreen(this))
            return true
        }

        if (currentDisplayToggle.mouseClicked(mouseButtonEvent,doubled)) {
            when (currentDisplay) {
                CurrentDisplay.Greenhouses -> {
                    currentDisplay = CurrentDisplay.Presets
                    initPresetLayout()
                }
                CurrentDisplay.Presets -> {
                    currentDisplay = CurrentDisplay.Greenhouses
                    initGreenhouseLayout()
                }
            }
            return true
        }
        // the swatches are only beside a greenhouse, and off screen they still sit where they
        // were last laid out, so they are asked before the grid but only where they exist
        if (currentDisplay == CurrentDisplay.Greenhouses &&
            hoverControls.mouseClicked(mouseButtonEvent, doubled)
        ) {
            return true
        }

        if (displayedGridWidget?.mouseClicked(mouseButtonEvent, doubled) == true) {
            return true
        }
        // the preset ui is only laid out in preset mode, off screen its buttons still sit at 0,0
        // and would take clicks meant for the corner of the screen
        if (currentDisplay == CurrentDisplay.Presets &&
            presetUI.mouseClicked(mouseButtonEvent, doubled)
        ) {
            return true
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseMoved(realX: Double, realY: Double) {
        val mouseX = realX / drawScale
        val mouseY = realY / drawScale

        hoveredElement?.isFocused = false
        hoveredElement = null

        overlays.toList().forEach {
            it.mouseMoved(mouseX, mouseY)
            if (hoveredElement == null && it.hoveredElement != null) {
                hoveredElement = it.hoveredElement
            }

            if (hoveredElement == null && it.isMouseOver(mouseX.toInt(), mouseY.toInt())) {
                hoveredElement = it
            }
        }
        if (currentDisplay == CurrentDisplay.Greenhouses) {
            hoverControls.mouseMoved(mouseX, mouseY)
            plotTabs.mouseMoved(mouseX, mouseY)
            teleportTab.mouseMoved(mouseX, mouseY)
        }
        if (currentDisplay == CurrentDisplay.Presets) {
            plantPalette.mouseMoved(mouseX, mouseY)
            partTabs.mouseMoved(mouseX, mouseY)
        }

        displayedGridWidget?.mouseMoved(mouseX, mouseY)

        if (hoveredElement == null) {
            if (displayedGridWidget?.hoveredElement != null) {
                hoveredElement = displayedGridWidget!!.hoveredElement!!
            }
        }
        currentDisplayToggle.mouseMoved(mouseX, mouseY)

        if (hoveredElement == null) {
            if (currentDisplayToggle.isMouseOver(mouseX, mouseY)) {
                hoveredElement = currentDisplayToggle
            }
        }
        if (currentDisplay == CurrentDisplay.Presets) {
            presetUI.mouseMoved(mouseX, mouseY)

            if (hoveredElement == null) {
                hoveredElement = presetUI.hoveredElement
            }
        }

        dynamicNameDisplay?.mouseMoved(mouseX, mouseY)
        hoverWarning = dynamicNameDisplay?.isMouseOver(mouseX, mouseY) ?: false && shouldWarn
        hoveredElement?.isFocused = true
    }



    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        overlays.toList().forEach {
            if (it.charTyped(characterEvent)) return true
        }
        if (currentDisplay == CurrentDisplay.Presets && plantPalette.charTyped(characterEvent)) return true
        return super.charTyped(characterEvent)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (currentDisplay == CurrentDisplay.Presets && plantPalette.mouseDragged(event.x / drawScale, event.y / drawScale)) return true
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        plantPalette.mouseReleased()?.let { placeDragged(it, event.x / drawScale, event.y / drawScale) }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(realX: Double, realY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mouseX = realX / drawScale
        val mouseY = realY / drawScale

        // an open list takes the wheel before the screen's own scrolling does
        overlays.toList().forEach {
            if (it.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true
        }

        if (scrollY == 0.0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        if (currentDisplay == CurrentDisplay.Presets && plantPalette.mouseScrolled(mouseX, mouseY, scrollY)) return true

        // the wheel walks the plots or presets; in greenhouse mode a middle click points it at
        // the swatches instead, and presets never care which way that switch is left
        if (currentDisplay == CurrentDisplay.Greenhouses && scrollPicksInfo) {
            hoverControls.cycle(down = scrollY < 0)
        } else {
            cycleDisplayedGrid(forward = scrollY < 0)
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
    private fun presetChanged(master: MasterLayout) {
        GreenhouseData.currentPreset = master
        shownPlot = null
        presetCleared = false
        initPresetLayout()
    }

    /** Puts an imported layout where it belongs: into the empty plot on show, or as a new preset. */
    private fun imported(result: LayoutTransferResult.Imported) {
        val master = GreenhouseData.currentPreset
        val shown = displayedGridWidget?.layout

        when {
            // one plot into the empty plot on show, keeping the plot's place in its preset
            master != null && shown != null && result.plots.size == 1 && shown.elementInstances.isEmpty() -> {
                shown.takeContentsFrom(result.layout)
                if (master.name == null) master.name = result.layout.name
                initPresetLayout()
                ChatUtils.sendWithPrefix("Imported into ${GreenhouseData.describe(shown)}")
            }
            // several plots into a preset with nothing in it, replacing its plots
            master != null && master.isEmpty() && result.plots.size > 1 -> {
                master.plots.clear()
                result.plots.forEachIndexed { index, plot ->
                    GreenhouseLayout(id = master.plotId(index), name = plot.name).also { it.takeContentsFrom(plot) }.let(master.plots::add)
                }
                if (master.name == null) master.name = result.layout.name
                shownPlot = null
                initPresetLayout()
                ChatUtils.sendWithPrefix("Imported into ${master.displayName()}")
            }
            else -> {
                val preset = MasterLayout(id = result.layout.id, name = result.layout.name)
                result.plots.forEachIndexed { index, plot ->
                    GreenhouseLayout(id = preset.plotId(index), name = plot.name.takeIf { index > 0 || result.plots.size > 1 }).also { it.takeContentsFrom(plot) }.let(preset.plots::add)
                }
                addPresetLayout(preset)
            }
        }
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        overlays.toList().forEach {
            if (it.keyPressed(keyEvent)) return true
        }
        if (currentDisplay == CurrentDisplay.Presets && plantPalette.keyPressed(keyEvent)) return true
        return super.keyPressed(keyEvent)
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
                    initDynamicName()
                }
            }
        }
        addContext(menu)
    }

    fun gridWidgetChanged(layout: GreenhouseLayout) {
        if (greenhouseGridWidgets.isEmpty() && presetGridWidgets.isEmpty()) return

        var widget = greenhouseGridWidgets.find { it.layout == layout }
        if (widget == null){
            widget = presetGridWidgets.find {it.layout == layout}
        }
        if (widget == null){
            ChatUtils.sendWithPrefix("Unable to find correct layout")
            return
        }

        GreenhouseData.greenhouseGrids
            .indexOfFirst { it.layout === widget.layout }
            .takeIf { it >= 0 }
            ?.let { GreenhouseData.currentGridIndex = it }

        displayedGridWidget = widget
        plotTabs.selected = widget.layout
        presetCleared = false
        displayedName = widget.layout.displayName()

        dynamicNameDisplay = null
        initDynamicName()
    }

    /** Whether the greenhouse on screen has a plan running, which is what the button is for. */
    private fun plannerRunning(): Boolean =
        currentDisplay == CurrentDisplay.Greenhouses &&
                displayedGrid()?.state?.assignedLayout != null

    /** The greenhouse on screen is whichever the selector shows, not the one being stood in. */
    private fun displayedGrid(): GreenhouseGrid? {
        val layout = displayedGridWidget?.layout ?: return null

        return GreenhouseData.greenhouseGrids.firstOrNull { it.layout === layout }
    }

    fun assignPresetLayout(layout: GreenhouseLayout?, grid: GreenhouseGrid) {
        if (layout == null) {
            ChatUtils.sendWithPrefix(
                "No such plan to run on ${grid.layout.displayName()}"
            )
            return
        }
        grid.state.assignedLayout = layout
        grid.state.completionMuted = false
        GreenhouseData.regenRender()

        ChatUtils.sendWithPrefix(
            "Planner active on ${grid.layout.displayName()} for ${GreenhouseData.describe(layout)}"
        )
    }

    /** Gives the current preset one more plot, shown at once. */
    private fun addPlot() {
        val master = GreenhouseData.currentPreset ?: return
        if (master.plots.size >= MasterLayout.MAX_PLOTS) {
            ChatUtils.sendWithPrefix("A preset holds at most ${MasterLayout.MAX_PLOTS} plots, one a greenhouse.")
            return
        }
        shownPlot = master.addPlot()
        initPresetLayout()
    }

    fun addPresetLayout(master: MasterLayout){
        GreenhouseData.presetGrids.add(master)
        GreenhouseData.currentPreset = master
        shownPlot = null
        presetCleared = false
        initPresetLayout()
    }

    /** Takes [plot] off the current preset, or with null the whole preset; the last plot takes the preset with it. */
    fun removePresetLayout(plot: GreenhouseLayout?) {
        val master = GreenhouseData.currentPreset
        if (master != null && plot != null && master.plots.size > 1) {
            master.plots.remove(plot)
            shownPlot = null
            initPresetLayout()
            return
        }

        val layoutNum = GreenhouseData.currentPreset?.id?.removePrefix("preset_")?.toIntOrNull() ?: run {
            ChatUtils.sendWithPrefix("No preset to remove.")
            return
        }

        GreenhouseData.currentPreset?.let {
            GreenhouseData.presetGrids.remove(it)

            GreenhouseData.currentPreset = GreenhouseData.presetGrids.find { preset ->
                preset.id.removePrefix("preset_").toIntOrNull() == layoutNum + 1 ||
                        preset.id.removePrefix("preset_").toIntOrNull() == layoutNum - 1
            }
        }
        initPresetLayout()
    }
    companion object {
        /** Whether the wheel walks the swatches rather than the plots, kept for the session. */
        private var scrollPicksInfo: Boolean = false

        /** The "not all greenhouses available" warning is sent at most once per game run. */
        private var warnedMissingGreenhouses: Boolean = false

        /** Enough for one button, however narrow the window gets. */
        private const val MIN_ACTION_ROW_WIDTH: Int = 90


        /** What the toolbar down the left of the grid needs, so the grid never sits on top of it. */
        private const val TOOLBAR_WIDTH: Int = 180

        /** Below this the item art rounds away to nothing, so the grid stops shrinking instead. */
        private const val MIN_SLOT_SIZE: Int = 8

        /** The units the panels and grid need; a smaller window is drawn at the first scale that gives them. */
        private const val COMFORTABLE_WIDTH: Int = 800
        private const val COMFORTABLE_HEIGHT: Int = 400

        /** Full size, then gui scale 3 at 1080p, then gui scale 4 at 1080p. */
        private val DRAW_SCALES: List<Float> = listOf(1f, 0.75f, 0.5f)

        /** Where the name box sits from the top of the screen. */
        private const val NAME_TOP: Int = 9

        private const val TELEPORT_WIDTH: Int = 110
        private const val TELEPORT_LABEL: String = "Teleport to Plot"

        /** The layout behind the empty grid, never saved. */
        private const val EMPTY_GRID_ID: String = "preset_none"


        /** About what the mark menu takes, for keeping it on screen. */
        private const val MARK_MENU_WIDTH: Int = 80
        private const val MARK_MENU_HEIGHT: Int = 100

        /** The footprint a carried plant would take, seen through. */
        private const val DROP_OK: Int = 0x6000FF00
        private const val DROP_BLOCKED: Int = 0x60FF0000

        /** The next tick box, top left. */
        private const val TIME_LEFT: Int = 10
        private const val TIME_CENTER_Y: Int = 18

        private const val SHELF_VIEW: String = "View"
        private const val SHELF_GREENHOUSE: String = "Greenhouse"
        private const val SHELF_PRESET: String = "Preset"

        /** What each part of the tick period runs up to. */
        private const val MAX_UNIQUE_CROPS: Int = 12
        private const val MAX_SPEED_UPGRADE: Int = 9
        private const val MAX_ATTRIBUTE: Int = 10

        private const val RENAME_HINT: String = "\nRight click a name to rename it: greenhouses, presets and plots"
        private const val SCROLL_HINT_GREENHOUSES: String = "Scroll the mouse wheel to switch greenhouse$RENAME_HINT"
        private const val SCROLL_HINT_PRESETS: String = "Scroll the mouse wheel to switch preset$RENAME_HINT"

        /** How many actions the arrows can walk back. */
        private const val HISTORY_LIMIT: Int = 50
    }


    private operator fun IntArray.component4(): Int = this[3]
}
