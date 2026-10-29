package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.features.farming.greenhousePresets.flows.PresetMatcher
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.widgets.ListContextMenu
import org.magic.magicaddons.util.ScreenUtil.drawTooltipAtCursor

class ApplyPresetContext(
    x: Int,
    y: Int,
    private val preset: GreenhouseLayout,
    private val pairings: List<PresetMatcher.Pairing>,
    private val shownPlot: PlotLayout?,
    private val shownGreenhouse: GreenhouseGrid?,
    isNoRotateChecked: Boolean,
    private val context: OverlayContext,
    private val onApplyAll: (List<PresetMatcher.Pairing>, noRotate: Boolean) -> Unit,
    private val onApplySingle: (PlotLayout, GreenhouseGrid, noRotate: Boolean) -> Unit,
    private val onByHand: (noRotate: Boolean) -> Unit
) : ListContextMenu<ApplyPresetContext.Choice>(x, y, choicesOf(preset, pairings, shownPlot, shownGreenhouse), "Apply ${preset.displayName()}") {

    sealed class Choice(val label: String, private val drawsItself: Boolean) {
        override fun toString(): String = if (drawsItself) "" else label

        class ApplyAll(label: String) : Choice(label, drawsItself = false)
        class Pick(label: String) : Choice(label, drawsItself = true)
        class Single(val plot: PlotLayout, val grid: GreenhouseGrid, label: String) : Choice(label, drawsItself = false)
        class ByHand : Choice(BY_HAND_LABEL, drawsItself = true)
    }

    private val noRotateCheckbox = CheckboxWidget(CHECKBOX_SIZE, isNoRotateChecked)

    override val footerHeight: Int get() = rowHeight

    override val overlayWidth: Int
        get() = maxOf(
            super.overlayWidth,
            values.maxOf { font.width(it.label) } + Common.UI.TEXT_X_PAD * 2,
            font.width(NO_ROTATE_LABEL) + CHECKBOX_SIZE + Common.UI.TEXT_X_PAD * 3
        )

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderOverlay(graphics, mouseX, mouseY, delta)

        rows.forEach { row ->
            val choice = row.value
            val textY = row.y + (row.height - font.lineHeight) / 2
            when (choice) {
                is Choice.ByHand -> {
                    graphics.fill(row.x, row.y, row.x + row.width, row.y + 1, Common.UI.DIVIDER_COLOR)
                    graphics.text(font, Component.literal(choice.label), row.x + Common.UI.TEXT_X_PAD, textY, Common.UI.PENDING_COLOR, false)
                }
                is Choice.Pick -> graphics.text(font, Component.literal(choice.label), row.x + Common.UI.TEXT_X_PAD, textY, Common.UI.TEXT_DIM_COLOR, false)
                else -> {}
            }
        }

        val footerTop = overlayY + overlayHeight - footerHeight
        val isOverFooter = mouseX in overlayX until overlayX + overlayWidth && mouseY in footerTop until footerTop + footerHeight
        if (isOverFooter) graphics.drawTooltipAtCursor(NO_ROTATE_TOOLTIP, mouseX, mouseY)
    }

    override fun renderFooter(graphics: GuiGraphicsExtractor, footerTop: Int, mouseX: Int, mouseY: Int) {
        graphics.text(
            font,
            Component.literal(NO_ROTATE_LABEL),
            overlayX + Common.UI.TEXT_X_PAD,
            footerTop + (footerHeight - font.lineHeight) / 2 + 1,
            Common.UI.TEXT_COLOR,
            false
        )
        noRotateCheckbox.x = overlayX + overlayWidth - CHECKBOX_SIZE - Common.UI.TEXT_X_PAD
        noRotateCheckbox.y = footerTop + (footerHeight - CHECKBOX_SIZE) / 2
        noRotateCheckbox.render(graphics)
    }

    override fun onFooterClicked() {
        noRotateCheckbox.isChecked = !noRotateCheckbox.isChecked
    }

    override fun onValueSelected(value: Choice) {
        when (value) {
            is Choice.Pick -> return
            is Choice.ApplyAll -> onApplyAll(pairings, noRotateCheckbox.isChecked)
            is Choice.Single -> onApplySingle(value.plot, value.grid, noRotateCheckbox.isChecked)
            is Choice.ByHand -> onByHand(noRotateCheckbox.isChecked)
        }
        context.removeOverlay(this)
    }

    companion object {
        private const val CHECKBOX_SIZE: Int = 12
        private const val NO_ROTATE_LABEL: String = "No rotate"
        private const val NO_ROTATE_TOOLTIP: String = "Lays every plot exactly as drawn instead of turning it to fit what is already planted"
        private const val BY_HAND_LABEL: String = "✎ Match plots by hand…"

        private fun choicesOf(preset: GreenhouseLayout, pairings: List<PresetMatcher.Pairing>, shownPlot: PlotLayout?, shownGreenhouse: GreenhouseGrid?): List<Choice> = buildList {
            if (pairings.isNotEmpty()) {
                val greenhouses = pairings.joinToString(", ") { it.grid.layout.displayName() }
                val leftOut = preset.plots.filter { plot -> pairings.none { it.plot === plot } }
                val leftOutText = if (leftOut.isEmpty()) "" else " (no greenhouse for ${leftOut.joinToString(", ") { preset.plotTitle(it) }})"
                add(Choice.ApplyAll("Apply to $greenhouses$leftOutText"))
                pairings.forEach { pairing ->
                    val steps = if (pairing.plot.hasSteps) " (${pairing.plot.steps.size} steps)" else ""
                    add(Choice.Pick("  ${preset.plotTitle(pairing.plot)}$steps → ${pairing.grid.layout.displayName()} · ${pairing.describe()}"))
                }
            }
            if (shownPlot != null && shownGreenhouse != null) {
                add(Choice.Single(shownPlot, shownGreenhouse, "${preset.plotTitle(shownPlot)} → ${shownGreenhouse.layout.displayName()} only"))
            }
            add(Choice.ByHand())
        }
    }
}
