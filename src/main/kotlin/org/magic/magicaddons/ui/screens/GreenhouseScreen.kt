package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.ui.widgets.ArrowWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhouseElementWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhouseGridWidget
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

class GreenhouseScreen(title: Component) : Screen(title) {

    init {
        EventBus.register(this)
    }


    private val gridSize = 10

    private var paddingY: Int = 40
    private var startX: Int = 0
    private var startY: Int = 0
    private var containerSize: Int = 400

    var hoveredWidget: GreenhouseElementWidget? = null

    //todo add presets button which toggles between viewing your current REAL greenhouses
    // and presets you created (or imported)
    // that toggles between the buttons side visibility
    // preset buttons would be like: add crop (render all itemstacks of all crops scrollable)
    // (maybe instead of scrollable typable and autocomplete? since easier)
    // remove crop, render highlight,
    // and optionally move the tooltip of the crop to the right hand side (on non presets)
    // (if tooltip too large dont display maybe display tinier version)

    //todo PS maybe later on add custom layers as well as the base ones
    // for now make ingredients cancel break, target not cancel
    // (unless not fully grown but that later since we dont have all stages)

    //todo in the base feature add not all unique crops detected warning
    //todo add snoozling sleeping warning, thunderling thunder warning (prob forgot more)

    //todo after presets menu (long while)
    // add handling of sky mutation layout
    // eg NrAMBoIIgZQVwOYEMBOACAwkgdgUyuAEwC64Y4AjOFAEIoD2A7tmgLJwDOAFg-QLYESZCAGZqAQW6MUuJABcuBUKXIA2CVJnzFkFRAAcGrtNkKlKqlSiTjWs7rJUx1zaZ3LH4dS9tvznwx8TbX9gQkhqVlwAG3oWGFxcABMOQRVwqxtg+w8w8Gcsu3d08AAWI2zisnCAVgqi0PDvQr8HPIB2etbc8IBOLpC2sWgAdS5TNATk1KIVMUzXQdyxAsWcufBOoIa28oXfJZVy1YP1snLy7e6j8Dqrw-OvAbPgcq2Wh9fwfvuXuvDflVgHVLh8-ptnkC6j8wUD1NBYaF1PtKkiiJC0aC1nCIYDQp0AYi2p1nAAFehyeT0NJkTpY05Azp3LAAYzknBpwE6zWx+NxRNynUCApUnR+rHocQAZrFGLgUJzDAjeW1DISVblDPTUaqnnjdT9SXA+AAHADWAEt4okUpz+tAsCgGHI7ej9bl+tqdh69SKyP1AiMLdEkmgAEr0Dj4Wb+77UDD0Fn0JBoGiybAzITkAEAMQtMgIFD0ZWoADlcAp5WgRqgXZRi3dy5X0DWUHWi8JcXmC-XOz9u9GO8AqNAm+MW7XCxY3QOp55LmOq6329O7rPe8PcYuJ2255uftvq5ON+EAYfl3vwoF10OxLn84ONguK+Oj7uN2I1w+92JvOfj7e4DXt+H5xlA-7vkO5Sji+S4ATc949lBQFlrBO4rmQdTQDeKh1FYEEYcC+Soc2b6EXUX5IbheoEXudTAVRZDqM4OFMbcJGvheG7qH+aFkXu6gMY+bEHnxXFDp0MGkeJoqUNQrHAIY+FifBZCGM4tEboYlHCYpXYgUOhiBJpQ79FYCn9CxBkqP0jYqZBNlbvZK7EEAA
    // this layout = is lzstring of this:
    // [[0,0,"Sugar Cane",2],[0,1,"Brown Mushroom",2],[0,3,"Ashwreath",0],[0,6,"Ashwreath",0],[0,8,"Ashwreath",0],[1,1,"Ashwreath",0],[1,3,"Ashwreath",0],[1,6,"Ashwreath",0],[1,8,"Ashwreath",0],[2,0,"Melon Seeds",2],[2,1,"Ashwreath",0],[2,3,"Ashwreath",0],[2,4,"Ashwreath",0],[2,5,"Ashwreath",0],[2,6,"Ashwreath",0],[2,7,"Ashwreath",0],[2,9,"Ashwreath",0],[3,0,"Wheat Seeds",2],[3,1,"Ashwreath",0],[3,3,"Ashwreath",0],[3,7,"Ashwreath",0],[4,1,"Ashwreath",0],[4,3,"Ashwreath",0],[4,4,"Ashwreath",0],[4,5,"Ashwreath",0],[4,6,"Ashwreath",0],[4,7,"Ashwreath",0],[4,9,"Ashwreath",0],[5,2,"Ashwreath",0],[5,4,"Ashwreath",0],[5,7,"Ashwreath",0],[5,9,"Ashwreath",0],[6,0,"Ashwreath",0],[6,1,"Ashwreath",0],[6,2,"Ashwreath",0],[6,4,"Ashwreath",0],[6,7,"Ashwreath",0],[7,2,"Ashwreath",0],[7,3,"Potato",2],[7,4,"Ashwreath",0],[7,5,"Cactus",2],[7,6,"Ashwreath",0],[7,7,"Ashwreath",0],[7,8,"Ashwreath",0],[7,9,"Moonflower",2],[8,0,"Ashwreath",0],[8,2,"Ashwreath",0],[8,4,"Ashwreath",0],[8,6,"Ashwreath",0],[8,9,"Pumpkin Seeds",2],[9,0,"Carrot",2],[9,2,"Ashwreath",0],[9,4,"Ashwreath",0],[9,6,"Ashwreath",0],[9,8,"Wild Rose",2],[9,9,"Cocoa Beans",2],[0,2,"Fire",1],[0,4,"Nether Wart",1],[0,5,"Nether Wart",1],[0,7,"Fire",1],[0,9,"Fire",1],[1,0,"Nether Wart",1],[1,2,"Fire",1],[1,4,"Nether Wart",1],[1,5,"Fire",1],[1,7,"Nether Wart",1],[1,9,"Nether Wart",1],[2,2,"Nether Wart",1],[2,8,"Fire",1],[3,2,"Fire",1],[3,4,"Nether Wart",1],[3,5,"Fire",1],[3,6,"Nether Wart",1],[3,8,"Fire",1],[3,9,"Nether Wart",1],[4,0,"Nether Wart",1],[4,2,"Fire",1],[4,8,"Nether Wart",1],[5,0,"Fire",1],[5,1,"Nether Wart",1],[5,3,"Nether Wart",1],[5,5,"Fire",1],[5,6,"Nether Wart",1],[5,8,"Fire",1],[6,3,"Fire",1],[6,5,"Nether Wart",1],[6,6,"Nether Wart",1],[6,8,"Fire",1],[6,9,"Nether Wart",1],[7,0,"Nether Wart",1],[7,1,"Fire",1],[8,1,"Nether Wart",1],[8,3,"Nether Wart",1],[8,5,"Fire",1],[8,7,"Fire",1],[8,8,"Nether Wart",1],[9,1,"Fire",1],[9,3,"Fire",1],[9,5,"Nether Wart",1],[9,7,"Nether Wart",1]]
    // x, y, Crop Name, enum
    // enum values: 0 = target crop, 1 = ingredient, 2 = unique crop

