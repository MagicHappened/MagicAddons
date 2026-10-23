package org.magic.magicaddons.ui.screens

import java.time.Duration
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.FormattedCharSequence
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.features.farming.fortune.FarmingFortune
import org.magic.magicaddons.features.farming.fortune.FieldScope
import org.magic.magicaddons.features.farming.fortune.FortuneApi
import org.magic.magicaddons.features.farming.fortune.FortuneAudit
import org.magic.magicaddons.features.farming.fortune.FortuneCrops
import org.magic.magicaddons.features.farming.fortune.FortuneField
import org.magic.magicaddons.features.farming.fortune.FortuneMenuReaders
import org.magic.magicaddons.features.farming.fortune.FortuneReadings
import org.magic.magicaddons.features.farming.fortune.FortuneStat
import org.magic.magicaddons.features.farming.fortune.FortuneTables
import org.magic.magicaddons.features.farming.fortune.TemporaryFortune
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.ConfirmContext
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.drawScrollBar
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor
import org.magic.magicaddons.util.ScreenUtil.ellipsised

class FortuneSourcesScreen : MagicAddonsScreen(Component.literal("Fortune Sources"), "the fortune sources screen"), OverlayContext {

    private companion object {
        const val SCALE: Float = 1.25f
        const val ROW_HEIGHT: Int = 12
        const val PADDING: Int = 8
        const val BUTTON_HEIGHT: Int = 18
        const val BUTTON_GAP: Int = 6
        const val CROP_INDENT: Int = 12
        const val LEFT_SHARE: Float = 0.58f
        const val LABEL_COLUMN_WIDTH: Int = 190

        var savedScroll: Int = 0
        var savedSideScroll: Int = 0

        const val SET_HINT: String = "Set any source by hand: /ma fortune set <source> [crop] <value>"
    }

    private fun setHintTooltip(): String = buildString {
        append("Sources:")
        FortuneField.entries.forEach { field ->
            val crop = if (field.scope == FieldScope.PerCrop) " <crop>" else ""
            append("\n  ${field.name}$crop <${field.unit}>")
        }
        append("\nCrops: ${FortuneCrops.representatives.joinToString(", ") { it.name }}")
    }

    private class Row(val label: String, val value: String, val color: Int, val indent: Int, val tooltip: String?, val command: String?, val isHeader: Boolean)

    private class Button(val label: String, val action: () -> Unit) {
        var x: Int = 0
        var y: Int = 0
        var width: Int = 0
    }

    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    private var rows: List<Row> = emptyList()
    private var sideLines: List<Component> = emptyList()
    private var wrappedSideLines: List<FormattedCharSequence> = emptyList()
    private var statusText: String? = null
    private var statusColor: Int = Common.UI.TEXT_DIM_COLOR
    private var isFetching: Boolean = false

    private val buttons: List<Button> = listOf(
        Button("Fetch from API") { fetch() },
        Button("Refresh") { rebuild() },
        Button("Clear data") { confirmClear() },
        Button("Close") { onClose() }
    )

    private var logicalWidth: Int = 0
    private var logicalHeight: Int = 0
    private var listLeft: Int = 0
    private var listTop: Int = 0
    private var listWidth: Int = 0
    private var visibleRows: Int = 0
    private var sideLeft: Int = 0
    private var sideWidth: Int = 0
    private var visibleSideLines: Int = 0
    private var buttonsY: Int = 0

    override fun onInit() {
        rebuild()
    }

    private fun rebuild() {
        rows = buildRows()
        sideLines = buildSideLines()
    }

    private fun buildRows(): List<Row> = buildList {
        add(Row("Sources", "", Common.UI.ACCENT_COLOR, 0, null, null, isHeader = true))
        FortuneField.entries.filter { it.scope == FieldScope.General && it != FortuneField.Cropshot }.forEach { add(readingRow(it, null, it.displayName, 0)) }
        FortuneField.entries.filter { it.scope == FieldScope.Cocoa }.forEach { add(readingRow(it, null, "${it.displayName} (Cocoa Beans)", 0)) }
        add(Row("Chips and visitors", "", Common.UI.ACCENT_COLOR, 0, null, null, isHeader = true))
        add(readingRow(FortuneField.Cropshot, null, FortuneField.Cropshot.displayName, 0))
        FortuneField.entries.filter { it.scope == FieldScope.Info }.forEach { add(readingRow(it, null, it.displayName, 0)) }
        FortuneField.entries.filter { it.scope == FieldScope.PerCrop }.forEach { field ->
            add(Row(field.displayName, "", Common.UI.ACCENT_COLOR, 0, field.where, field.command, isHeader = true))
            field.crops.forEach { crop -> add(readingRow(field, crop, FortuneCrops.statName(crop), CROP_INDENT)) }
        }
    }

