package org.magic.magicaddons.util

import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.EventBus
import org.joml.Matrix3x2fc
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import net.minecraft.client.gui.navigation.ScreenRectangle
import com.mojang.blaze3d.vertex.VertexConsumer
import org.magic.magicaddons.Common
import org.magic.magicaddons.features.customization.Customization
import com.mojang.blaze3d.pipeline.RenderPipeline
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemDisplayContext
import org.joml.Matrix3x2f
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.render.ItemIconRenderState
import org.magic.magicaddons.util.compat.McCompat

object ScreenUtil {

    private class TextBoxLayout(val lines: List<String>, val boxWidth: Int, val boxHeight: Int)

    private fun computeLayout(text: String): TextBoxLayout {
        val font = Minecraft.getInstance().font
        val lines = text.lines()
        val maxWidth = lines.maxOfOrNull { font.width(it) } ?: 0

        return TextBoxLayout(lines, maxWidth + BOX_PAD * 2, lines.size * font.lineHeight + BOX_PAD * 2)
    }

    private var newScreen: Screen? = null

    fun setScreen(screen: Screen) {
        newScreen = screen
    }

    fun register() {
        EventBus.register(this)
    }

    @EventHandler
    fun onTick(event: WorldTickEvent) {
        val target = newScreen ?: return

        if (McCompat.currentScreen() !== target) {
            McCompat.setScreen(target)
        } else {
            newScreen = null
        }
    }

    /** Whether ([mouseX], [mouseY]) lies in the rectangle from ([x], [y]) of [width] by [height]. */
    fun inRect(mouseX: Double, mouseY: Double, x: Int, y: Int, width: Int, height: Int): Boolean =
        inRect(mouseX.toInt(), mouseY.toInt(), x, y, width, height)

    fun inRect(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Boolean =
        mouseX in x until x + width && mouseY in y until y + height

    /** The same event at another point, for a screen that draws in its own coordinates. */
    fun MouseButtonEvent.at(x: Double, y: Double): MouseButtonEvent =
        MouseButtonEvent(x, y, MouseButtonInfo(button(), modifiers()))

    operator fun IntArray.component4(): Int = this[3]

    /**
     * Writing on one of the mod's screens. Every one of them draws through this, so the text shadow
     * setting reaches all of it rather than the places that happened to ask for a shadow.
     */
    fun GuiGraphicsExtractor.modText(font: Font, text: Component, x: Int, y: Int, color: Int) {
        text(font, inModFont(text), x, y, color, Customization.textShadow)
    }

    fun GuiGraphicsExtractor.modText(font: Font, text: String, x: Int, y: Int, color: Int) {
        modText(font, Component.literal(text), x, y, color)
    }

    /** The same, for one line of text already wrapped by the font; see [splitMod] for the wrapping. */
    fun GuiGraphicsExtractor.modText(font: Font, text: FormattedCharSequence, x: Int, y: Int, color: Int) {
        text(font, text, x, y, color, Customization.textShadow)
    }

    /** [text] in the font the settings pick, when they pick one other than the game's own. */
    fun inModFont(text: Component): Component {
        val fontId = Customization.fontId ?: return text

        return text.copy().withStyle { style -> style.withFont(FontDescription.Resource(fontId)) }
    }

    /** Wraps text for the mod's screens, in the picked font, so every wrapped line carries it. */
    fun Font.splitMod(text: Component, width: Int): List<FormattedCharSequence> = split(inModFont(text), width)

    /** [text] when it fits in [room], else cut to fit with an ellipsis on the end. */
    fun ellipsised(font: Font, text: String, room: Int): String =
        if (font.width(text) <= room) text
        else font.plainSubstrByWidth(text, (room - font.width(ELLIPSIS)).coerceAtLeast(0)) + ELLIPSIS

    /** [scroll] moved one row by a wheel turn of [scrollY], kept within [total] rows of which [visible] show. */
    fun stepScroll(scroll: Int, scrollY: Double, total: Int, visible: Int): Int =
        (scroll - scrollY.toInt().coerceIn(-1, 1)).coerceIn(0, (total - visible).coerceAtLeast(0))

    /** The item that stands for a crop: its display item, its skyblock item, or a barrier when neither exists. */
    fun stackFor(def: CropDefinition): ItemStack =
        def.displayItem?.let { ItemStack(it) }
            ?: def.skyblockId?.toItem()?.takeUnless { it.isEmpty }
            ?: ItemStack(Items.BARRIER)

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
        fill: Int = Common.UI.BACKGROUND_COLOR,
        frame: Int = Common.UI.BORDER_COLOR,
        frameSize: Int = Common.UI.BORDER_SIZE
    ) {
        fill(x1, y1, x2, y2, fill)
        if (pressed) {
            fill(x1, y1, x2, y2, Common.UI.PRESSED_SHADE)
        } else if (hovered) {
            fill(x1, y1, x2, y2, Common.UI.HOVER_WASH)
        }
        drawBorder(x1, y1, x2, y2, frameSize, if (pressed) Common.UI.SELECTED_FRAME_COLOR else frame)
    }

