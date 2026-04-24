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

    private val paddingY: Int = 40

    private var startX: Int = 0
    private var startY: Int = 0
    private var containerSize: Int = 400

    private var gridWidget: GreenhouseGridWidget? = null

    var forwardArrow: ArrowWidget? = null
    var backwardArrow: ArrowWidget? = null

    override fun init() {
        super.init()

        containerSize = height - paddingY * 2
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

        gridWidget = GreenhouseGridWidget(grid).apply {
            widgetX = startX
            widgetY = startY
            widgetWidth = containerSize
            widgetHeight = containerSize
            init()
        }

        forwardArrow = ArrowWidget(
            x = (width + 20) / 2,
            y = startY + containerSize + 10,
            normal = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join.png"),
            hovered = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_highlighted.png")
        ) {
            ChatUtils.sendWithPrefix("Forward arrow")
        }

        backwardArrow = ArrowWidget(
            x = (width - 20) / 2,
            y = startY + containerSize + 10,
            normal = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_backward.png"),
            hovered = Identifier.fromNamespaceAndPath("magicaddons", "textures/gui/join_backward_highlighted.png")
        ) {
            ChatUtils.sendWithPrefix("Backwards arrow")
        }



    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(graphics, mouseX, mouseY, delta)

        // background
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.fromNamespaceAndPath("minecraft", "popup/background"),
            startX,
            startY,
            containerSize,
            containerSize
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
        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY)
    }
}