package org.magic.magicaddons.ui.screens

import org.magic.magicaddons.util.toReadableDuration
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.features.farming.GreenhouseScreenCommand
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
import org.magic.magicaddons.ui.widgets.greenhouse.HoverControls
import org.magic.magicaddons.ui.widgets.greenhouse.PresetUI
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

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



    //todo
    // preset buttons would be like: add crop (render all itemstacks of all crops scrollable) with search on enum thingy
    // (maybe instead of scrollable typable and autocomplete? since easier)
    // remove crop, render highlight

    //todo PS maybe later on add custom layers as well as the base ones
    // for now make ingredients cancel break, target not cancel
    // (unless not fully grown but that later since we dont have all stages)

    //todo in the base feature add not all unique crops detected warning
    //todo add snoozling sleeping warning, thunderling thunder warning (prob forgot more)

    //todo add inference of stages, on next tick time detection timestamp it, and every time
    // upon reopening the screen check that time, if its past the tick time,
    // will have to think about how to do this because cant just add +1 to everything
    // need logic like, if it exits this range and enters this one
    // after all that is sorted :pray: add a warning when its the inference
    // eg when tick has passed make a yellow warning saying this is a prediction of the stages
    // will be hard, but validate scan from previous tick (if only 1 tick difference)
    // and if no crop has been detected in an old crop's spot, make it a yellow warning on the corner
    // informing the user that the crop definition has not been added for this crop and to send the debug to me

    var currentDisplay = CurrentDisplay.Greenhouses

    /**
     * Whether the greenhouse on screen is running on guessed growth. Read while drawing rather than
     * stored, since the tick that makes it stale can land while the screen is open.
     */
    private val shouldWarn: Boolean
        get() = currentDisplay == CurrentDisplay.Greenhouses &&
                (GreenhouseData.greenhouseGrids.getOrNull(GreenhouseData.currentGridIndex)
                    ?.state?.pendingGrowthTicks ?: 1) > 0
    var borderPadding: Int = 6

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

    private var dynamicNameDisplay: ClickableButtonWidget? = null
    private var hoverWarning = false

    private val gridSelector = EnumWidget(
        values = greenhouseGridWidgets.map { it.layout },
        currentValue = displayedGridWidget?.layout,
        onRightClickValue = { widget, event ->
            openLayoutWidgetContext(widget, event) },
        valueChanged = { gridWidgetChanged(it) },
        overlayContext = this
    )

    private val presetUI = PresetUI(
        400,
        400,
        this,
        onAssignedLayout = { assignedLayout, selectedGrid ->
            assignPresetLayout(assignedLayout, selectedGrid)
        },
        onAddPreset = {
            addPresetLayout(it)
        },
        onRemovePreset = {
            removePresetLayout()
        }
    )



    var displayedName: String = "Error loading name."

    var slotSize: Int = 20

    private val hoverControls = HoverControls()


    override fun init() {
        super.init()
        initBaseLayout()

        // said here rather than on joining a world or on every tick: opening this screen is the
        // player asking about their greenhouses, which is the one moment a missing number is worth
        // interrupting them for, and the warning carries its own cooldown against repeats
        if (!GreenhouseData.miscInfo.shouldIgnoreWarning) {
            GreenhouseData.warnUnknownValues()
        }
    }
    fun initBaseLayout(){

        paddingY = height / 10

        // the toolbar down the left and the swatches plus a tooltip down the right have to fit
        // beside the grid, so the grid takes whichever of the two axes runs out first
        val sideRoom = TOOLBAR_WIDTH + HoverControls.TOTAL_WIDTH + Common.UI.SPACING_LARGE * 2
        val heightRoom = height - paddingY * 2 - borderPadding * 2
        val widthRoom = width - sideRoom - borderPadding * 2

        slotSize = (minOf(heightRoom, widthRoom) / gridSize).coerceAtLeast(MIN_SLOT_SIZE)
        containerSize = (slotSize + 1) * gridSize

        // never left of the toolbar, however narrow the window gets
        startX = ((width - containerSize) / 2).coerceAtLeast(TOOLBAR_WIDTH + Common.UI.SPACING_LARGE)
        startY = paddingY

        currentDisplayToggle.x = Common.UI.SPACING_LARGE
        currentDisplayToggle.y = startY + borderPadding * 2

        gridSelector.x = currentDisplayToggle.x + currentDisplayToggle.width + Common.UI.SPACING_LARGE
        gridSelector.y = startY + borderPadding * 2
        gridSelector.width = 100
        gridSelector.height = currentDisplayToggle.height
        addOverlay(gridSelector.overlay)

        hoverControls.layoutAgainstGrid(startX + containerSize, startY, containerSize)
        
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
                ChatUtils.sendWithCommand(
                    "Plot data is null, please join skyblock.",
                    "/desk"
                )
            }
            return
        }
        if (amountInitialized != PlotAPI.plots.count { it.data?.isGreenhouse ?: throw IllegalStateException("Plot data was null after null check.") }){
            if (!GreenhouseData.miscInfo.shouldIgnoreWarning){
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
        if (displayedGridWidget == null) {
            ChatUtils.sendWithPrefix("Unable to find your greenhouses.")
            return
        }

        displayedName = displayedGridWidget?.layout?.name
            ?: displayedGridWidget?.layout?.id
            ?: "Unknown Plot"




        gridSelector.currentValue = displayedGridWidget!!.layout
        gridSelector.values = greenhouseGridWidgets.map { it.layout }

        relayoutSelector()

        initDynamicName()
    }

    fun initPresetLayout(){
        presetGridWidgets.clear()
        displayedGridWidget = null
        hoveredElement = null


        presetUI.x = currentDisplayToggle.x
        presetUI.y = currentDisplayToggle.y + currentDisplayToggle.height + Common.UI.SPACING_LARGE
        presetUI.init()

        GreenhouseData.presetGrids.forEach { layout ->
            val gridWidget = GridWidget(layout, slotSize).apply {
                widgetX = startX
                widgetY = startY
                widgetWidth = containerSize
                widgetHeight = containerSize
                init()
            }
            presetGridWidgets.add(gridWidget)
        }
        if (GreenhouseData.currentPreset == null){
            GreenhouseData.currentPreset = GreenhouseData.presetGrids.firstOrNull()
        }
        displayedGridWidget = presetGridWidgets.find { GreenhouseData.currentPreset == it.layout  }
        displayedGridWidget = displayedGridWidget ?: presetGridWidgets.firstOrNull()

        displayedName = displayedGridWidget?.layout?.name
            ?: displayedGridWidget?.layout?.id
                    ?: "Unknown Preset"
        
        gridSelector.currentValue = displayedGridWidget?.layout
        gridSelector.values = presetGridWidgets.map { it.layout }
        relayoutSelector()
        currentDisplayToggle.message = Component.literal("Presets")


        initDynamicName()

    }

    /** Sizes the selector to whatever it currently lists, which changes when a layout is renamed. */
    private fun relayoutSelector() {
        val maxWidth = gridSelector.values.maxOfOrNull { font.width(it.toString()) }
            ?: font.width("null")

        gridSelector.width = maxWidth + SELECTOR_TEXT_PADDING
    }

    fun initDynamicName(){
        val iconWidth = 18
        val widgetWidth = font.width(displayedName) + 10 //padding = 4 border size 1 x2 (from multiline box centered)
        val widgetHeight = font.lineHeight + 10
        val screenWidth = width
        dynamicNameDisplay = ClickableButtonWidget(
            widgetWidth + iconWidth + 1, //icon padding + icon width
            widgetHeight,
            {
                it.drawMultilineBoxCentered(
                    displayedName,
                    screenWidth / 2,
                    18,
                    if (shouldWarn) Common.UI.WARNING_COLOR else null
                )
                if (shouldWarn){
                it.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("minecraft", "icon/unseen_notification"),
                    ((screenWidth + widgetWidth) / 2) + 1,
                    9,
                    iconWidth,
                    19, // why 19? idk height is randomly +1
                )
                }
            },
            false
        )
        dynamicNameDisplay?.x = (screenWidth-widgetWidth) / 2
        dynamicNameDisplay?.y = 9
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // background
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.fromNamespaceAndPath("minecraft", "popup/background"),
            startX - borderPadding,
            startY - borderPadding,
            containerSize + borderPadding * 2,
            containerSize + borderPadding * 2,
        )

        dynamicNameDisplay?.extractRenderState(graphics,mouseX,mouseY,delta)

        val timeText = GreenhouseData.miscInfo.nextTickTime?.toReadableDuration() ?: "Unknown Time"
        val timeWidth = font.width(timeText)

        graphics.drawMultilineBoxCentered(
            timeText,
            10 + timeWidth/2, 18)

        // presets are a plan rather than a greenhouse that exists, no plant in one has a stage,
        // a water level or an age to report
        displayedGridWidget?.extractRenderState(graphics, mouseX, mouseY, delta)

        if (currentDisplay == CurrentDisplay.Greenhouses) {
            hoverControls.extractRenderState(graphics, mouseX, mouseY, delta)
        }
        gridSelector.extractRenderState(graphics, mouseX, mouseY, delta)
        currentDisplayToggle.extractRenderState(graphics, mouseX, mouseY, delta)

        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> {

            }
            CurrentDisplay.Presets -> {
                presetUI.extractRenderState(graphics, mouseX, mouseY, delta)
            }
        }
        if (hoverWarning) {
            graphics.drawSimpleTooltip(
                """
                    The displayed greenhouse uses prediction based data.
                    Enter it to update its state.
                    """.trimIndent(),
                mouseX + 7,
                mouseY + 30
            )
        }

        overlays.asReversed().forEach {
            it.renderOverlay(graphics, mouseX, mouseY, delta)
        }
        val hovered = hoveredElement
        if (hovered !is ElementWidget) return

        // the star beside a water time answers for itself, and says so instead of the plant's own
        // tooltip, since the mouse is on the star rather than on the plant
        hovered.debtTooltipAt(mouseX, mouseY)?.let {
            graphics.drawSimpleTooltip(it, mouseX + 7, mouseY + 12)
            return
        }
        // clears the swatches sitting against the right edge of the grid rather than covering them
        hovered.renderTooltip(
            graphics,
            hoverControls.x + hoverControls.width + Common.UI.SPACING_LARGE,
            startY + borderPadding *2)

    }


    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, a)
        }
        this.extractMenuBackground(graphics)
        this.minecraft.gui.hud.extractDeferredSubtitles()
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        // a handler may open or close an overlay, so the list being walked is a copy of it
        overlays.toList().forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) {
                return true
            }
        }

        // the click landed outside every overlay, which is what closes them
        closeOverlays()

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
        if (gridSelector.mouseClicked(mouseButtonEvent, doubled)) {
            return true
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {

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
        }

        displayedGridWidget?.pinnedInfo =
            hoverControls.hoveredInfo.takeIf { currentDisplay == CurrentDisplay.Greenhouses }

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
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        overlays.toList().forEach {
            if (it.keyPressed(keyEvent)) return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun removed() {
        super.removed()

        // only the command changes the scale, opening the screen any other way leaves it alone
        GreenhouseScreenCommand.tempGuiScale?.let {
            Minecraft.getInstance().options.guiScale().set(it)
            GreenhouseScreenCommand.tempGuiScale = null
        }
    }

    fun openLayoutWidgetContext(layout: GreenhouseLayout?, buttonEvent: MouseButtonEvent) {
        if (layout == null) return
        if (layout !in greenhouseGridWidgets.map { it.layout } && layout !in presetGridWidgets.map { it.layout }) return
        val (menuX, menuY) = OverlayRenderable.placeOnScreen(
            buttonEvent.x.toInt(),
            buttonEvent.y.toInt(),
            EditLayoutContextMenu.WIDTH,
            EditLayoutContextMenu.HEIGHT
        )

        val menu = EditLayoutContextMenu(
            menuX,
            menuY,
            layout,
            this,
            onLayoutRenamed = { renamed ->
                if (displayedGridWidget?.layout === renamed) {
                    displayedName = renamed.displayName()
                }
                // the selector was sized to fit the old names
                relayoutSelector()
                initDynamicName()
            }
        )
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

        // apply, export and delete all read currentPreset, so selecting one has to move it
        GreenhouseData.currentPreset =
            widget.layout.takeIf { currentDisplay == CurrentDisplay.Presets }

        displayedGridWidget = widget
        displayedName = displayedGridWidget?.layout?.name
            ?: displayedGridWidget?.layout?.id
                    ?: "Unknown Preset"

        dynamicNameDisplay = null
        initDynamicName()
    }

    fun assignPresetLayout(layout: GreenhouseLayout?, grid: GreenhouseGrid) {
        if (layout == null) {
            ChatUtils.sendWithPrefix("Cannot assign a non existing layout to grid: ${grid.layout.name ?: grid.layout.id}")
            return
        }
        grid.state.assignedLayout = layout
        GreenhouseData.regenRender()
        ChatUtils.sendWithPrefix("Assigned ${layout.displayName()} to: ${grid.layout.displayName()}")
    }

    fun addPresetLayout(layout: GreenhouseLayout){
        GreenhouseData.presetGrids.add(layout)
        GreenhouseData.currentPreset = layout
        initPresetLayout()
    }

    fun removePresetLayout(){
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
        /** Room either side of the widest name the selector lists. */
        private const val SELECTOR_TEXT_PADDING: Int = 12

        /** What the toolbar down the left of the grid needs, so the grid never sits on top of it. */
        private const val TOOLBAR_WIDTH: Int = 180

        /** Below this the item art rounds away to nothing, so the grid stops shrinking instead. */
        private const val MIN_SLOT_SIZE: Int = 8
    }

}