    /** The ground of a text field or checkbox: a dark inset, framed white only while it has the keyboard. */
    fun GuiGraphicsExtractor.drawField(x1: Int, y1: Int, x2: Int, y2: Int, focused: Boolean, frameSize: Int = Common.UI.BORDER_SIZE) {
        fill(x1, y1, x2, y2, Common.UI.FIELD_COLOR)
        if (focused) drawBorder(x1, y1, x2, y2, frameSize, Common.UI.SELECTED_FRAME_COLOR)
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

    /**
     * A frame just inside the rectangle: four strips that meet square at the corners, handed to the
     * gui as one element rather than four, since the gui checks every element against the others.
     */
    fun GuiGraphicsExtractor.drawBorder(x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, color: Int) {
        fillShape(
            floatArrayOf(
                x1f(x1), x1f(y1), x1f(x1), x1f(y1 + thickness), x1f(x2), x1f(y1 + thickness), x1f(x2), x1f(y1),
                x1f(x1), x1f(y2 - thickness), x1f(x1), x1f(y2), x1f(x2), x1f(y2), x1f(x2), x1f(y2 - thickness),
                x1f(x1), x1f(y1), x1f(x1), x1f(y2), x1f(x1 + thickness), x1f(y2), x1f(x1 + thickness), x1f(y1),
                x1f(x2 - thickness), x1f(y1), x1f(x2 - thickness), x1f(y2), x1f(x2), x1f(y2), x1f(x2), x1f(y1)
            ),
            color
        )
    }

    private fun x1f(n: Int): Float = n.toFloat()

    /**
     * Two greys in small squares, the way image editors show see-through: what stands for air. Two
     * gui elements however big, the dark ground and the light squares together.
     */
    fun GuiGraphicsExtractor.drawCheckerboard(x1: Int, y1: Int, x2: Int, y2: Int) {
        if (x2 <= x1 || y2 <= y1) return
        fill(x1, y1, x2, y2, CHECKER_DARK)
        val squares = mutableListOf<Float>()
        var row = 0
        var top = y1
        while (top < y2) {
            val bottom = minOf(top + CHECKER_SQUARE, y2)
            var column = 0
            var left = x1
            while (left < x2) {
                val right = minOf(left + CHECKER_SQUARE, x2)
                if ((row + column) % 2 == 0) {
                    squares += listOf(left.toFloat(), top.toFloat(), left.toFloat(), bottom.toFloat(), right.toFloat(), bottom.toFloat(), right.toFloat(), top.toFloat())
                }
                left = right
                column++
            }
            top = bottom
            row++
        }
        fillShape(squares.toFloatArray(), CHECKER_LIGHT)
    }

    private const val CHECKER_SQUARE: Int = 4
    private const val CHECKER_DARK: Int = 0xFF6E6E6E.toInt()
    private const val CHECKER_LIGHT: Int = 0xFFB4B4B4.toInt()

    /** A right angled triangle with the corner at ([cornerX], [cornerY]) and legs of [size], one element. */
    fun GuiGraphicsExtractor.fillCornerTriangle(cornerX: Int, cornerY: Int, size: Int, color: Int) {
        val cx = cornerX.toFloat()
        val cy = cornerY.toFloat()
        // a quad whose last two points coincide, which draws as a triangle
        fillShape(floatArrayOf(cx - size, cy, cx, cy + size, cx, cy + size, cx, cy), color)
    }

    /**
     * Any number of quads of one colour as a single gui element: four points a quad, walked top
     * left, bottom left, bottom right, top right, as the gui's own rectangles are.
     */
    fun GuiGraphicsExtractor.fillShape(points: FloatArray, color: Int) {
        if (points.size < 8) return
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in points.indices step 2) {
            minX = minOf(minX, points[i]); maxX = maxOf(maxX, points[i])
            minY = minOf(minY, points[i + 1]); maxY = maxOf(maxY, points[i + 1])
        }
        val pose = Matrix3x2f(this.pose())
        val scissor = this.scissorStack.peek()
        val bounds = ScreenRectangle(minX.toInt(), minY.toInt(), kotlin.math.ceil(maxX - minX).toInt(), kotlin.math.ceil(maxY - minY).toInt())
            .transformAxisAligned(pose)
            .let { if (scissor == null) it else scissor.intersection(it) }
            ?: return
        this.guiRenderState.addGuiElement(ShapeRenderState(pose, points, color, scissor, bounds))
    }

