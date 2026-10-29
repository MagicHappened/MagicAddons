package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.features.farming.greenhousePresets.flows.PresetMatcher
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.ScreenRect
import org.magic.magicaddons.ui.widgets.CheckboxWidget
import org.magic.magicaddons.ui.widgets.ClickableButtonWidget
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.modText

class MatchPlotsOverlay(
    screenWidth: Int,
    screenHeight: Int,
    private val preset: GreenhouseLayout,
    private val greenhouses: List<GreenhouseGrid>,
    startingPairs: Map<GreenhouseGrid, PlotLayout>,
    isNoRotateChecked: Boolean,
    private val context: OverlayContext,
    private val onApply: (pairs: Map<GreenhouseGrid, PlotLayout>, noRotate: Boolean) -> Unit
) : OverlayRenderable {

    override val renderPriority: Int = OverlayRenderable.DIALOG_PRIORITY

    override var hoveredElement: GuiEventListener? = null

    private val font = Minecraft.getInstance().font

    private val pairs: MutableMap<GreenhouseGrid, PlotLayout> = startingPairs.toMutableMap()

    private var pickedPlot: PlotLayout? = null
    private var draggingPlot: PlotLayout? = null
    private var dragX: Int = 0
    private var dragY: Int = 0

    private val noRotateCheckbox = CheckboxWidget(CHECKBOX_SIZE, isNoRotateChecked)

    private val autoFillButton = ClickableButtonWidget("Auto fill")
    private val cancelButton = ClickableButtonWidget("Cancel")
    private val applyButton = ClickableButtonWidget("Apply")

    private val title = "Match ${preset.displayName()}"

    private val columnWidth: Int
    override val overlayWidth: Int
    override val overlayHeight: Int
    override val overlayX: Int
    override val overlayY: Int

    private val plotRowTop: Int
    private val greenhouseRowTop: Int

    init {
        val texts = preset.plots.map { plotTitle(it) } + greenhouses.map { it.layout.displayName() } +
                preset.plots.flatMap { plot -> listOf(plotCaption(plot), PICK_HINT) } + greenhouses.flatMap { grid -> preset.plots.map { greenhouseCaption(grid, it) } }
        columnWidth = maxOf(MINI_SIZE, texts.maxOf { font.width(it) })
        val widestRow = maxOf(preset.plots.size, greenhouses.size) * (columnWidth + CARD_GAP) - CARD_GAP
        val buttonsWidth = autoFillButton.width + cancelButton.width + applyButton.width + Common.UI.SPACING * 2
        overlayWidth = (maxOf(widestRow, buttonsWidth, font.width(title) + CHECKBOX_SIZE + font.width(NO_ROTATE_LABEL) + Common.UI.SPACING_LARGE * 2, font.width(HINT)) + PADDING * 2)
            .coerceAtMost(screenWidth - Common.UI.SPACING_LARGE * 2)

        var top = PADDING
        top += TITLE_HEIGHT + font.lineHeight + Common.UI.SPACING
        top += font.lineHeight + Common.UI.SPACING_SMALL
        plotRowTop = top
        top += CARD_HEIGHT + SECTION_GAP
        top += font.lineHeight + Common.UI.SPACING_SMALL
        greenhouseRowTop = top
        top += CARD_HEIGHT + SECTION_GAP
        overlayHeight = top + ClickableButtonWidget.DEFAULT_HEIGHT + PADDING
        overlayX = (screenWidth - overlayWidth) / 2
        overlayY = (screenHeight - overlayHeight) / 2

        val buttonsY = overlayY + overlayHeight - PADDING - ClickableButtonWidget.DEFAULT_HEIGHT
        autoFillButton.x = overlayX + (overlayWidth - buttonsWidth) / 2
        autoFillButton.y = buttonsY
        cancelButton.x = autoFillButton.x + autoFillButton.width + Common.UI.SPACING
        cancelButton.y = buttonsY
        applyButton.x = cancelButton.x + cancelButton.width + Common.UI.SPACING
        applyButton.y = buttonsY
        noRotateCheckbox.x = overlayX + (overlayWidth + font.width(title) + Common.UI.SPACING_LARGE - CHECKBOX_SIZE - font.width(NO_ROTATE_LABEL) - Common.UI.SPACING) / 2
        noRotateCheckbox.y = overlayY + PADDING + (TITLE_HEIGHT - CHECKBOX_SIZE) / 2
    }

    private fun plotTitle(plot: PlotLayout): String = preset.plotTitle(plot) + if (plot.hasSteps) " · ${plot.steps.size} steps" else ""

    private fun plotCaption(plot: PlotLayout): String {
        val homes = pairs.filterValues { it === plot }.keys.map { it.layout.displayName() }
        return if (homes.isEmpty()) NOT_PLACED else "→ ${homes.joinToString(", ")}"
    }

    private fun greenhouseCaption(grid: GreenhouseGrid, plot: PlotLayout): String {
        val pairing = PresetMatcher.pairingOf(plot, grid, noRotateCheckbox.isChecked)
        val step = if (plot.hasSteps) " · s${pairing.stepIndex + 1}" else ""
        return "← ${preset.plotTitle(plot)}$step" + if (pairing.turns == 0) "" else " · ${pairing.turns * 90}°"
    }

    private fun rowLeft(count: Int): Int = overlayX + (overlayWidth - (count * (columnWidth + CARD_GAP) - CARD_GAP)) / 2

    private fun plotCard(index: Int): ScreenRect = ScreenRect(rowLeft(preset.plots.size) + index * (columnWidth + CARD_GAP), plotRowTop + overlayY, columnWidth, CARD_HEIGHT)

    private fun greenhouseCard(index: Int): ScreenRect = ScreenRect(rowLeft(greenhouses.size) + index * (columnWidth + CARD_GAP), greenhouseRowTop + overlayY, columnWidth, CARD_HEIGHT)

    override fun renderOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val window = Minecraft.getInstance().window
        graphics.fill(0, 0, window.guiScaledWidth * 4, window.guiScaledHeight * 4, Common.UI.SCREEN_DIM_COLOR)
        graphics.drawPanel(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)

        val titleWidth = font.width(title) + Common.UI.SPACING_LARGE + CHECKBOX_SIZE + Common.UI.SPACING + font.width(NO_ROTATE_LABEL)
        val titleLeft = overlayX + (overlayWidth - titleWidth) / 2
        graphics.modText(font, title, titleLeft, overlayY + PADDING + (TITLE_HEIGHT - font.lineHeight) / 2 + 1, Common.UI.TEXT_COLOR)
        noRotateCheckbox.x = titleLeft + font.width(title) + Common.UI.SPACING_LARGE
        noRotateCheckbox.render(graphics)
        graphics.modText(font, NO_ROTATE_LABEL, noRotateCheckbox.x + CHECKBOX_SIZE + Common.UI.SPACING, noRotateCheckbox.y + (CHECKBOX_SIZE - font.lineHeight) / 2 + 1, Common.UI.TEXT_DIM_COLOR)
        drawCentered(graphics, HINT, overlayY + PADDING + TITLE_HEIGHT, Common.UI.TEXT_DIM_COLOR)

        drawCentered(graphics, PLOTS_HEADING, overlayY + plotRowTop - font.lineHeight - Common.UI.SPACING_SMALL, Common.UI.TEXT_DIM_COLOR)
        preset.plots.forEachIndexed { index, plot ->
            val caption = if (plot === pickedPlot) PICK_HINT else plotCaption(plot)
            drawCard(graphics, plotCard(index), plotTitle(plot), plot.steps.first().layout, caption, plot === pickedPlot)
        }

        drawCentered(graphics, GREENHOUSES_HEADING, overlayY + greenhouseRowTop - font.lineHeight - Common.UI.SPACING_SMALL, Common.UI.TEXT_DIM_COLOR)
        greenhouses.forEachIndexed { index, grid ->
            val card = greenhouseCard(index)
            val paired = pairs[grid]
            val isTarget = (pickedPlot ?: draggingPlot) != null && inRect(mouseX, mouseY, card.x, card.y, card.width, card.height)
            drawCard(graphics, card, grid.layout.displayName(), grid.layout, paired?.let { greenhouseCaption(grid, it) } ?: NOT_PLACED, isTarget || paired != null && paired === pickedPlot)
        }

        draggingPlot?.let { plot -> drawMiniGrid(graphics, plot.steps.first().layout, dragX - MINI_SIZE / 2, dragY - MINI_SIZE / 2) }

        autoFillButton.extractRenderState(graphics, mouseX, mouseY, delta)
        cancelButton.extractRenderState(graphics, mouseX, mouseY, delta)
        applyButton.extractRenderState(graphics, mouseX, mouseY, delta)
        if (pairs.isEmpty()) graphics.fill(applyButton.x, applyButton.y, applyButton.x + applyButton.width, applyButton.y + applyButton.height, Common.UI.PRESSED_SHADE)
    }

    private fun drawCentered(graphics: GuiGraphicsExtractor, text: String, top: Int, color: Int) {
        graphics.modText(font, text, overlayX + (overlayWidth - font.width(text)) / 2, top, color)
    }

    private fun drawCard(graphics: GuiGraphicsExtractor, card: ScreenRect, title: String, layout: PlotLayout, caption: String, isLit: Boolean) {
        graphics.modText(font, title, card.x + (card.width - font.width(title)) / 2, card.y, Common.UI.TEXT_COLOR)
        val miniLeft = card.x + (card.width - MINI_SIZE) / 2
        val miniTop = card.y + font.lineHeight + Common.UI.SPACING_SMALL
        drawMiniGrid(graphics, layout, miniLeft, miniTop)
        if (isLit) graphics.drawBorder(miniLeft - 1, miniTop - 1, miniLeft + MINI_SIZE + 1, miniTop + MINI_SIZE + 1, 1, Common.UI.ACCENT_COLOR)
        graphics.modText(font, caption, card.x + (card.width - font.width(caption)) / 2, miniTop + MINI_SIZE + Common.UI.SPACING_SMALL, Common.UI.TEXT_DIM_COLOR)
    }

    private fun drawMiniGrid(graphics: GuiGraphicsExtractor, layout: PlotLayout, left: Int, top: Int) {
        val cell = MINI_SIZE / layout.size
        val span = cell * layout.size
        graphics.fill(left, top, left + span, top + span, EMPTY_CELL_COLOR)
        layout.slots.forEach { slot ->
            if (slot.soil == null) return@forEach
            graphics.fill(left + slot.x * cell, top + slot.y * cell, left + slot.x * cell + cell, top + slot.y * cell + cell, SOIL_COLOR)
        }
        layout.plants.forEach { plant ->
            val color = when (plant.slot.mark) {
                LayoutSlot.Marking.Target -> LayoutSlot.Marking.Target.color
                LayoutSlot.Marking.Ingredient -> LayoutSlot.Marking.Ingredient.color
                null -> if (plant.cropDef.isMutation) MUTATION_COLOR else PLANT_COLOR
            }
            val x = left + plant.slot.x * cell
            val y = top + plant.slot.y * cell
            graphics.fill(x, y, x + cell * plant.cropDef.footprint.width, y + cell * plant.cropDef.footprint.height, color)
        }
        graphics.drawBorder(left, top, left + span, top + span, 1, Common.UI.BORDER_COLOR)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        autoFillButton.mouseMoved(mouseX, mouseY)
        cancelButton.mouseMoved(mouseX, mouseY)
        applyButton.mouseMoved(mouseX, mouseY)
        if (draggingPlot != null) {
            dragX = mouseX.toInt()
            dragY = mouseY.toInt()
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseX = mouseButtonEvent.x
        val mouseY = mouseButtonEvent.y
        if (mouseButtonEvent.button() != 0) {
            pickedPlot = null
            return isMouseOver(mouseX, mouseY)
        }
        if (cancelButton.mouseClicked(mouseButtonEvent, doubled)) {
            context.removeOverlay(this)
            return true
        }
        if (applyButton.mouseClicked(mouseButtonEvent, doubled)) {
            if (pairs.isEmpty()) return true
            context.removeOverlay(this)
            onApply(pairs.toMap(), noRotateCheckbox.isChecked)
            return true
        }
        if (autoFillButton.mouseClicked(mouseButtonEvent, doubled)) {
            fillAutomatically()
            return true
        }
        if (inRect(mouseX, mouseY, noRotateCheckbox.x, noRotateCheckbox.y, CHECKBOX_SIZE + Common.UI.SPACING + font.width(NO_ROTATE_LABEL), CHECKBOX_SIZE)) {
            noRotateCheckbox.isChecked = !noRotateCheckbox.isChecked
            return true
        }

        plotCardAt(mouseX, mouseY)?.let { plot ->
            pickedPlot = if (pickedPlot === plot) null else plot
            draggingPlot = plot
            dragX = mouseX.toInt()
            dragY = mouseY.toInt()
            return true
        }
        greenhouseCardAt(mouseX, mouseY)?.let { grid ->
            val plot = pickedPlot
            if (plot == null) pairs.remove(grid) else pairs[grid] = plot
            return true
        }
        return isMouseOver(mouseX, mouseY)
    }

    fun mouseReleased(mouseX: Double, mouseY: Double) {
        val plot = draggingPlot ?: return
        draggingPlot = null
        val target = greenhouseCardAt(mouseX, mouseY) ?: return
        pairs[target] = plot
        pickedPlot = null
    }

    private fun fillAutomatically() {
        pairs.clear()
        PresetMatcher.bestPairings(preset, greenhouses, noRotateCheckbox.isChecked).forEach { pairs[it.grid] = it.plot }
        pickedPlot = null
    }

    private fun plotCardAt(mouseX: Double, mouseY: Double): PlotLayout? =
        preset.plots.withIndex().firstOrNull { (index, _) -> plotCard(index).let { inRect(mouseX, mouseY, it.x, it.y, it.width, it.height) } }?.value

    private fun greenhouseCardAt(mouseX: Double, mouseY: Double): GreenhouseGrid? =
        greenhouses.withIndex().firstOrNull { (index, _) -> greenhouseCard(index).let { inRect(mouseX, mouseY, it.x, it.y, it.width, it.height) } }?.value

    companion object {
        private const val PADDING: Int = 10
        private const val TITLE_HEIGHT: Int = 14
        private const val MINI_SIZE: Int = 60
        private const val CARD_GAP: Int = 14
        private const val SECTION_GAP: Int = 12
        private const val CHECKBOX_SIZE: Int = 10
        private val CARD_HEIGHT: Int = Minecraft.getInstance().font.lineHeight * 2 + MINI_SIZE + Common.UI.SPACING_SMALL * 2
        private const val EMPTY_CELL_COLOR: Int = 0xFF202020.toInt()
        private const val SOIL_COLOR: Int = 0xFF5A3F2A.toInt()
        private const val PLANT_COLOR: Int = 0xFF3B6B3B.toInt()
        private const val MUTATION_COLOR: Int = 0xFF8A5BD6.toInt()
        private const val NO_ROTATE_LABEL: String = "No rotate"
        private const val HINT: String = "Click a plot, then its greenhouses. Click a greenhouse to clear it."
        private const val PLOTS_HEADING: String = "Preset plots"
        private const val GREENHOUSES_HEADING: String = "Greenhouses"
        private const val NOT_PLACED: String = "—"
        private const val PICK_HINT: String = "pick its greenhouses…"
    }
}
