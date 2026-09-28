package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.ScreenRect
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.ellipsised
import org.magic.magicaddons.util.ScreenUtil.inRect
import kotlin.math.roundToInt

class Bookmarks<T>(
    private val side: Side,
    private val labelOf: (T) -> String,
    private val fillColorOf: (T) -> Int = { Common.UI.BACKGROUND_COLOR },
    private val tooltipOf: (T) -> String? = { null },
    private val onPick: (T, MouseButtonEvent) -> Unit
) {
    enum class Side { Top, Right, Bottom }

    var items: List<T> = emptyList()
        set(value) {
            field = value
            liftByItem.keys.retainAll(value.toSet())
            if (hoveredItem !in value) hoveredItem = null
        }

    var selectedItem: T? = null

    var hoveredItem: T? = null
        private set

    private val font = Minecraft.getInstance().font

    private var edgeX = 0
    private var edgeY = 0
    private var length = 0

    private val liftByItem = mutableMapOf<T, Float>()
    private var lastAnimatedNanos = 0L

    fun layoutAlong(edgeX: Int, edgeY: Int, length: Int) {
        this.edgeX = edgeX
        this.edgeY = edgeY
        this.length = length
    }

    private fun tabSize(): Int {
        if (items.isEmpty()) return 0
        val share = length / items.size
        return if (side == Side.Top) share.coerceAtMost(MAX_TAB_WIDTH) else share
    }

    private fun tabRect(index: Int, item: T): ScreenRect {
        val size = tabSize()
        val lift = (liftByItem[item] ?: 0f).roundToInt()

        return when (side) {
            Side.Top -> ScreenRect(edgeX + index * size, edgeY - TAB_THICKNESS - lift, size, TAB_THICKNESS + lift + TUCK_UNDER_FRAME)
            Side.Right -> ScreenRect(edgeX - TUCK_UNDER_FRAME, edgeY + index * size, TUCK_UNDER_FRAME + TAB_THICKNESS + lift, size)
            Side.Bottom -> ScreenRect(edgeX + index * size, edgeY - TUCK_UNDER_FRAME, size, TUCK_UNDER_FRAME + TAB_THICKNESS + lift)
        }
    }

    private fun stepTabLifts() {
        val now = System.nanoTime()
        val elapsedMs = if (lastAnimatedNanos == 0L) 0f else (now - lastAnimatedNanos) / 1_000_000f
        lastAnimatedNanos = now

        val step = SELECTED_LIFT * elapsedMs / LIFT_ANIMATION_MS

        items.forEach { item ->
            val target = if (item == selectedItem) SELECTED_LIFT.toFloat() else 0f
            val current = liftByItem[item] ?: target
            liftByItem[item] = when {
                current < target -> (current + step).coerceAtMost(target)
                current > target -> (current - step).coerceAtLeast(target)
                else -> target
            }
        }
    }

    fun render(graphics: GuiGraphicsExtractor) {
        stepTabLifts()

        items.forEachIndexed { index, item ->
            val (left, top, width, height) = tabRect(index, item)
            val right = left + width
            val bottom = top + height
            val isSelected = item == selectedItem

            graphics.fill(left, top, right, bottom, fillColorOf(item))
            if (!isSelected) graphics.fill(left, top, right, bottom, Common.UI.PRESSED_SHADE)
            if (item == hoveredItem && !isSelected) graphics.fill(left, top, right, bottom, Common.UI.HOVER_WASH)
            graphics.drawBorder(left, top, right, bottom, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)

            if (side != Side.Right) {
                val labelRoomWidth = width - Common.UI.TEXT_X_PAD * 2
                val shownLabel = ellipsised(font, labelOf(item), labelRoomWidth)
                val textTop = if (side == Side.Top) top + (TAB_THICKNESS - font.lineHeight) / 2 + Common.UI.BORDER_SIZE / 2
                else top + TUCK_UNDER_FRAME + (TAB_THICKNESS - font.lineHeight) / 2 + Common.UI.BORDER_SIZE / 2
                graphics.text(
                    font,
                    Component.literal(shownLabel),
                    left + (width - font.width(shownLabel)) / 2,
                    textTop,
                    Common.UI.TEXT_COLOR,
                    false
                )
            }
        }
    }

    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val item = hoveredItem ?: return
        val text = tooltipOf(item)
            ?: labelOf(item).takeIf { side != Side.Right && ellipsised(font, it, tabSize() - Common.UI.TEXT_X_PAD * 2) != it }
            ?: return

        graphics.drawTooltipAtCursor(text, mouseX, mouseY)
    }

    private fun tabAt(mouseX: Double, mouseY: Double): T? =
        items.withIndex().firstOrNull { (index, item) ->
            val (left, top, width, height) = tabRect(index, item)
            inRect(mouseX, mouseY, left, top, width, height)
        }?.value

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = tabAt(mouseX, mouseY) != null

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredItem = tabAt(mouseX, mouseY)
    }

    fun mouseClicked(event: MouseButtonEvent): Boolean {
        val item = tabAt(event.x, event.y) ?: return false
        onPick(item, event)
        return true
    }

    companion object {
        const val TAB_THICKNESS: Int = 16
        const val SELECTED_LIFT: Int = 4

        const val TOTAL_REACH: Int = TAB_THICKNESS + SELECTED_LIFT

        private val TUCK_UNDER_FRAME: Int get() = Common.UI.BORDER_SIZE

        private const val MAX_TAB_WIDTH: Int = 100
        private const val LIFT_ANIMATION_MS: Float = 150f
    }
}
