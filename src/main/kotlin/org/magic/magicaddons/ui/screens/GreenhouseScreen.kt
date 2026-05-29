package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.toReadableDuration
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.greenhouse.EditLayoutContextMenu
import org.magic.magicaddons.ui.widgets.EnumWidget
import org.magic.magicaddons.ui.widgets.config.ClickableButtonWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhouseElementWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhouseGridWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhousePresetUI
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

class GreenhouseScreen(title: Component) : Screen(title), HoverableContainer, OverlayContext {

    init {
        EventBus.register(this)
    }

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
    var borderPadding: Int = 6

    override var hoveredElement: GuiEventListener? = null

    override val overlays = mutableListOf<OverlayRenderable>()
    private var displayedGridWidget: GreenhouseGridWidget? = null
    private val greenhouseGridWidgets: MutableList<GreenhouseGridWidget> = mutableListOf()
    private val presetGridWidgets: MutableList<GreenhouseGridWidget> = mutableListOf()
    private var currentGridIndex: Int = 0
    private var currentPresetLayout: GreenhouseLayout? = null
    private val currentDisplayToggle = ClickableButtonWidget(
        0,
        0,
        60,
        26,
        Component.literal("Plots")
    )


    private val gridSelector = EnumWidget(
        values = greenhouseGridWidgets.map { it.layout },
        currentValue = displayedGridWidget?.layout,
        onRightClickValue = { widget, event ->
            openGridWidgetContext(widget, event) },
        valueChanged = { gridWidgetChanged(it) },
        overlayContext = this
    )

    private val presetUI = GreenhousePresetUI(
        0,
        0,
        400,
        400,
        this,
        selectedPreset = currentPresetLayout,
        onAssignedLayout = { assignedLayout, selectedGrid ->
            assignPresetLayout(assignedLayout, selectedGrid)
        },
        onAddPreset = {
            addPresetLayout(it)
        }
    )

    var displayedName: String = "Error loading name."

    var slotSize: Int = 20
    var savedWidth: Int? = null
    var savedHeight: Int? = null


    override fun init() {
        super.init()
        initBaseLayout()
    }
    fun initBaseLayout(){

        savedWidth = width
        savedHeight = height

        paddingY = height/10

        slotSize = (height - paddingY * 2 - borderPadding * 2) / gridSize
        containerSize = (slotSize + 1) * gridSize

        startX = (width - containerSize) / 2
        startY = paddingY

        currentDisplayToggle.x = 10
        currentDisplayToggle.y = startY + borderPadding *2

        gridSelector.x = currentDisplayToggle.x + currentDisplayToggle.width + 10
        gridSelector.y = startY + borderPadding *2
        gridSelector.width = 100
        gridSelector.height = currentDisplayToggle.height
        addOverlay(gridSelector.overlay)
        
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
        greenhouseGridWidgets.clear()

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
            val gridWidget = GreenhouseGridWidget(grid.layout, slotSize).apply {
                widgetX = startX
                widgetY = startY
                widgetWidth = containerSize
                widgetHeight = containerSize
                init()
            }
            if ("plot_${PlotAPI.getCurrentPlot()?.id}" == grid.layout.id)
                currentGridIndex = index
            greenhouseGridWidgets.add(gridWidget)
        }
        displayedGridWidget = greenhouseGridWidgets.getOrNull(currentGridIndex)

        if (displayedGridWidget == null) {
            displayedGridWidget = greenhouseGridWidgets.firstOrNull()
        }
        if (displayedGridWidget == null) {
            //todo maybe add an auto switch to preset mode if not available.
            // if ended up here means either we dont have any data, or player doesnt have any greenhouses
            return
        }

        displayedName = displayedGridWidget?.layout?.name
            ?: displayedGridWidget?.layout?.id
            ?: "Unknown Plot"


        gridSelector.currentValue = displayedGridWidget!!.layout
        gridSelector.values = greenhouseGridWidgets.map { it.layout }

        val maxWidth = greenhouseGridWidgets.maxOf {
            font.width(it.layout.toString())
        }

