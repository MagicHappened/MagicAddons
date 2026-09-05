package org.magic.magicaddons.ui.hud

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.FormattedCharSequence
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import kotlin.math.roundToInt

/** Lays content out inside a width and draws it at a scale, wrapped where it has to be. */
object HudPainter {

    /** Room between a box's edge and its text, in pixels. */
    const val PAD: Int = 5

    /** The narrowest text column, in font units, so a box never wraps to a word a line. */
    const val MIN_INNER_UNITS: Int = 40

    private const val LINE_GAP: Int = 1
    private const val PAIR_GAP: Int = 4

    private val font get() = Minecraft.getInstance().font

    /** One row as drawn: its wrapped lines, and a value set against the right edge of the first. */
    class Row(val lines: List<FormattedCharSequence>, val value: FormattedCharSequence?)

    class Laid(val rows: List<Row>) {
        val heightUnits: Int
            get() = rows.sumOf { it.lines.size * Minecraft.getInstance().font.lineHeight } +
                    (rows.size - 1).coerceAtLeast(0) * LINE_GAP
    }

    /** The width the content wants with nothing wrapped, in font units. */
    fun naturalUnits(content: HudContent): Int = content.lines.maxOfOrNull { line ->
        when (line) {
            is HudLine.Text -> font.width(line.text)
            is HudLine.Pair -> font.width(line.label) + PAIR_GAP + font.width(line.value)
        }
    } ?: 0

    fun lay(content: HudContent, innerUnits: Int): Laid {
        val width = innerUnits.coerceAtLeast(MIN_INNER_UNITS)
        return Laid(content.lines.map { line ->
            when (line) {
                is HudLine.Text -> Row(font.split(line.text, width), null)
                is HudLine.Pair -> {
                    val value = line.value.visualOrderText
                    val labelRoom = (width - font.width(line.value) - PAIR_GAP).coerceAtLeast(MIN_INNER_UNITS / 2)
                    Row(font.split(line.label, labelRoom), value)
                }
            }
        })
    }

    /** The box width in pixels a content wants at [scale] when nothing is wrapped. */
    fun naturalWidth(content: HudContent, scale: Float): Int = (naturalUnits(content) * scale).roundToInt() + PAD * 2

    fun minWidth(scale: Float): Int = (MIN_INNER_UNITS * scale).roundToInt() + PAD * 2

    fun innerUnits(width: Int, scale: Float): Int = ((width - PAD * 2) / scale).toInt()

    fun height(laid: Laid, scale: Float): Int = (laid.heightUnits * scale).roundToInt() + PAD * 2

    /** Draws the rows from the inner top left [x], [y], at [scale]. */
    fun draw(graphics: GuiGraphicsExtractor, laid: Laid, x: Int, y: Int, innerUnits: Int, scale: Float, shadow: Boolean) {
        val width = innerUnits.coerceAtLeast(MIN_INNER_UNITS)
        graphics.pose().pushMatrix()
        graphics.pose().translate(x.toFloat(), y.toFloat())
        graphics.pose().scale(scale, scale)

        var lineY = 0
        laid.rows.forEach { row ->
            row.lines.forEachIndexed { index, line ->
                graphics.text(font, line, 0, lineY, TEXT, shadow)
                if (index == 0 && row.value != null) {
                    graphics.text(font, row.value, width - font.width(row.value), lineY, TEXT, shadow)
                }
                lineY += font.lineHeight
            }
            lineY += LINE_GAP
        }
        graphics.pose().popMatrix()
    }

    /** A colour with its alpha replaced by [alpha], from none at zero to the colour's own at one. */
    fun faded(color: Int, alpha: Float): Int {
        val own = (color ushr 24) and 0xFF
        return (color and 0xFFFFFF) or (((own * alpha).roundToInt().coerceIn(0, 255)) shl 24)
    }

    /** The panel behind a box, as solid as [alpha] says; nothing at all at zero. */
    fun drawPanel(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        if (alpha <= 0f) return
        graphics.fill(x, y, x + width, y + height, faded(Common.UI.BACKGROUND_COLOR, alpha))
        graphics.drawBorder(x, y, x + width, y + height, Common.UI.BORDER_SIZE, faded(Common.UI.BORDER_COLOR, alpha))
    }

    /** White, so a line's own colours show and an unstyled one reads plainly. */
    private const val TEXT: Int = 0xFFFFFFFF.toInt()
}