    private fun readingRow(field: FortuneField, crop: CropDrop?, label: String, indent: Int): Row {
        val reading = FortuneReadings.get(field, crop)
        val tooltip = buildString {
            append("Update: ${field.where}")
            field.command?.let { append("\nClick to run /$it") }
            if (field.fromApi) append("\nOr press Fetch from API")
        }
        if (reading == null) return Row(label, "not read", Common.UI.WARNING_COLOR, indent, tooltip, field.command, isHeader = false)

        val max = field.maxLevel?.let { "/$it" }.orEmpty()
        val fortune = when {
            field == FortuneField.FortunateFeasting -> if (FarmingFortune.isFeastActive()) ", +${FortuneAudit.format(reading.level * FortuneTables.PER_FORTUNATE_FEASTING_LEVEL)} (feast on)" else ", no feast now"
            field.scope == FieldScope.Info -> ""
            else -> ", +${FortuneAudit.format(field.fortune(reading.level))}"
        }
        val value = "${FortuneAudit.format(reading.level)}$max ${field.unit}$fortune"
        return Row(label, value, Common.UI.TEXT_COLOR, indent, tooltip, field.command, isHeader = false)
    }

    private fun styled(text: String, color: Int): MutableComponent = Component.literal(text).withColor(color)

    private fun buildSideLines(): List<Component> = buildList {
        val snapshot = FarmingFortune.snapshot()
        add(styled("Stats menus", Common.UI.ACCENT_COLOR))
        val breakdown = FortuneReadings.breakdown(FortuneStat.General)
        add(
            if (breakdown == null) styled("Farming Fortune ➜ Flat Bonuses: not read", Common.UI.WARNING_COLOR)
            else styled("Farming Fortune ➜ Flat Bonuses: ${breakdown.entries.size} sources", Common.UI.TEXT_COLOR)
        )

        val totals = FortuneReadings.statTotals()
        if (totals == null) {
            add(styled("Your Stats Breakdown: not read", Common.UI.WARNING_COLOR))
        } else {
            val tool = totals.heldTool.takeIf { it.isNotEmpty() }?.let { ", holding $it" }.orEmpty()
            add(styled("Your Stats Breakdown$tool", Common.UI.TEXT_COLOR))
            FortuneAudit.comparisons(snapshot).forEach { comparison ->
                val line = styled("  ${comparison.stat}: ", Common.UI.TEXT_COLOR)
                line.append(styled("+${FortuneAudit.format(comparison.hypixel)}", if (comparison.matches) Common.UI.SUCCESS_COLOR else Common.UI.TEXT_COLOR))
                if (!comparison.matches) line.append(styled(" (${FortuneAudit.signed(comparison.difference)})", Common.UI.DANGER_COLOR))
                add(line)
            }
            FortuneAudit.categoryGaps(snapshot).forEach { gap ->
                val line = styled("    ${gap.category}: ours +${FortuneAudit.format(gap.ours)}, breakdown +${FortuneAudit.format(gap.breakdown)} ", Common.UI.TEXT_DIM_COLOR)
                line.append(styled("(${FortuneAudit.signed(gap.ours - gap.breakdown)})", Common.UI.DANGER_COLOR))
                add(line)
            }
            if (totals.heldTool != FortuneMenuReaders.heldToolName()) {
                add(styled("Held tool differs from the reading, reopen Your Stats Breakdown to compare", Common.UI.WARNING_COLOR))
            }
        }
        add(Component.empty())

        add(styled("Temporary", Common.UI.ACCENT_COLOR))
        val temporaries = FortuneReadings.activeTemporaries()
        if (temporaries.isEmpty()) add(styled("none active", Common.UI.TEXT_DIM_COLOR))
        temporaries.forEach { (name, temporary) ->
            add(styled("$name: +${FortuneAudit.format(temporary.value)}, ${remainingText(temporary.expiresAt)} left", Common.UI.TEXT_COLOR))
        }
        TemporaryFortune.cakeCount?.let { (count, total) -> add(styled("Century Cakes eaten: $count/$total", Common.UI.TEXT_DIM_COLOR)) }
        val contest = TemporaryFortune.contest
        if (contest != null) {
            val crops = contest.crops.joinToString(", ") { FortuneCrops.statName(it) + if (it == contest.boosted) " (boosted)" else "" }
            val timing = contest.endsAt?.let { ", ${remainingText(it)}" }.orEmpty()
            add(styled("Jacob's Contest: ${if (contest.isActive) "active" else "upcoming"}, $crops$timing", Common.UI.TEXT_COLOR))
        }
    }
    private fun remainingText(until: Long): String {
        val left = Duration.ofMillis((until - System.currentTimeMillis()).coerceAtLeast(0))
        return if (left.toHours() >= 1) "${left.toHours()}h ${left.toMinutesPart()}m" else "${left.toMinutes()}m"
    }

