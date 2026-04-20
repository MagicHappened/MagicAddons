package org.magic.magicaddons.util

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.texture.Sprite
import net.minecraft.client.texture.TextureSetup
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
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
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val padding = 4

        val lines = text.lines()
        val maxWidth = lines.maxOfOrNull { textRenderer.getWidth(it) } ?: 0
        val lineHeight = textRenderer.fontHeight
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

            if (MinecraftClient.getInstance().currentScreen !== target) {
                MinecraftClient.getInstance().setScreen(target)
            } else {
                newScreen = null
            }
        }
    }
    fun drawBorder(
        ctx: DrawContext,
        x1: Int, y1: Int,
        x2: Int, y2: Int,
        thickness: Int,
        color: Int
    ) {
        ctx.fill(x1, y1, x2, y1 + thickness, color)
        ctx.fill(x1, y2 - thickness, x2, y2, color)
        ctx.fill(x1, y1, x1 + thickness, y2, color)
        ctx.fill(x2 - thickness, y1, x2, y2, color)
    }

    fun drawSquareBorder(ctx: DrawContext, x: Int, y: Int, size: Int, thickness: Int, color: Int) {
        ctx.fill(x, y, x + size, y + thickness, color)
        ctx.fill(x, y + size - thickness, x + size, y + size, color)
        ctx.fill(x, y, x + thickness, y + size, color)
        ctx.fill(x + size - thickness, y, x + size, y + size, color)
    }

    fun GuiRenderState.drawLine(
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

    fun GuiRenderState.drawLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        color: Int
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        if (len == 0f) return

        val pose = Matrix3x2f()

        pose.translate(x1, y1)
        pose.rotate(kotlin.math.atan2(dy, dx))

        val half = thickness / 2f

        addSimpleElement(
            ColoredQuadGuiElementRenderState(
                RenderPipelines.GUI,
                TextureSetup.empty(),
                pose,
                0, -half.toInt(),
                len.toInt(), half.toInt(),
                color,
                color,
                null
            )
        )
    }

    fun drawMultilineBox(
        ctx: DrawContext,
        text: String,
        x: Int,
        y: Int
    ) {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val padding = 4

        val layout = computeLayout(text)

        val x1 = x
        val y1 = y
        val x2 = x + layout.boxWidth
        val y2 = y + layout.boxHeight

        // background
        ctx.fill(x1, y1, x2, y2, 0x88000000.toInt())

        // border
        drawBorder(ctx, x1, y1, x2, y2, 1, 0xFFFFFFFF.toInt())

        // draw centered lines
        var currentY = y + padding
        layout.lines.forEach { line ->
            val lineWidth = textRenderer.getWidth(line)
            val centeredX = x + (layout.boxWidth - lineWidth) / 2

            ctx.drawText(
                textRenderer,
                line,
                centeredX,
                currentY,
                0xFFFFFFFF.toInt(),
                false
            )

            currentY += layout.lineHeight
        }
    }

    fun drawMultilineBoxCentered(
        ctx: DrawContext,
        text: String,
        centerX: Int,
        centerY: Int
    ) {
        val layout = computeLayout(text)

        val x = centerX - layout.boxWidth / 2
        val y = centerY - layout.boxHeight / 2

        drawMultilineBox(ctx, text, x, y)
    }

    fun getTopSpriteForState(state: BlockState?): Sprite? {
        val client = MinecraftClient.getInstance()

        val model = client.blockRenderManager.models.getModel(state)

        val parts = model.getParts(Random.create())

        for (part in parts) {
            val quads = part.getQuads(Direction.UP)
            if (quads.isNotEmpty()) {
                return quads[0].sprite
            }
        }

        for (part in parts) {
            val quads = part.getQuads(null)
            if (quads.isNotEmpty()) {
                return quads[0].sprite
            }
        }

        return null
    }

}