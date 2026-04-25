package org.magic.magicaddons.ui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.magic.magicaddons.features.farming.GreenhousePresets
import org.magic.magicaddons.ui.widgets.ArrowWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhouseGridWidget
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

class GreenhouseScreen(title: Component) : Screen(title) {


    private val gridSize = 10

    private var paddingY: Int = 40
    private var startX: Int = 0
    private var startY: Int = 0
    private var containerSize: Int = 400

    var ignoreWarnings = false

    var borderPadding: Int = 6

    private var displayedGridWidget: GreenhouseGridWidget? = null
    private val gridWidgets: MutableList<GreenhouseGridWidget> = mutableListOf()
    private var currentIndex = 0


    var forwardArrow: ArrowWidget? = null
    var backwardArrow: ArrowWidget? = null


    override fun init() {
        super.init()
        paddingY = height/10

        val slotSize = (height - paddingY * 2 - borderPadding * 2) / gridSize

        containerSize = (slotSize + 1) * gridSize

        startX = (width - containerSize) / 2
        startY = paddingY

        if (GreenhousePresets.initializedGreenhouseIds.isEmpty()) {
            ChatUtils.sendWithPrefix("No initialized greenhouse ids, please enter your greenhouse.")
            displayedGridWidget = null
            return
        }
        if (GreenhousePresets.knownGreenhouseIds.size != GreenhousePresets.initializedGreenhouseIds.size) {
            ChatUtils.sendWithPrefix("Not all greenhouses initialized, enter the other greenhouses to see them.")
        }
        GreenhousePresets.greenhouseList.forEach {
            val gridWidget = GreenhouseGridWidget(it,gridSize,slotSize).apply {
                widgetX = startX
                widgetY = startY
                widgetWidth = containerSize
                widgetHeight = containerSize
                init()
            }

            gridWidgets.add(gridWidget)
        }

        currentIndex = gridWidgets.indexOfFirst {
            it.grid.plot?.id == PlotAPI.getCurrentPlot()?.id
        }.takeIf { it != -1 } ?: 0

        displayedGridWidget = gridWidgets.getOrNull(currentIndex)

        if (displayedGridWidget == null) {
            displayedGridWidget = gridWidgets.firstOrNull()
        }
        if (displayedGridWidget == null) {
            ChatUtils.sendWithPrefix("No greenhouses to display.")
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
                ChatUtils.sendWithPrefix("Moved ahead 1 widget")
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
                ChatUtils.sendWithPrefix("Moved back 1 widget")
            }
        )



    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
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

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY)
    }

    fun gridWidgetChanged(direction: Int) {
        if (gridWidgets.isEmpty()) return

        currentIndex = (currentIndex + direction).mod(gridWidgets.size)

        displayedGridWidget = gridWidgets[currentIndex]
    }

}