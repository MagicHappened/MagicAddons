package org.magic.magicaddons.util

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.network.chat.Component
import org.joml.Matrix3x2f
import org.magic.magicaddons.util.compat.McCompat

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

            if (McCompat.currentScreen() !== target) {
                McCompat.setScreen(target)
            } else {
                newScreen = null
            }
        }
    }

    fun GuiGraphicsExtractor.drawBorder(x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, color: Int) {
        drawBorder(
            x1.toFloat(),
            y1.toFloat(),
            x2.toFloat(),
            y2.toFloat(),
            thickness.toFloat(),
            color
        )
    }


    fun GuiGraphicsExtractor.drawBorder(
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

    fun GuiGraphicsExtractor.drawSquareBorder(
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

    fun GuiGraphicsExtractor.drawLine(
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

    fun GuiGraphicsExtractor.drawLine(
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

        // rounded to whole pixels and never thinner than one: taking toInt() of half a thickness
        // turned every line of thickness 1 into a rectangle of no height, which drew nothing
        val pixels = kotlin.math.round(thickness).toInt().coerceAtLeast(1)
        val top = -(pixels / 2)

        this.guiRenderState.addGuiElement(
            ColoredRectangleRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                pose,
                0,
                top,
                kotlin.math.round(length).toInt().coerceAtLeast(1),
                top + pixels,
                color,
                color,
                this.scissorStack.peek()
            )
        )
    }

    /** How tall [text] is once wrapped to [maxWidth]. */
    fun wrappedHeight(font: Font, text: Component, maxWidth: Int): Int =
        font.wordWrapHeight(text, maxWidth.coerceAtLeast(font.width("W")))

    /** Draws [text] wrapped to [maxWidth], one line under the other. Returns the height used. */
    fun GuiGraphicsExtractor.drawWrappedText(
        font: Font,
        text: Component,
        x: Int,
        y: Int,
        maxWidth: Int,
        color: Int,
        shadow: Boolean = false
    ): Int {
        var currentY = y
        font.split(text, maxWidth.coerceAtLeast(font.width("W"))).forEach { line ->
            text(font, line, x, currentY, color, shadow)
            currentY += font.lineHeight
        }
        return currentY - y
    }

    /** Tooltips wrap at this width, the same as vanilla's own widget tooltips. */
    private const val TOOLTIP_MAX_WIDTH = 170

    /** A tooltip split on newlines and wrapped at vanilla's width, colour codes honoured. */
    fun GuiGraphicsExtractor.drawSimpleTooltip(text: String, mouseX: Int, mouseY: Int) {
        val client = Minecraft.getInstance()
        val lines = text.split('\n').flatMap { line ->
            client.font.split(Component.literal(line), TOOLTIP_MAX_WIDTH)
        }.map { ClientTooltipComponent.create(it) }

        this.tooltip(
            client.font,
            lines,
            mouseX,
            mouseY,
            DefaultTooltipPositioner.INSTANCE,
            null
        )
    }

    /** How tall [drawMultilineBox] draws [text], so screens can stack boxes under each other. */
    fun boxHeight(text: String): Int = computeLayout(text).boxHeight

    fun GuiGraphicsExtractor.drawMultilineBoxCentered(
        text: String,
        centerX: Int,
        centerY: Int
    ) {
        val layout = computeLayout(text)

        val x = centerX - layout.boxWidth / 2
        val y = centerY - layout.boxHeight / 2

        drawMultilineBox(text, x, y)
    }


    fun GuiGraphicsExtractor.drawMultilineBox(
        text: String,
        x: Int,
        y: Int,
    ){
        drawMultilineBox(
            text,
            x.toFloat(),
            y.toFloat(),
        )
    }


    fun GuiGraphicsExtractor.drawMultilineBox(
        text: String,
        x: Float,
        y: Float
    ) {
        val font = Minecraft.getInstance().font
        val padding = 4f

        val layout = computeLayout(text)

        val x1 = x
        val y1 = y
        val x2 = x + layout.boxWidth
        val y2 = y + layout.boxHeight

        fill(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), 0x88000000.toInt())

        drawBorder(x1, y1, x2, y2, 1f, 0xFFFFFFFF.toInt())

        // text
        var currentY = y + padding

        layout.lines.forEach { line ->
            val seq = Component.literal(line).visualOrderText
            val centeredX = x + (layout.boxWidth - font.width(line)) / 2f

            guiRenderState.addText(
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