        gridSelector.width = maxWidth + 12
        currentDisplayToggle.message = Component.literal("Plots")
    }

    fun initPresetLayout(){
        presetGridWidgets.clear()
        displayedGridWidget = null


        presetUI.x = currentDisplayToggle.x
        presetUI.y = currentDisplayToggle.y + currentDisplayToggle.height + 10
        presetUI.init()


        GreenhouseData.presetGrids.forEach { layout ->
            val gridWidget = GreenhouseGridWidget(layout, slotSize).apply {
                widgetX = startX
                widgetY = startY
                widgetWidth = containerSize
                widgetHeight = containerSize
                init()
            }
            presetGridWidgets.add(gridWidget)
        }
        displayedGridWidget = presetGridWidgets.find { currentPresetLayout == it.layout  }
        displayedGridWidget = displayedGridWidget ?: presetGridWidgets.firstOrNull()

        displayedName = displayedGridWidget?.layout?.name
            ?: displayedGridWidget?.layout?.id
                    ?: "Unknown Preset"
        
        gridSelector.currentValue = displayedGridWidget?.layout
        gridSelector.values = presetGridWidgets.map { it.layout }
        val maxWidth = presetGridWidgets.maxOfOrNull {
            font.width(it.layout.toString())
        }
        gridSelector.width = (maxWidth ?: font.width("null")) + 20
        currentDisplayToggle.message = Component.literal("Presets")
        
    }


    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // background
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.fromNamespaceAndPath("minecraft", "popup/background"),
            startX - borderPadding,
            startY - borderPadding,
            containerSize + borderPadding * 2,
            containerSize + borderPadding * 2,
        )

        graphics.drawMultilineBoxCentered(
            displayedName,
            width / 2,
            18
        )
        val timeText = GreenhouseData.miscInfo.nextTickTime?.toReadableDuration() ?: "Unknown Time"
        val timeWidth = font.width(timeText)

        graphics.drawMultilineBoxCentered(
            timeText,
            10 + timeWidth/2, 18)

        displayedGridWidget?.render(graphics, mouseX, mouseY, delta)
        gridSelector.render(graphics, mouseX, mouseY, delta)
        currentDisplayToggle.render(graphics, mouseX, mouseY, delta)

        when (currentDisplay) {
            CurrentDisplay.Greenhouses -> {

            }
            CurrentDisplay.Presets -> {
                presetUI.render(graphics, mouseX, mouseY, delta)
            }
        }

        overlays.asReversed().forEach {
            it.renderOverlay(graphics, mouseX, mouseY, delta)
        }
        val hovered = hoveredElement
        if (hovered !is GreenhouseElementWidget) return
        hovered.renderTooltip(
            graphics,
            startX + containerSize,
            startY + borderPadding *2)

    }


    override fun renderBackground(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        if (this.minecraft.level == null) {
            this.renderPanorama(guiGraphics, f)
        }
        this.renderMenuBackground(guiGraphics)
        this.minecraft.gui.renderDeferredSubtitles()
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        overlays.forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) {
                return true
            }
        }

        overlays.clear()

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
        if (presetUI.mouseClicked(mouseButtonEvent, doubled)) {
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

        overlays.forEach {
            it.mouseMoved(mouseX, mouseY)
            if (hoveredElement == null && it.hoveredElement != null) {
                hoveredElement = it.hoveredElement
            }

            if (hoveredElement == null && it.isMouseOver(mouseX.toInt(), mouseY.toInt())) {
                hoveredElement = it
            }
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
        presetUI.mouseMoved(mouseX, mouseY)

        if (hoveredElement == null) {
            if (presetUI.hoveredElement != null) {
                hoveredElement = presetUI.hoveredElement
            }
            else {
                if (presetUI.isMouseOver(mouseX, mouseY)) {
                    hoveredElement = presetUI
                }
            }
        }


        hoveredElement?.isFocused = true
    }



    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY)
    }




    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        overlays.forEach {
            it.charTyped(characterEvent)
        }
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        overlays.forEach {
            it.keyPressed(keyEvent)
        }
        return super.keyPressed(keyEvent)
    }

    fun openGridWidgetContext(layout: GreenhouseLayout?, buttonEvent: MouseButtonEvent) {
        if (layout == null) return
        if (layout !in greenhouseGridWidgets.map { it.layout } && layout !in presetGridWidgets.map { it.layout }) return //todo implement preset handling as well
        val menu = EditLayoutContextMenu(
            buttonEvent.x.toInt(),
            buttonEvent.y.toInt(),
            layout
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

        currentGridIndex = greenhouseGridWidgets.indexOf(widget)
        displayedGridWidget = widget
        displayedName = displayedGridWidget?.layout?.name
            ?: displayedGridWidget?.layout?.id
                    ?: "Unknown Preset"
    }

    fun assignPresetLayout(layout: GreenhouseLayout?, grid: GreenhouseGrid) {
        if (layout == null) {
            ChatUtils.sendWithPrefix("Cannot assign a non existing layout to grid: ${grid.layout.name ?: grid.layout.id}")
            return
        }
        ChatUtils.sendWithPrefix("(WIP) Assigned ${layout.name ?: layout.id} to grid: ${grid.layout.name ?: grid.layout.id}")
        grid.state.assignedLayout = layout
    }

    fun addPresetLayout(layout: GreenhouseLayout){
        ChatUtils.sendWithPrefix("layout size: ${layout.elementInstances.size}")
        GreenhouseData.presetGrids.add(layout)
        currentPresetLayout = layout
        initPresetLayout()
    }
}