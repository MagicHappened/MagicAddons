package org.magic.magicaddons.util

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.ColoredRectangleRenderState
import net.minecraft.client.gui.render.state.GuiTextRenderState
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.joml.Matrix3x2f

object ScreenUtil {

    private data class TextBoxLayout(
        val lines: List<String>,
        val maxWidth: Int,
        val lineHeight: Int,
        val totalHeight: Int,
        val boxWidth: Int,
        val boxHeight: Int
    )

    private fun computeLayout(text: String): TextBoxLayout {
        val textRenderer = Minecraft.getInstance().font
        val padding = 4

        val lines = text.lines()
        val maxWidth = lines.maxOfOrNull { textRenderer.width(it) } ?: 0
        val lineHeight = textRenderer.lineHeight
        val totalHeight = lines.size * lineHeight

        val boxWidth = maxWidth + padding * 2
        val boxHeight = totalHeight + padding * 2

        return TextBoxLayout(
            lines,
            maxWidth,
            lineHeight,
            totalHeight,
            boxWidth,
            boxHeight
        )
    }


    private var newScreen: Screen? = null

    fun setScreen(screen: Screen) {
        newScreen = screen
    }

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            val target = newScreen ?: return@register

            if (Minecraft.getInstance().screen !== target) {
                Minecraft.getInstance().setScreen(target)
            } else {
                newScreen = null
            }
        }
    }

    fun GuiGraphics.drawBorder(x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, color: Int) {
        drawBorder(
            x1.toFloat(),
            y1.toFloat(),
            x2.toFloat(),
            y2.toFloat(),
            thickness.toFloat(),
            color
        )
    }


    fun GuiGraphics.drawBorder(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        thickness: Float,
        color: Int
    ) {
        // top
        drawLine(x1, y1, x2, y1, thickness, color)

        // bottom
        drawLine(x1, y2, x2, y2, thickness, color)

        // left
        drawLine(x1, y1, x1, y2, thickness, color)

        // right
        drawLine(x2, y1, x2, y2, thickness, color)
    }

    fun GuiGraphics.drawSquareBorder(
        x: Float,
        y: Float,
        size: Float,
        thickness: Float,
        color: Int
    ) {
        drawBorder(
            x,
            y,
            x + size,
            y + size,
            thickness,
            color
        )
    }

    fun GuiGraphics.drawLine(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        thickness: Int,
        color: Int
    ) {
        drawLine(
            x1.toFloat(),
            y1.toFloat(),
            x2.toFloat(),
            y2.toFloat(),
            thickness.toFloat(),
            color
        )
    }

    fun GuiGraphics.drawLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        color: Int
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (length == 0f) return

        val pose = Matrix3x2f(this.pose())

        pose.translate(x1, y1)
        pose.rotate(kotlin.math.atan2(dy, dx))

        val half = thickness / 2f

        this.guiRenderState.submitGuiElement(
            ColoredRectangleRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                pose,
                0,
                -half.toInt(),
                length.toInt(),
                half.toInt(),
                color,
                color,
                this.scissorStack.peek()
            )
        )
    }

    fun GuiGraphics.drawSimpleTooltip(text: String, mouseX: Int, mouseY: Int) {
        val client = Minecraft.getInstance()
        val lines = listOf(
            ClientTooltipComponent.create(
                Component.literal(text).visualOrderText
            )
        )

        this.renderTooltip(
            client.font,
            lines,
            mouseX + 8,
            mouseY + 8,
            DefaultTooltipPositioner.INSTANCE,
            null
        )
    }

    fun GuiGraphics.drawMultilineBoxCentered(
        text: String,
        centerX: Int,
        centerY: Int,
    ){
        drawMultilineBoxCentered(
            text,
            centerX.toFloat(),
            centerY.toFloat(),
        )
    }


    fun GuiGraphics.drawMultilineBoxCentered(
        text: String,
        centerX: Float,
        centerY: Float
    ) {
        val font = Minecraft.getInstance().font
        val padding = 4f

        val layout = computeLayout(text)

        val x1 = centerX
        val y1 = centerY
        val x2 = centerX + layout.boxWidth
        val y2 = centerY + layout.boxHeight

        fill(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), 0x88000000.toInt())

        drawBorder(x1, y1, x2, y2, 1f, 0xFFFFFFFF.toInt())

        // text
        var currentY = centerY + padding

        layout.lines.forEach { line ->
            val seq = Component.literal(line).visualOrderText
            val centeredX = centerX + (layout.boxWidth - font.width(line)) / 2f

            guiRenderState.submitText(
                GuiTextRenderState(
                    font,
                    seq,
                    Matrix3x2f(pose()),
                    centeredX.toInt(),
                    currentY.toInt(),
                    0xFFFFFFFF.toInt(),
                    0,
                    false,
                    false,
                    scissorStack.peek()
                )
            )
            currentY += layout.lineHeight
        }
    }

}