    private fun layout() {
        logicalWidth = (width / SCALE).toInt()
        logicalHeight = (height / SCALE).toInt()
        val headerSpace = ROW_HEIGHT * 2 + PADDING
        buttonsY = logicalHeight - PADDING - BUTTON_HEIGHT
        listLeft = PADDING
        listTop = PADDING + headerSpace
        listWidth = (logicalWidth * LEFT_SHARE).toInt() - PADDING * 2
        sideLeft = listLeft + listWidth + PADDING * 2
        val listBottom = buttonsY - ROW_HEIGHT - PADDING
        visibleRows = ((listBottom - listTop) / ROW_HEIGHT).coerceAtLeast(1).coerceAtMost(rows.size)
        sideWidth = logicalWidth - sideLeft - PADDING - Common.UI.SCROLLBAR_WIDTH - Common.UI.SPACING
        wrappedSideLines = sideLines.flatMap { line -> if (line.string.isEmpty()) listOf(FormattedCharSequence.EMPTY) else font.split(line, sideWidth) }
        visibleSideLines = ((listBottom - listTop) / ROW_HEIGHT).coerceAtLeast(1).coerceAtMost(wrappedSideLines.size)

        val buttonWidth = (logicalWidth - PADDING * 2 - BUTTON_GAP * (buttons.size - 1)) / buttons.size
        buttons.forEachIndexed { index, button ->
            button.x = PADDING + index * (buttonWidth + BUTTON_GAP)
            button.y = buttonsY
            button.width = buttonWidth
        }
    }

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        layout()
        savedScroll = savedScroll.coerceIn(0, (rows.size - visibleRows).coerceAtLeast(0))
        savedSideScroll = savedSideScroll.coerceIn(0, (wrappedSideLines.size - visibleSideLines).coerceAtLeast(0))
        val logicalMouseX = (mouseX / SCALE).toInt()
        val logicalMouseY = (mouseY / SCALE).toInt()

        graphics.pose().pushMatrix()
        graphics.pose().scale(SCALE, SCALE)

        graphics.drawPanel(0, 0, logicalWidth, logicalHeight)
        graphics.text(font, Component.literal("Non Automatic Farming Fortune sources status"), PADDING, PADDING, Common.UI.ACCENT_COLOR, false)
        val setHintY = PADDING + ROW_HEIGHT
        graphics.text(font, Component.literal(SET_HINT), PADDING, setHintY, Common.UI.TEXT_DIM_COLOR, false)

        var hoveredTooltip: String? = null
        if (logicalMouseX in PADDING until PADDING + font.width(SET_HINT) && logicalMouseY in setHintY until setHintY + font.lineHeight) hoveredTooltip = setHintTooltip()
        val valueWidth = listWidth - LABEL_COLUMN_WIDTH
        rows.drop(savedScroll).take(visibleRows).forEachIndexed { index, row ->
            val rowY = listTop + index * ROW_HEIGHT
            val hovered = logicalMouseX in listLeft until listLeft + listWidth && logicalMouseY in rowY until rowY + ROW_HEIGHT
            if (hovered && !row.isHeader) {
                graphics.fill(listLeft, rowY, listLeft + listWidth, rowY + ROW_HEIGHT, Common.UI.HOVER_WASH)
                hoveredTooltip = row.tooltip
            }
            val textY = rowY + (ROW_HEIGHT - font.lineHeight) / 2
            graphics.text(font, Component.literal(row.label), listLeft + row.indent, textY, if (row.isHeader) Common.UI.ACCENT_COLOR else Common.UI.TEXT_COLOR, false)
            val value = ellipsised(font, row.value, valueWidth)
            graphics.text(font, Component.literal(value), listLeft + listWidth - Common.UI.SPACING - font.width(value), textY, row.color, false)
        }
        if (rows.size > visibleRows) {
            graphics.drawScrollBar(listLeft + listWidth + Common.UI.SPACING, listTop, visibleRows * ROW_HEIGHT, rows.size, visibleRows, savedScroll)
        }