    var ignoreWarnings = false
    var sentWarnings = false

    var borderPadding: Int = 6

    private var displayedGridWidget: GreenhouseGridWidget? = null
    private val greenhouseGridWidgets: MutableList<GreenhouseGridWidget> = mutableListOf()
    private val presetGridWidgets: MutableList<GreenhouseGridWidget> = mutableListOf()
    private var currentGreenhouseIndex = 0
    private val currentPresetIndex = 0

    var forwardArrow: ArrowWidget? = null
    var backwardArrow: ArrowWidget? = null

    var savedWidth: Int? = null
    var savedHeight: Int? = null

    override fun init() {
        super.init()
        sentWarnings = false
        initLayout()
        sentWarnings = true
    }

    fun initLayout(){
        displayedGridWidget = null
        greenhouseGridWidgets.clear()

        savedWidth = width
        savedHeight = height

        paddingY = height/10

        val slotSize = (height - paddingY * 2 - borderPadding * 2) / gridSize

        containerSize = (slotSize + 1) * gridSize

        startX = (width - containerSize) / 2
        startY = paddingY

        val amountInitialized = GreenhouseData.greenhouseGrids.count { it.state.initialized }
        if (PlotAPI.plots.any { it.data == null }) {
            if (!ignoreWarnings) {
                ChatUtils.sendWithPrefix("Plot data is null, please open configure plots in desk.")
            }
            return
        }
        if (amountInitialized != PlotAPI.plots.count { it.data?.isGreenhouse ?: throw IllegalStateException("Plot data was null after null check.") }){
            if (!ignoreWarnings){ //todo change to persistent
                ChatUtils.sendWithPrefix("The mod scans greenhouses after you've entered them.")
                ChatUtils.sendWithIgnoreClick("Not all greenhouses available, enter them to see them.")
            }
        }

        GreenhouseData.greenhouseGrids.forEach {
            if (!it.state.initialized) return@forEach
            val gridWidget = GreenhouseGridWidget(it, gridSize, slotSize).apply {
                widgetX = startX
                widgetY = startY
                widgetWidth = containerSize
                widgetHeight = containerSize
                init()
            }

            greenhouseGridWidgets.add(gridWidget)
        }

        currentGreenhouseIndex = greenhouseGridWidgets.indexOfFirst {
            it.grid.plot?.id == PlotAPI.getCurrentPlot()?.id
        }.takeIf { it != -1 } ?: 0

        displayedGridWidget = greenhouseGridWidgets.getOrNull(currentGreenhouseIndex)

        if (displayedGridWidget == null) {
            displayedGridWidget = greenhouseGridWidgets.firstOrNull()
        }
        if (displayedGridWidget == null) {
            //todo maybe add an auto switch to preset mode if not available.
            return
        }


        // normal = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join.png"),
        forwardArrow = ArrowWidget(
            x = (width / 2) + 10,
            y = startY + containerSize + borderPadding + 10,
            width = 22,
            normal = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join.png"),
            hovered = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_highlighted.png"),
            onClick = {
                gridWidgetChanged(1)
            }
        )

        backwardArrow = ArrowWidget(
            x = ((width) / 2) - 22 - 10,
            y = startY + containerSize + borderPadding + 10,
            width = 22,
            normal = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_backward.png"),
            hovered = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_backward_highlighted.png"),
            onClick = {
                gridWidgetChanged(-1)
            }
        )
    }


    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        if (savedWidth != width || savedHeight != height) {
            initLayout()
            return
        }