    /** Several quads of one colour that the gui lays out as one element. */
    private class ShapeRenderState(
        private val pose: Matrix3x2fc,
        private val points: FloatArray,
        private val color: Int,
        private val scissor: ScreenRectangle?,
        private val bounds: ScreenRectangle
    ) : GuiElementRenderState {
        override fun buildVertices(consumer: VertexConsumer) {
            for (i in points.indices step 2) {
                consumer.addVertexWith2DPose(pose, points[i], points[i + 1]).setColor(color)
            }
        }

        override fun pipeline(): RenderPipeline = RenderPipelines.GUI
        override fun textureSetup(): TextureSetup = TextureSetup.noTexture()
        override fun scissorArea(): ScreenRectangle? = scissor
        override fun bounds(): ScreenRectangle = bounds
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

    /** A pill: a rectangle whose ends are half circles, drawn a row at a time. A square makes a circle. */
    fun GuiGraphicsExtractor.fillPill(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        val height = y2 - y1
        if (height <= 0 || x2 <= x1) return

        val radius = height / 2f
        for (row in 0 until height) {
            val dy = row + 0.5f - radius
            val half = kotlin.math.sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
            val left = kotlin.math.round(x1 + radius - half).toInt()
            val right = kotlin.math.round(x2 - radius + half).toInt()
            if (right > left) fill(left, y1 + row, right, y1 + row + 1, color)
        }
    }

    /** How far along an animation of [durationMs] started at [startedAt] is, eased so it lands softly. */
    /** [color] with its alpha scaled by [fraction], for something fading in or out. */
    fun withAlpha(color: Int, fraction: Float): Int {
        val alpha = (((color ushr 24) and 0xFF) * fraction.coerceIn(0f, 1f)).toInt()

        return (color and 0xFFFFFF) or (alpha shl 24)
    }

    fun eased(startedAt: Long, durationMs: Long): Float {
        val linear = ((System.currentTimeMillis() - startedAt) / durationMs.toFloat()).coerceIn(0f, 1f)
        val back = 1f - linear
        return 1f - back * back * back
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

    /** A straight line of [thickness] from ([x1], [y1]) to ([x2], [y2]), as one gui element. */
    fun GuiGraphicsExtractor.drawLine(x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int, color: Int) {
        val dx = (x2 - x1).toFloat()
        val dy = (y2 - y1).toFloat()
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (length == 0f) return

        val pose = Matrix3x2f(this.pose())

        pose.translate(x1.toFloat(), y1.toFloat())
        pose.rotate(kotlin.math.atan2(dy, dx))

        // never thinner than one pixel: taking toInt() of half a thickness turned every line of
        // thickness 1 into a rectangle of no height, which drew nothing
        val half = thickness / 2f
        val top = kotlin.math.floor(-half).toInt()
        val bottom = kotlin.math.ceil(half).toInt()

        this.guiRenderState.addGuiElement(
            ColoredRectangleRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                pose,
                0,
                top,
                kotlin.math.round(length).toInt().coerceAtLeast(1),
                bottom,
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
        font.splitMod(text, maxWidth.coerceAtLeast(font.width("W"))).forEach { line ->
            text(font, line, x, currentY, color, shadow)
            currentY += font.lineHeight
        }
        return currentY - y
    }

    /** Tooltips wrap at this width, the same as vanilla's own widget tooltips. */
    private const val TOOLTIP_MAX_WIDTH = 170

    /** Room between a tooltip's frame and its text. */
    const val TOOLTIP_PAD: Int = Common.UI.TEXT_X_PAD

    /** How far a tooltip sits from the cursor, so the pointer does not cover its first line. */
    const val CURSOR_TOOLTIP_X: Int = 7
    const val CURSOR_TOOLTIP_Y: Int = 12

    /** An item drawn larger than this is rendered at size rather than stretched from sixteen. */
    private const val CRISP_ITEM_ABOVE = 16

    /** A tooltip split on newlines and wrapped at vanilla's width, colour codes honoured. */
    fun GuiGraphicsExtractor.drawSimpleTooltip(text: String, x: Int, y: Int, maxWidth: Int = TOOLTIP_MAX_WIDTH): IntArray =
        drawTooltipLines(wrapTooltip(text, maxWidth), x, y)

    /** [drawSimpleTooltip] beside the cursor. */
    fun GuiGraphicsExtractor.drawTooltipAtCursor(text: String, mouseX: Int, mouseY: Int, maxWidth: Int = TOOLTIP_MAX_WIDTH): IntArray =
        drawSimpleTooltip(text, mouseX + CURSOR_TOOLTIP_X, mouseY + CURSOR_TOOLTIP_Y, maxWidth)

    /** [drawTooltipLines] beside the cursor. */
    fun GuiGraphicsExtractor.drawTooltipLinesAtCursor(
        lines: List<FormattedCharSequence>,
        mouseX: Int,
        mouseY: Int,
        icons: (Int) -> List<ItemStack> = { emptyList() }
    ): IntArray =
        drawTooltipLines(lines, mouseX + CURSOR_TOOLTIP_X, mouseY + CURSOR_TOOLTIP_Y, icons)

    private fun wrapTooltip(text: String, maxWidth: Int): List<FormattedCharSequence> {
        val font = Minecraft.getInstance().font
        return text.split('\n').flatMap { line ->
            font.splitMod(Component.literal(line), maxWidth.coerceAtLeast(font.width("W")))
        }
    }

    /**
     * Lines on a panel at ([x], [y]), moved to stay on screen. Call it last, it has no depth of its
     * own. [icons] gives the items drawn text-high before a line. Returns the panel's rectangle.
     */
    fun GuiGraphicsExtractor.drawTooltipLines(
        lines: List<FormattedCharSequence>,
        x: Int,
        y: Int,
        icons: (Int) -> List<ItemStack> = { emptyList() }
    ): IntArray {
        if (lines.isEmpty()) return intArrayOf(x, y, x, y)
        val font = Minecraft.getInstance().font
        val icon = font.lineHeight
        val iconStep = icon + Common.UI.SPACING_SMALL

        val boxWidth = lines.indices.maxOf { icons(it).size * iconStep + font.width(lines[it]) } + TOOLTIP_PAD * 2
        val boxHeight = lines.size * font.lineHeight + TOOLTIP_PAD * 2

        val screen = McCompat.currentScreen()
        val screenWidth = screen?.width ?: Minecraft.getInstance().window.guiScaledWidth
        val screenHeight = screen?.height ?: Minecraft.getInstance().window.guiScaledHeight

        val left = x.coerceAtMost(screenWidth - boxWidth).coerceAtLeast(0)
        val top = y.coerceAtMost(screenHeight - boxHeight).coerceAtLeast(0)

        drawPanel(left, top, left + boxWidth, top + boxHeight)

        lines.forEachIndexed { index, line ->
            var textX = left + TOOLTIP_PAD
            val lineY = top + TOOLTIP_PAD + index * font.lineHeight
            icons(index).forEach { stack ->
                renderFakeItem(stack, textX, lineY, icon, icon)
                textX += iconStep
            }
            text(font, line, textX, lineY, Common.UI.TEXT_COLOR, false)
        }
        return intArrayOf(left, top, left + boxWidth, top + boxHeight)
    }

    /** How tall [drawMultilineBoxCentered] draws [text], so screens can stack boxes under each other. */
    fun boxHeight(text: String): Int = computeLayout(text).boxHeight

    /** A framed box with [text] centred in it, line by line, its middle at ([centerX], [centerY]). */
    fun GuiGraphicsExtractor.drawMultilineBoxCentered(
        text: String,
        centerX: Int,
        centerY: Int,
        color: Int? = null
    ) {
        val font = Minecraft.getInstance().font
        val layout = computeLayout(text)

        val x = centerX - layout.boxWidth / 2
        val y = centerY - layout.boxHeight / 2

        drawPanel(x, y, x + layout.boxWidth, y + layout.boxHeight, frame = color ?: Common.UI.BORDER_COLOR)

        var currentY = y + BOX_PAD

        layout.lines.forEach { line ->
            val centeredX = x + (layout.boxWidth - font.width(line)) / 2

            guiRenderState.addText(
                GuiTextRenderState(
                    font,
                    Component.literal(line).visualOrderText,
                    Matrix3x2f(pose()),
                    centeredX,
                    currentY,
                    Common.UI.TEXT_COLOR,
                    0,
                    false,
                    false,
                    scissorStack.peek()
                )
            )

            currentY += font.lineHeight
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

        // the gui draws every item at sixteen units and stretches the picture, which blurs a big
        // one, so anything larger is drawn into its own texture at the size it shows at
        val size = minOf(width, height)
        if (size > CRISP_ITEM_ABOVE && !renderDecorations) {
            val state = TrackingItemStackRenderState()
            mc.itemModelResolver.updateForTopItem(state, stack, ItemDisplayContext.GUI, mc.level, null, 0)

            guiRenderState.addPicturesInPictureState(
                ItemIconRenderState(state, x, y, x + size, y + size, size.toFloat(), Matrix3x2f(pose()))
            )
            return
        }

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

    /** Room between a text box's frame and its lines. */
    private const val BOX_PAD: Int = 4

    private const val ELLIPSIS: String = "…"
}