        wrappedSideLines.drop(savedSideScroll).take(visibleSideLines).forEachIndexed { index, text ->
            graphics.text(font, text, sideLeft, listTop + index * ROW_HEIGHT, Common.UI.TEXT_COLOR, false)
        }
        if (wrappedSideLines.size > visibleSideLines) {
            graphics.drawScrollBar(sideLeft + sideWidth + Common.UI.SPACING, listTop, visibleSideLines * ROW_HEIGHT, wrappedSideLines.size, visibleSideLines, savedSideScroll)
        }

        statusText?.let { graphics.text(font, Component.literal(it), PADDING, buttonsY - ROW_HEIGHT, statusColor, false) }

        buttons.forEach { button ->
            val hovered = logicalMouseX in button.x until button.x + button.width && logicalMouseY in button.y until button.y + BUTTON_HEIGHT
            val label = if (button.label == "Fetch from API" && isFetching) "Fetching…" else button.label
            graphics.drawButtonPanel(button.x, button.y, button.x + button.width, button.y + BUTTON_HEIGHT, hovered)
            graphics.text(font, Component.literal(label), button.x + (button.width - font.width(label)) / 2, button.y + (BUTTON_HEIGHT - font.lineHeight) / 2, Common.UI.TEXT_COLOR, false)
        }

        graphics.pose().popMatrix()

        renderOverlays(graphics, mouseX, mouseY, delta)
        if (overlays.isEmpty()) hoveredTooltip?.let { graphics.drawTooltipAtCursor(it, mouseX, mouseY) }
    }

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (overlaysMouseClicked(event, doubled)) return true
        val x = (event.x / SCALE).toInt()
        val y = (event.y / SCALE).toInt()

        buttons.firstOrNull { x in it.x until it.x + it.width && y in it.y until it.y + BUTTON_HEIGHT }?.let {
            it.action()
            return true
        }

        if (x in listLeft until listLeft + listWidth && y in listTop until listTop + visibleRows * ROW_HEIGHT) {
            val row = rows.getOrNull(savedScroll + (y - listTop) / ROW_HEIGHT) ?: return true
            row.command?.let {
                ChatUtils.sendCommand(it)
                onClose()
            }
            return true
        }
        return super.onMouseClicked(event, doubled)
    }

    override fun onMouseMoved(mouseX: Double, mouseY: Double) {
        overlays.forEach { it.mouseMoved(mouseX, mouseY) }
    }

    private fun confirmClear() {
        val clear = buttons.first { it.label == "Clear data" }
        val question = "Forget every fortune reading, breakdown and temporary buff for this profile?"
        val menuWidth = ConfirmContext.widthFor(question)
        val menuHeight = ConfirmContext.heightFor()
        val menuX = ((clear.x + clear.width / 2) * SCALE).toInt() - menuWidth / 2
        val menuY = (clear.y * SCALE).toInt() - menuHeight - Common.UI.SPACING
        addContext(ConfirmContext(menuX.coerceIn(0, width - menuWidth), menuY.coerceAtLeast(0), question, this) {
            FortuneReadings.clearAll()
            statusText = "Cleared the stored fortune data for this profile."
            statusColor = Common.UI.TEXT_DIM_COLOR
            rebuild()
        })
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
        if ((mouseX / SCALE).toInt() >= sideLeft) {
            savedSideScroll = (savedSideScroll - scrollY.toInt()).coerceIn(0, (wrappedSideLines.size - visibleSideLines).coerceAtLeast(0))
        } else {
            savedScroll = (savedScroll - scrollY.toInt()).coerceIn(0, (rows.size - visibleRows).coerceAtLeast(0))
        }
        return true
    }

    override fun onExtractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = Unit

    override fun isPauseScreen(): Boolean = false
}
