package org.magic.magicaddons.ui.screens

import java.time.Duration
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.features.farming.fortune.FarmingFortune
import org.magic.magicaddons.features.farming.fortune.FieldScope
import org.magic.magicaddons.features.farming.fortune.FortuneApi
import org.magic.magicaddons.features.farming.fortune.FortuneAudit
import org.magic.magicaddons.features.farming.fortune.FortuneCrops
import org.magic.magicaddons.features.farming.fortune.FortuneField
import org.magic.magicaddons.features.farming.fortune.FortuneReadings
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.ellipsised

class FortuneSourcesScreen : MagicAddonsScreen(Component.literal("Fortune Sources"), "the fortune sources screen") {

    private companion object {
        const val ROW_HEIGHT: Int = 12
        const val PADDING: Int = 6
        const val BUTTON_HEIGHT: Int = 16
        const val PANEL_WIDTH: Int = 330
        const val MAX_VISIBLE_ROWS: Int = 18
        const val CROP_INDENT: Int = 12
        const val VALUE_COLUMN_WIDTH: Int = 120

        val STALE_AFTER: Duration = Duration.ofDays(3)

        var savedScroll: Int = 0
    }

    private class Row(
        val label: String,
        val value: String,
        val color: Int,
        val indent: Int,
        val tooltip: String?,
        val command: String?,
        val isHeader: Boolean
    )

    private var rows: List<Row> = emptyList()
    private var auditLines: List<FortuneAudit.Line> = emptyList()
    private var statusText: String? = null
    private var statusColor: Int = Common.UI.TEXT_DIM_COLOR
    private var isFetching: Boolean = false

    private var panelX: Int = 0
    private var panelY: Int = 0
    private var panelHeight: Int = 0
    private var listTop: Int = 0
    private var visibleRows: Int = 0
    private var auditTop: Int = 0
    private var fetchButtonY: Int = 0

    override fun onInit() {
        rebuild()
    }

    private fun rebuild() {
        rows = buildRows()
        auditLines = FortuneAudit.lines(FarmingFortune.snapshot())
    }

    private fun buildRows(): List<Row> = buildList {
        FortuneField.entries.forEach { field ->
            when (field.scope) {
                FieldScope.PerCrop -> {
                    add(Row(field.displayName, "", Common.UI.ACCENT_COLOR, 0, field.where, field.command, isHeader = true))
                    FortuneCrops.representatives.forEach { crop -> add(readingRow(field, crop, FortuneCrops.statName(crop), CROP_INDENT)) }
                }
                FieldScope.Cocoa -> add(readingRow(field, null, "${field.displayName} (Cocoa Beans)", 0))
                FieldScope.General -> add(readingRow(field, null, field.displayName, 0))
            }
        }
    }

    private fun readingRow(field: FortuneField, crop: CropDrop?, label: String, indent: Int): Row {
        val reading = FortuneReadings.get(field, crop)
        val tooltip = buildString {
            append(field.where)
            if (field.fromApi) append("\nFilled by Fetch from API")
            append("\nSet by hand: /ma fortune set ${field.name}${crop?.let { " ${it.name}" } ?: ""} <${field.unit}>")
        }
        if (reading == null) return Row(label, "not read", Common.UI.WARNING_COLOR, indent, tooltip, field.command, isHeader = false)

        val age = Duration.ofMillis(System.currentTimeMillis() - reading.readAt)
        val max = field.maxLevel?.let { "/$it" }.orEmpty()
        val value = "${FortuneAudit.format(reading.level)}$max ${field.unit}, +${FortuneAudit.format(field.fortune(reading.level))}, ${ageText(age)} (${reading.origin})"
        val color = if (age > STALE_AFTER) Common.UI.WARNING_COLOR else Common.UI.TEXT_COLOR
        return Row(label, value, color, indent, tooltip, field.command, isHeader = false)
    }

    private fun ageText(age: Duration): String = when {
        age.toMinutes() < 1 -> "just now"
        age.toHours() < 1 -> "${age.toMinutes()}m ago"
        age.toDays() < 1 -> "${age.toHours()}h ago"
        else -> "${age.toDays()}d ago"
    }

    private fun layout() {
        visibleRows = rows.size.coerceAtMost(MAX_VISIBLE_ROWS)
        val headerSpace = ROW_HEIGHT + PADDING
        val auditSpace = (auditLines.size + 1) * ROW_HEIGHT + PADDING
        val statusSpace = ROW_HEIGHT
        panelHeight = PADDING * 2 + headerSpace + visibleRows * ROW_HEIGHT + PADDING + auditSpace + statusSpace + BUTTON_HEIGHT + PADDING
        panelX = (width - PANEL_WIDTH) / 2
        panelY = ((height - panelHeight) / 2).coerceAtLeast(0)
        listTop = panelY + PADDING + headerSpace
        auditTop = listTop + visibleRows * ROW_HEIGHT + PADDING
        fetchButtonY = auditTop + auditSpace + statusSpace
    }

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        layout()
        savedScroll = savedScroll.coerceIn(0, (rows.size - visibleRows).coerceAtLeast(0))

