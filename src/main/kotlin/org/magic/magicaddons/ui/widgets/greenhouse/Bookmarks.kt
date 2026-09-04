package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip
import kotlin.math.roundToInt

/**
 * Tabs hanging off one edge of a frame like bookmarks out of a book. The picked one stands out a
 * little further and slides there; the rest sit shaded and tucked under the frame's line.
 * Draw them before the frame, so the frame covers where they tuck in.
 */
class Bookmarks<T>(
    private val side: Side,
    private val label: (T) -> String,
    private val fill: (T) -> Int = { Common.UI.BACKGROUND_COLOR },
    private val tooltip: (T) -> String? = { null },
    private val onPick: (T, MouseButtonEvent) -> Unit
) {
    enum class Side { Top, Right, Bottom }

    var items: List<T> = emptyList()
    var selected: T? = null

    var hovered: T? = null
        private set

    private val font = Minecraft.getInstance().font

    /** The frame's corner the strip starts from, and how far it runs along that edge. */
    private var edgeX = 0
    private var edgeY = 0
    private var length = 0

    /** How far out each tab stands now, moving towards where it belongs. */
    private val lifts = mutableMapOf<T, Float>()
    private var lastNanos = 0L

    /** The frame's corner the strip starts from: top left for the top, top right for the right, bottom left for the bottom. */
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

    /** Left, top, right, bottom of the tab for [item] at [index], lift included. */
    private fun rect(index: Int, item: T): IntArray {
        val size = tabSize()
        val lift = (lifts[item] ?: 0f).roundToInt()

        return when (side) {
            Side.Top -> intArrayOf(
                edgeX + index * size,
                edgeY - THICKNESS - lift,
                edgeX + (index + 1) * size,
                edgeY + TUCK
            )
            Side.Right -> intArrayOf(
                edgeX - TUCK,
                edgeY + index * size,
                edgeX + THICKNESS + lift,
                edgeY + (index + 1) * size
            )
            Side.Bottom -> intArrayOf(
                edgeX + index * size,
                edgeY - TUCK,
                edgeX + (index + 1) * size,
                edgeY + THICKNESS + lift
            )
        }
    }

    /** Moves every tab a step towards its place, a full lift taking [ANIM_MS]. */
    private fun animate() {
        val now = System.nanoTime()
        val elapsedMs = if (lastNanos == 0L) 0f else (now - lastNanos) / 1_000_000f
        lastNanos = now

        val step = LIFT * elapsedMs / ANIM_MS

        items.forEach { item ->
            val target = if (item == selected) LIFT.toFloat() else 0f
            val current = lifts[item] ?: target
            lifts[item] = when {
                current < target -> (current + step).coerceAtMost(target)
                current > target -> (current - step).coerceAtLeast(target)
                else -> target
            }
        }
    }

    fun render(graphics: GuiGraphicsExtractor) {
        animate()

        items.forEachIndexed { index, item ->
            val (x1, y1, x2, y2) = rect(index, item)
            val picked = item == selected

            graphics.fill(x1, y1, x2, y2, fill(item))
            if (!picked) graphics.fill(x1, y1, x2, y2, Common.UI.PRESSED_SHADE)
            if (item == hovered && !picked) graphics.fill(x1, y1, x2, y2, Common.UI.HOVER_WASH)
            graphics.drawBorder(x1, y1, x2, y2, Common.UI.BORDER_SIZE, Common.UI.BORDER_COLOR)

            if (side != Side.Right) {
                val room = x2 - x1 - Common.UI.TEXT_X_PAD * 2
                val shown = shortened(label(item), room)
                // the text sits in the part that shows: above the frame for the top, under it for the bottom
                val textTop = if (side == Side.Top) y1 + (THICKNESS - font.lineHeight) / 2 + Common.UI.BORDER_SIZE / 2
                else y1 + TUCK + (THICKNESS - font.lineHeight) / 2 + Common.UI.BORDER_SIZE / 2
                graphics.text(
                    font,
                    Component.literal(shown),
                    x1 + (x2 - x1 - font.width(shown)) / 2,
                    textTop,
                    Common.UI.TEXT_COLOR,
                    false
                )
            }
        }
    }

    /** The name of the tab under the mouse, when the tab has one or could not show all of its own. */
    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val item = hovered ?: return
        val text = tooltip(item)
            ?: label(item).takeIf { side != Side.Right && shortened(it, tabSize() - Common.UI.TEXT_X_PAD * 2) != it }
            ?: return

        graphics.drawSimpleTooltip(text, mouseX + 7, mouseY + 12)
    }

    private fun shortened(text: String, room: Int): String =
        if (font.width(text) <= room) text
        else font.plainSubstrByWidth(text, (room - font.width(ELLIPSIS)).coerceAtLeast(0)) + ELLIPSIS

    private fun tabAt(mouseX: Double, mouseY: Double): T? {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()

        return items.withIndex().firstOrNull { (index, item) ->
            val (x1, y1, x2, y2) = rect(index, item)
            mx in x1 until x2 && my in y1 until y2
        }?.value
    }

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = tabAt(mouseX, mouseY) != null

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        hovered = tabAt(mouseX, mouseY)
    }

    fun mouseClicked(event: MouseButtonEvent): Boolean {
        val item = tabAt(event.x, event.y) ?: return false
        onPick(item, event)
        return true
    }

    private operator fun IntArray.component4(): Int = this[3]

    companion object {
        /** How far a tab stands out of the frame, and how much further the picked one goes. */
        const val THICKNESS: Int = 16
        const val LIFT: Int = 4

        /** The room a strip needs outside the frame. */
        const val REACH: Int = THICKNESS + LIFT

        /** How far a tab reaches under the frame's line, so the two read as one piece. */
        private const val TUCK: Int = Common.UI.BORDER_SIZE

        private const val MAX_TAB_WIDTH: Int = 100
        private const val ANIM_MS: Float = 150f
        private const val ELLIPSIS: String = "…"
    }
}
