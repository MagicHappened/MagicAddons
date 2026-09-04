package org.magic.magicaddons.util

import org.magic.magicaddons.Common
import com.mojang.blaze3d.pipeline.RenderPipeline
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemDisplayContext
import org.joml.Matrix3x2f
import org.joml.Matrix4f
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

    /** A panel: the fill with the standard frame around it. Every box of this mod is one. */
    fun GuiGraphicsExtractor.drawPanel(
        x1: Int, y1: Int, x2: Int, y2: Int,
        fill: Int = Common.UI.BACKGROUND_COLOR,
        frame: Int = Common.UI.BORDER_COLOR
    ) {
        fill(x1, y1, x2, y2, fill)
        drawBorder(x1, y1, x2, y2, Common.UI.BORDER_SIZE, frame)
    }

    /** A panel that acts as a button: lit under the mouse, pressed in and framed white when picked. */
    fun GuiGraphicsExtractor.drawButtonPanel(
        x1: Int, y1: Int, x2: Int, y2: Int,
        hovered: Boolean,
        pressed: Boolean = false,
        fill: Int = Common.UI.BACKGROUND_COLOR
    ) {
        fill(x1, y1, x2, y2, fill)
        if (pressed) {
            fill(x1, y1, x2, y2, Common.UI.PRESSED_SHADE)
        } else if (hovered) {
            fill(x1, y1, x2, y2, Common.UI.HOVER_WASH)
        }
        drawBorder(x1, y1, x2, y2, Common.UI.BORDER_SIZE, if (pressed) Common.UI.SELECTED_FRAME_COLOR else Common.UI.BORDER_COLOR)
    }

    /** The ground of a text field or checkbox: a dark inset, framed white only while it has the keyboard. */
    fun GuiGraphicsExtractor.drawField(x1: Int, y1: Int, x2: Int, y2: Int, focused: Boolean) {
        fill(x1, y1, x2, y2, Common.UI.FIELD_COLOR)
        if (focused) drawBorder(x1, y1, x2, y2, Common.UI.BORDER_SIZE, Common.UI.SELECTED_FRAME_COLOR)
    }

    /** A red square with a white exclamation mark, for data that may be wrong. */
    fun GuiGraphicsExtractor.drawWarningBadge(x: Int, y: Int, size: Int) {
        drawPanel(x, y, x + size, y + size, Common.UI.WARNING_COLOR)

        val font = Minecraft.getInstance().font
        val mark = "!"
        val scale = size * 0.7f / font.lineHeight

        // the glyph's own middle, not the advance's: a glyph carries a spacing column on its right
        // and sits above the line's descent row
        val glyphCenterX = (font.width(mark) - 1) / 2f
        val glyphCenterY = (font.lineHeight - 2) / 2f

        pose().pushMatrix()
        pose().translate(x + size / 2f - glyphCenterX * scale, y + size / 2f - glyphCenterY * scale)
        pose().scale(scale, scale)
        text(font, Component.literal(mark), 0, 0, Common.UI.TEXT_COLOR, false)
        pose().popMatrix()
    }

    /** A scroll bar down a list of [total] rows showing [visible] of them from [scroll]. Display only. */
    fun GuiGraphicsExtractor.drawScrollBar(x: Int, y: Int, height: Int, total: Int, visible: Int, scroll: Int) {
        if (total <= visible || height <= 0) return

        val thumb = (height * visible / total).coerceAtLeast(6).coerceAtMost(height)
        val thumbY = y + (height - thumb) * scroll / (total - visible)

        fill(x, y, x + Common.UI.SCROLLBAR_WIDTH, y + height, Common.UI.SCROLL_TRACK_COLOR)
        fill(x, thumbY, x + Common.UI.SCROLLBAR_WIDTH, thumbY + thumb, Common.UI.TEXT_COLOR)
    }

    /** A frame just inside the rectangle: four strips that meet square at the corners. */
    fun GuiGraphicsExtractor.drawBorder(x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, color: Int) {
        fill(x1, y1, x2, y1 + thickness, color)
        fill(x1, y2 - thickness, x2, y2, color)
        fill(x1, y1, x1 + thickness, y2, color)
        fill(x2 - thickness, y1, x2, y2, color)
    }

    fun GuiGraphicsExtractor.drawBorder(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        thickness: Float,
        color: Int? = null
    ) {
        drawBorder(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), thickness.toInt(), color ?: Common.UI.TEXT_COLOR)
    }

    /** A panel with a quiet title in its top left, for a group of controls that belong together. */
    fun GuiGraphicsExtractor.drawShelf(x1: Int, y1: Int, x2: Int, y2: Int, title: String) {
        drawPanel(x1, y1, x2, y2)
        text(
            Minecraft.getInstance().font,
            Component.literal(title),
            x1 + Common.UI.TEXT_X_PAD,
            y1 + Common.UI.SPACING,
            Common.UI.TEXT_DIM_COLOR,
            false
        )
    }

    /** A filled rectangle with its corners taken off: a full height middle band and two inset ones. */
    fun GuiGraphicsExtractor.fillRounded(x1: Int, y1: Int, x2: Int, y2: Int, radius: Int, color: Int) {
        if (x2 <= x1 || y2 <= y1) return

        val corner = radius.coerceAtMost(minOf(x2 - x1, y2 - y1) / 2)

        fill(x1, y1 + corner, x2, y2 - corner, color)

        if (corner <= 0) return

        fill(x1 + corner, y1, x2 - corner, y1 + corner, color)
        fill(x1 + corner, y2 - corner, x2 - corner, y2, color)
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
        color: Int? = null
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (length == 0f) return

        val pose = Matrix3x2f(this.pose())

        pose.translate(x1, y1)
        pose.rotate(kotlin.math.atan2(dy, dx))

        // never thinner than one pixel: taking toInt() of half a thickness turned every line of
        // thickness 1 into a rectangle of no height, which drew nothing
        val half = thickness / 2f
        val y0 = kotlin.math.floor(-half).toInt()
        val y1 = kotlin.math.ceil(half).toInt()

        val actualColor = color ?: Common.UI.TEXT_COLOR

        this.guiRenderState.addGuiElement(
            ColoredRectangleRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                pose,
                0,
                y0,
                kotlin.math.round(length).toInt().coerceAtLeast(1),
                y1,
                actualColor,
                actualColor,
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
        val font = Minecraft.getInstance().font
        val lines = text.split('\n').flatMap { line ->
            font.split(Component.literal(line), TOOLTIP_MAX_WIDTH)
        }
        drawTooltipLines(lines, mouseX, mouseY)
    }

    /** Lines on a panel at the cursor, drawn at once and kept on screen. Call it last, it has no depth of its own. */
    fun GuiGraphicsExtractor.drawTooltipLines(lines: List<FormattedCharSequence>, mouseX: Int, mouseY: Int) {
        if (lines.isEmpty()) return
        val font = Minecraft.getInstance().font
        val pad = Common.UI.TEXT_X_PAD

        val boxWidth = lines.maxOf { font.width(it) } + pad * 2
        val boxHeight = lines.size * font.lineHeight + pad * 2

        val screen = McCompat.currentScreen()
        val screenWidth = screen?.width ?: Minecraft.getInstance().window.guiScaledWidth
        val screenHeight = screen?.height ?: Minecraft.getInstance().window.guiScaledHeight

        val x = mouseX.coerceAtMost(screenWidth - boxWidth).coerceAtLeast(0)
        val y = mouseY.coerceAtMost(screenHeight - boxHeight).coerceAtLeast(0)

        drawPanel(x, y, x + boxWidth, y + boxHeight)

        lines.forEachIndexed { index, line ->
            text(font, line, x + pad, y + pad + index * font.lineHeight, Common.UI.TEXT_COLOR, false)
        }
    }

    /** How tall [drawMultilineBox] draws [text], so screens can stack boxes under each other. */
    fun boxHeight(text: String): Int = computeLayout(text).boxHeight

    fun GuiGraphicsExtractor.drawMultilineBoxCentered(
        text: String,
        centerX: Int,
        centerY: Int,
        color: Int? = null
    ) {
        val layout = computeLayout(text)

        val x = centerX - layout.boxWidth / 2
        val y = centerY - layout.boxHeight / 2

        drawMultilineBox(text, x, y, color)
    }


    fun GuiGraphicsExtractor.drawMultilineBox(
        text: String,
        x: Int,
        y: Int,
        color: Int? = null
    ){
        drawMultilineBox(
            text,
            x.toFloat(),
            y.toFloat(),
            color
        )
    }


    fun GuiGraphicsExtractor.drawMultilineBox(
        text: String,
        x: Float,
        y: Float,
        color: Int? = null
    ) {
        val font = Minecraft.getInstance().font
        val padding = 4f

        val layout = computeLayout(text)

        val x1 = x
        val y1 = y
        val x2 = x + layout.boxWidth
        val y2 = y + layout.boxHeight

        drawPanel(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), frame = color ?: Common.UI.BORDER_COLOR)

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
                    Common.UI.TEXT_COLOR,
                    0,
                    false,
                    false,
                    scissorStack.peek()
                )
            )

            currentY += layout.lineHeight
        }
    }

    fun GuiGraphicsExtractor.renderFakeItem(
        stack: ItemStack,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        renderDecorations: Boolean = false
    ) {
        if (stack.isEmpty) return

        val mc = Minecraft.getInstance()

        val pose = this.pose()

        pose.pushMatrix()

        try {
            pose.translate(x.toFloat(), y.toFloat())

            val scaleX = width / 16.0f
            val scaleY = height / 16.0f
            val scale = minOf(scaleX, scaleY)

            pose.scale(scale, scale)

            this.item(stack, 0, 0)

            if (renderDecorations) {
                this.itemDecorations(mc.font, stack, 0, 0)
            }

        } finally {
            pose.popMatrix()
        }
    }

    fun getSpriteForState(state: BlockState, direction: Direction): TextureAtlasSprite {

        val client = Minecraft.getInstance()

        val model = client.modelManager.blockStateModelSet.get(state)

        val random = RandomSource.create(0)

        val parts: MutableList<BlockStateModelPart> = mutableListOf()

        model.collectParts(random, parts)

        parts.forEach { part ->
            val quads = part.getQuads(direction)
            if (quads.isNotEmpty()) {
                return quads.first().materialInfo.sprite
            }
        }

        parts.forEach { part ->
            val quads = part.getQuads(null)
            if (quads.size == 1){
                return quads[0].materialInfo.sprite
            }
            quads.forEach { quad ->
                if (quad.direction == direction) {
                    return quad.materialInfo.sprite
                }
            }
        }


        throw IllegalStateException("No sprite for state $state")
    }




}