        graphics.drawPanel(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight)
        graphics.text(font, Component.literal("Fortune sources the mod cannot see on your gear"), panelX + PADDING, panelY + PADDING, Common.UI.ACCENT_COLOR, false)

        var hoveredTooltip: String? = null
        rows.drop(savedScroll).take(visibleRows).forEachIndexed { index, row ->
            val rowY = listTop + index * ROW_HEIGHT
            val hovered = mouseX in panelX until panelX + PANEL_WIDTH && mouseY in rowY until rowY + ROW_HEIGHT
            if (hovered && !row.isHeader) {
                graphics.fill(panelX + Common.UI.BORDER_SIZE, rowY, panelX + PANEL_WIDTH - Common.UI.BORDER_SIZE, rowY + ROW_HEIGHT, Common.UI.HOVER_WASH)
                hoveredTooltip = row.tooltip
            }
            val textY = rowY + (ROW_HEIGHT - font.lineHeight) / 2
            graphics.text(font, Component.literal(row.label), panelX + PADDING + row.indent, textY, if (row.isHeader) Common.UI.ACCENT_COLOR else Common.UI.TEXT_COLOR, false)
            val valueX = panelX + PANEL_WIDTH - PADDING - Common.UI.SCROLLBAR_WIDTH - VALUE_COLUMN_WIDTH - 60
            graphics.text(font, Component.literal(ellipsised(font, row.value, VALUE_COLUMN_WIDTH + 56)), valueX, textY, row.color, false)
        }
        if (rows.size > visibleRows) {
            graphics.drawScrollBar(panelX + PANEL_WIDTH - PADDING - Common.UI.SCROLLBAR_WIDTH, listTop, visibleRows * ROW_HEIGHT, rows.size, visibleRows, savedScroll)
        }

        graphics.text(font, Component.literal("Audit against the tab list Stats widget"), panelX + PADDING, auditTop, Common.UI.ACCENT_COLOR, false)
        auditLines.forEachIndexed { index, line ->
            val color = when (line.verdict) {
                FortuneAudit.Verdict.Matches -> Common.UI.SUCCESS_COLOR
                FortuneAudit.Verdict.Differs -> Common.UI.DANGER_COLOR
                FortuneAudit.Verdict.NotInTab -> Common.UI.TEXT_DIM_COLOR
            }
            graphics.text(font, Component.literal(line.text), panelX + PADDING, auditTop + (index + 1) * ROW_HEIGHT, color, false)
        }

        statusText?.let { graphics.text(font, Component.literal(it), panelX + PADDING, fetchButtonY - ROW_HEIGHT, statusColor, false) }

        drawButton(graphics, panelX + PADDING, fetchButtonY, (PANEL_WIDTH - PADDING * 3) / 2, if (isFetching) "Fetching…" else "Fetch from API", mouseX, mouseY)
        drawButton(graphics, panelX + PADDING * 2 + (PANEL_WIDTH - PADDING * 3) / 2, fetchButtonY, (PANEL_WIDTH - PADDING * 3) / 2, "Refresh", mouseX, mouseY)

        hoveredTooltip?.let { graphics.drawTooltipAtCursor(it, mouseX, mouseY) }
    }

    private fun drawButton(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, label: String, mouseX: Int, mouseY: Int) {
        val hovered = mouseX in x until x + width && mouseY in y until y + BUTTON_HEIGHT
        graphics.drawButtonPanel(x, y, x + width, y + BUTTON_HEIGHT, hovered)
        graphics.text(font, Component.literal(label), x + (width - font.width(label)) / 2, y + (BUTTON_HEIGHT - font.lineHeight) / 2, Common.UI.TEXT_COLOR, false)
    }

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        if (x !in panelX until panelX + PANEL_WIDTH) return super.onMouseClicked(event, doubled)

        val buttonWidth = (PANEL_WIDTH - PADDING * 3) / 2
        if (y in fetchButtonY until fetchButtonY + BUTTON_HEIGHT) {
            if (x < panelX + PADDING + buttonWidth) fetch() else rebuild()
            return true
        }

        if (y in listTop until listTop + visibleRows * ROW_HEIGHT) {
            val row = rows.getOrNull(savedScroll + (y - listTop) / ROW_HEIGHT) ?: return true
            row.command?.let {
                ChatUtils.sendCommand(it)
                onClose()
            }
            return true
        }
        return super.onMouseClicked(event, doubled)
    }

    private fun fetch() {
        if (isFetching) return
        isFetching = true
        statusText = "Asking the server…"
        statusColor = Common.UI.TEXT_DIM_COLOR
        FortuneApi.fetch().thenAccept { outcome ->
            Minecraft.getInstance().execute {
                isFetching = false
                statusText = FortuneApi.describe(outcome)
                statusColor = if (outcome is FortuneApi.Outcome.Filled) Common.UI.SUCCESS_COLOR else Common.UI.WARNING_COLOR
                rebuild()
            }
        }
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        savedScroll = (savedScroll - scrollY.toInt()).coerceIn(0, (rows.size - visibleRows).coerceAtLeast(0))
        return true
    }

    override fun onExtractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = Unit

    override fun isPauseScreen(): Boolean = false
}
