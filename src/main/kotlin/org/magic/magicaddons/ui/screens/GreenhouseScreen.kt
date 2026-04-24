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
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered

class GreenhouseScreen(title: Component) : Screen(title) {


    private val gridSize = 10

    private var paddingY: Int = 40
    private var startX: Int = 0
    private var startY: Int = 0
    private var containerSize: Int = 400


    var borderPadding: Int = 6

    private var gridWidget: GreenhouseGridWidget? = null

    var forwardArrow: ArrowWidget? = null
    var backwardArrow: ArrowWidget? = null


    override fun init() {
        super.init()
        paddingY = height/10

        val slotSize = (height - paddingY * 2 - borderPadding * 2) / gridSize

        containerSize = (slotSize) * gridSize

        startX = (width - containerSize) / 2
        startY = paddingY

        if (GreenhousePresets.initializedGreenhouseIds.isEmpty()) {
            ChatUtils.sendWithPrefix("No initialized greenhouse ids, please enter your greenhouse.")
            gridWidget = null
            return
        }

        val grid = GreenhousePresets.greenhouseList.firstOrNull()
        if (grid == null) {
            gridWidget = null
            return
        }

        gridWidget = GreenhouseGridWidget(grid,gridSize,slotSize).apply {
            widgetX = startX
            widgetY = startY
            widgetWidth = containerSize
            widgetHeight = containerSize
            init()
        }
        // normal = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join.png"),
        forwardArrow = ArrowWidget(
            x = (width / 2) + 10,
            y = startY + containerSize + borderPadding + 10,
            width = 22,
            normal = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join.png"),
            hovered = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_highlighted.png"),
            onClick = {
                ChatUtils.sendWithPrefix("Forward arrow")
            }
        )

        backwardArrow = ArrowWidget(
            x = ((width) / 2) - 22 - 10,
            y = startY + containerSize + borderPadding + 10,
            width = 22,
            normal = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_backward.png"),
            hovered = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_backward_highlighted.png"),
            onClick = {
                ChatUtils.sendWithPrefix("Backwards arrow")
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
            containerSize + borderPadding * 2 + 1,
            containerSize + borderPadding * 2 + 1,
        )

        graphics.drawMultilineBoxCentered(
            "Greenhouse 1",
            width / 2,
            18
        )
        gridWidget?.render(graphics, mouseX, mouseY, delta)

        forwardArrow?.render(graphics, mouseX, mouseY, delta)
        backwardArrow?.render(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        if (gridWidget?.mouseClicked(mouseButtonEvent, doubled) == true) {
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
}