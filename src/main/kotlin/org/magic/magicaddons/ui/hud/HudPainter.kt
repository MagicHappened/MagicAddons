package org.magic.magicaddons.ui.hud

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.FormattedCharSequence
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import kotlin.math.roundToInt

object HudPainter {

    const val BOX_PADDING: Int = 5

    const val MIN_TEXT_WIDTH_UNITS: Int = 40

    private const val ROW_GAP_UNITS: Int = 1
    private const val LABEL_VALUE_GAP_UNITS: Int = 4

    private const val TEXT_COLOR: Int = 0xFFFFFFFF.toInt()

    private val font get() = Minecraft.getInstance().font

    class LaidRow(val lines: List<FormattedCharSequence>, val rightValue: FormattedCharSequence?)

    class LaidContent(val rows: List<LaidRow>) {
        val heightUnits: Int
            get() = rows.sumOf { it.lines.size * font.lineHeight } +
                    (rows.size - 1).coerceAtLeast(0) * ROW_GAP_UNITS
    }

    fun naturalTextWidthUnits(content: HudContent): Int = content.lines.maxOfOrNull { line ->
        when (line) {
            is HudLine.Text -> font.width(line.text)
            is HudLine.LabelValue -> font.width(line.label) + LABEL_VALUE_GAP_UNITS + font.width(line.value)
        }
    } ?: 0

    fun layContent(content: HudContent, textWidthUnits: Int): LaidContent {
        val textWidth = textWidthUnits.coerceAtLeast(MIN_TEXT_WIDTH_UNITS)
        return LaidContent(content.lines.map { line ->
            when (line) {
                is HudLine.Text -> LaidRow(font.split(line.text, textWidth), null)
                is HudLine.LabelValue -> {
                    val rightValue = line.value.visualOrderText
                    val labelWidth = (textWidth - font.width(line.value) - LABEL_VALUE_GAP_UNITS).coerceAtLeast(MIN_TEXT_WIDTH_UNITS / 2)
                    LaidRow(font.split(line.label, labelWidth), rightValue)
                }
            }
        })
    }

    fun naturalBoxWidth(content: HudContent, scale: Float): Int = (naturalTextWidthUnits(content) * scale).roundToInt() + BOX_PADDING * 2

    fun minBoxWidth(scale: Float): Int = (MIN_TEXT_WIDTH_UNITS * scale).roundToInt() + BOX_PADDING * 2

    fun minBoxHeight(scale: Float): Int = (font.lineHeight * scale).roundToInt() + BOX_PADDING * 2

    fun textWidthUnits(boxWidth: Int, scale: Float): Int = ((boxWidth - BOX_PADDING * 2) / scale).toInt()

    fun boxHeight(laidContent: LaidContent, scale: Float): Int = (laidContent.heightUnits * scale).roundToInt() + BOX_PADDING * 2

    fun drawLaidContent(graphics: GuiGraphicsExtractor, laidContent: LaidContent, x: Int, y: Int, textWidthUnits: Int, scale: Float, hasTextShadow: Boolean) {
        val textWidth = textWidthUnits.coerceAtLeast(MIN_TEXT_WIDTH_UNITS)
        graphics.pose().pushMatrix()
        graphics.pose().translate(x.toFloat(), y.toFloat())
        graphics.pose().scale(scale, scale)

        var lineY = 0
        laidContent.rows.forEach { row ->
            row.lines.forEachIndexed { index, line ->
                graphics.text(font, line, 0, lineY, TEXT_COLOR, hasTextShadow)
                if (index == 0 && row.rightValue != null) {
                    graphics.text(font, row.rightValue, textWidth - font.width(row.rightValue), lineY, TEXT_COLOR, hasTextShadow)
                }
                lineY += font.lineHeight
            }
            lineY += ROW_GAP_UNITS
        }
        graphics.pose().popMatrix()
    }

    fun withScaledAlpha(color: Int, alpha: Float): Int {
        val ownAlpha = (color ushr 24) and 0xFF
        return (color and 0xFFFFFF) or (((ownAlpha * alpha).roundToInt().coerceIn(0, 255)) shl 24)
    }

    fun drawBoxPanel(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        if (alpha <= 0f) return
        graphics.fill(x, y, x + width, y + height, withScaledAlpha(Common.UI.BACKGROUND_COLOR, alpha))
        graphics.drawBorder(x, y, x + width, y + height, Common.UI.BORDER_SIZE, withScaledAlpha(Common.UI.BORDER_COLOR, alpha))
    }
}