        super.render(graphics, mouseX, mouseY, delta)


        // background
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.fromNamespaceAndPath("minecraft", "popup/background"),
            startX - borderPadding,
            startY - borderPadding,
            containerSize + borderPadding * 2,
            containerSize + borderPadding * 2,
        )
        val displayedName =
            displayedGridWidget?.grid?.plot?.data?.name
                ?: displayedGridWidget?.grid?.plot?.id?.toString()
                ?: "Unknown greenhouse"


        graphics.drawMultilineBoxCentered(
            displayedName,
            width / 2,
            18
        )
        displayedGridWidget?.render(graphics, mouseX, mouseY, delta)

        forwardArrow?.render(graphics, mouseX, mouseY, delta)
        backwardArrow?.render(graphics, mouseX, mouseY, delta)

        hoveredWidget?.renderTooltip(
            graphics,
            startX + containerSize,
            startY + borderPadding *2)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (displayedGridWidget?.mouseClicked(mouseButtonEvent, doubled) == true) {
            return true
        }
        if (forwardArrow?.mouseClicked(mouseButtonEvent, doubled) == true) {
            return true
        }
        if (backwardArrow?.mouseClicked(mouseButtonEvent, doubled) == true) {
            return true
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredWidget = null
        displayedGridWidget?.mouseMoved(mouseX, mouseY)
    }


    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY)
    }

    fun gridWidgetChanged(direction: Int) {
        if (greenhouseGridWidgets.isEmpty()) return

        currentGreenhouseIndex = (currentGreenhouseIndex + direction).mod(greenhouseGridWidgets.size)

        displayedGridWidget = greenhouseGridWidgets[currentGreenhouseIndex]
    }

}