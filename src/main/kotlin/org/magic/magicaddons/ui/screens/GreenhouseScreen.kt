package org.magic.magicaddons.ui.screens

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.magic.magicaddons.features.farming.GreenhousePresets
import org.magic.magicaddons.ui.widgets.ArrowWidget
import org.magic.magicaddons.ui.widgets.greenhouse.GreenhouseGridWidget
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

class GreenhouseScreen(title: Text) : Screen(title) {

    private val paddingY: Int = 40

    private var startX: Int = 0
    private var startY: Int = 0
    private var containerSize: Int = 400

    private var gridWidget: GreenhouseGridWidget? = null

    var forward_arrow: ArrowWidget? = null
    var backward_arrow: ArrowWidget? = null

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

        forward_arrow = ArrowWidget(
            x = (width + 20) / 2,
            y = startY + containerSize + 10,
            normal = Identifier.of("magicaddons", "gui/join"),
            hovered = Identifier.of("magicaddons", "gui/join_highlighted")
        ) {
            ChatUtils.sendWithPrefix("Forward arrow")
        }

        backward_arrow = ArrowWidget(
            x = (width - 20) / 2,
            y = startY + containerSize + 10,
            normal = Identifier.of("magicaddons", "gui/join_backward"),
            hovered = Identifier.of("magicaddons", "gui/join_backward_highlighted")
        ) {
            ChatUtils.sendWithPrefix("Backwards arrow")
        }



    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        // background
        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            Identifier.of("minecraft", "popup/background"),
            startX,
            startY,
            containerSize,
            containerSize
        )

        ScreenUtil.drawMultilineBoxCentered(
            context,
            "Greenhouse 1",
            width / 2,
            18
        )
        gridWidget?.render(context, mouseX, mouseY, delta)

        forward_arrow?.render(context, mouseX, mouseY, delta)
        backward_arrow?.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(click: Click?, doubled: Boolean): Boolean {
        if (click != null && gridWidget?.mouseClicked(click, doubled) == true) {
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY)
    }
}