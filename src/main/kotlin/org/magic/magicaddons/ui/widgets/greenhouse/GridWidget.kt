package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.Footprint
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.ChorusFruit
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.ChorusCollision
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.ScreenRect
import org.magic.magicaddons.ui.widgets.greenhouse.CropRegions.claimedCells
import org.magic.magicaddons.ui.widgets.greenhouse.CropRegions.freeCellIn
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawCountedCrop
import org.magic.magicaddons.util.ScreenUtil.easedProgress
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.drawItem
import org.magic.magicaddons.util.ScreenUtil.itemStackFor

class GridWidget(
    val layout: PlotLayout,
    val slotSize: Int
) : Renderable, HoverableContainer {

    private fun pixelOffsetOfCell(index: Int): Int = index * (slotSize + LINE_WIDTH)

    val gridSpan: Int get() = gridSpanFor(slotSize, layout.size)

    private val slotWidgets = mutableListOf<SlotWidget>()
    private val plantWidgets = mutableListOf<PlantWidget>()

    var x: Int = 0
    var y: Int = 0

    override var hoveredElement: GuiEventListener? = null

    var shownLabel: PlantLabel? = null

    var cropIdsWithoutLabel: Set<String> = emptySet()

    var targetPlan: () -> PlotLayout? = { null }

    var isShowingUnplannedMutations: Boolean = false

    var chorusMarks: ChorusMarks? = null

    class ChorusMarks(val breaks: List<ChorusCollision.Break>, val ripeCells: List<Pair<Int, Int>>, val isNextTick: Boolean)

    private var unplannedSpotsKey: Int? = null
    private var unplannedSpotsCache: Map<Pair<Int, Int>, List<CropDefinition>> = emptyMap()

    fun unplannedMutationSpots(): Map<Pair<Int, Int>, List<CropDefinition>> {
        val plan = targetPlan() ?: return emptyMap()
        val plannedCropsBySlot = PlotPrediction.targetCropsBySlot(plan)
        var key = plannedCropsBySlot.hashCode()
        layout.slots.forEach { key = key * 31 + (it.soil?.hashCode() ?: 0) }
        layout.plants.forEach {
            key = key * 31 + it.slot.x * 64 + it.slot.y
            key = key * 31 + it.acceptedCrops.map { crop -> crop.name }.hashCode()
            key = key * 31 + (it.slot.mark?.ordinal ?: -1)
            key = key * 31 + if (it.growthStage == null) 0 else 1
        }

        if (key != unplannedSpotsKey) {
            unplannedSpotsCache = PlotPrediction.unplannedMutationSpots(layout, plannedCropsBySlot)
            unplannedSpotsKey = key
        }
        return unplannedSpotsCache
    }

    val plantsJustPlaced: MutableSet<Plant> = mutableSetOf()

    val plantsJustMarked: MutableSet<Plant> = mutableSetOf()

    private class VanishingPlant(val rect: ScreenRect, val stack: ItemStack, val removedAt: Long)

    private val vanishingPlants = mutableListOf<VanishingPlant>()

    fun startVanishing(plant: Plant) {
        val footprint = plant.cropDef.footprint
        val rect = cellRect(plant.slot.x, plant.slot.y, footprint.width, footprint.height)

        vanishingPlants.add(VanishingPlant(rect, itemStackFor(plant.cropDef), System.currentTimeMillis()))
    }

    var turns: Int = 0

    private fun drawnCellOf(x: Int, y: Int): Pair<Int, Int> {
        val last = layout.size - 1
        return when (Math.floorMod(turns, 4)) {
            1 -> (last - y) to x
            2 -> (last - x) to (last - y)
            3 -> y to (last - x)
            else -> x to y
        }
    }

    private fun slotOfDrawnCell(cx: Int, cy: Int): Pair<Int, Int> {
        val last = layout.size - 1
        return when (Math.floorMod(turns, 4)) {
            1 -> cy to (last - cx)
            2 -> (last - cx) to (last - cy)
            3 -> (last - cy) to cx
            else -> cx to cy
        }
    }

    fun slotAt(mouseX: Double, mouseY: Double): Pair<Int, Int>? {
        val step = slotSize + LINE_WIDTH
        val cx = (mouseX.toInt() - x) / step
        val cy = (mouseY.toInt() - y) / step
        if (mouseX < x || mouseY < y || cx !in 0 until layout.size || cy !in 0 until layout.size) return null
        return slotOfDrawnCell(cx, cy)
    }

    fun cellRect(sx: Int, sy: Int, width: Int, height: Int): ScreenRect {
        val (ax, ay) = drawnCellOf(sx, sy)
        val (bx, by) = drawnCellOf(sx + width - 1, sy + height - 1)
        val left = minOf(ax, bx)
        val top = minOf(ay, by)
        val across = maxOf(ax, bx) - left + 1
        val down = maxOf(ay, by) - top + 1
        return ScreenRect.fromEdges(
            x + pixelOffsetOfCell(left),
            y + pixelOffsetOfCell(top),
            x + pixelOffsetOfCell(left + across) - LINE_WIDTH,
            y + pixelOffsetOfCell(top + down) - LINE_WIDTH
        )
    }

    fun footprintRect(sx: Int, sy: Int, footprint: Footprint): ScreenRect = cellRect(sx, sy, footprint.width, footprint.height)

    private fun overlappingTargetRegions(): List<List<Plant>> =
        layout.plants
            .filter { it.slot.mark == LayoutSlot.Marking.Target && it.growthStage == null }
            .groupBy { it.cropDef }
            .flatMap { (crop, targets) ->
                CropRegions.overlappingRegions(targets.map { it.slot.x to it.slot.y }, crop.footprint)
                    .filter { it.size > 1 }
                    .map { region -> region.mapNotNull { corner -> targets.find { (it.slot.x to it.slot.y) == corner } } }
            }

    private fun cellRects(cells: Set<Pair<Int, Int>>): List<ScreenRect> =
        cells.map { (cellX, cellY) -> cellRect(cellX, cellY, 1, 1) }

    private fun outlineOf(cells: Set<Pair<Int, Int>>): List<ScreenRect> {
        val drawn = cells.mapTo(mutableSetOf()) { (cellX, cellY) -> drawnCellOf(cellX, cellY) }
        val border = Common.UI.BORDER_SIZE

        return drawn.flatMap { (cx, cy) ->
            val left = x + pixelOffsetOfCell(cx)
            val top = y + pixelOffsetOfCell(cy)

            val right = left + slotSize + if ((cx + 1 to cy) in drawn) LINE_WIDTH else 0
            val bottom = top + slotSize + if ((cx to cy + 1) in drawn) LINE_WIDTH else 0

            buildList {
                if ((cx to cy - 1) !in drawn) add(ScreenRect.fromEdges(left, top, right, top + border))
                if ((cx to cy + 1) !in drawn) add(ScreenRect.fromEdges(left, bottom - border, right, bottom))
                if ((cx - 1 to cy) !in drawn) add(ScreenRect.fromEdges(left, top, left + border, bottom))
                if ((cx + 1 to cy) !in drawn) add(ScreenRect.fromEdges(right - border, top, right, bottom))
            }
        }
    }

    fun rebuildWidgets() {
        slotWidgets.clear()
        plantWidgets.clear()

        for (sx in 0 until layout.size) {
            for (sy in 0 until layout.size) {
                val slot = layout.getSlot(sx, sy) ?: continue

                val widget = SlotWidget(slot, layout.kind == PlotLayout.Kind.GREENHOUSE_PRESET)

                widget.width = slotSize
                widget.height = slotSize

                val (cx, cy) = drawnCellOf(sx, sy)
                widget.x = x + pixelOffsetOfCell(cx)
                widget.y = y + pixelOffsetOfCell(cy)

                slotWidgets.add(widget)
            }
        }

        val targetRegions = overlappingTargetRegions()
        val plantsDrawnByTheirRegion = targetRegions.flatMap { it.drop(1) }
        val cellsHoldingAnIcon = mutableSetOf<Pair<Int, Int>>()

        layout.plants.forEach { plant ->
            if (plantsDrawnByTheirRegion.any { it === plant }) return@forEach

            val widget = PlantWidget(plant)
            val targetRegion = targetRegions.firstOrNull { it.first() === plant }

            if (plant.slot.mark == LayoutSlot.Marking.Target && plant.cropDef.spawnRule != null) {
                widget.missingSpawnConditions = (targetRegion ?: listOf(plant))
                    .map { PlotPrediction.missingSpawnConditions(layout, it.cropDef, it.slot.x, it.slot.y, ignoredPlant = it) }
                    .minBy { it.size }
            }

            widget.padding = slotSize / 10

            targetRegion?.let {
                val corners = it.map { target -> target.slot.x to target.slot.y }
                val cells = claimedCells(corners, plant.cropDef.footprint)
                val iconCell = freeCellIn(cells, cellsHoldingAnIcon)
                cellsHoldingAnIcon += iconCell

                widget.targetRegion = PlantWidget.MergedTargetRegion(
                    corners = corners.sortedWith(compareBy({ corner -> corner.second }, { corner -> corner.first })),
                    cells = cellRects(cells),
                    outline = outlineOf(cells),
                    iconCell = cellRect(iconCell.first, iconCell.second, 1, 1)
                )
            }

            val footprint = plant.cropDef.footprint
            val rect = widget.targetRegion?.let { region ->
                ScreenRect.fromEdges(
                    region.cells.minOf { cell -> cell.x }, region.cells.minOf { cell -> cell.y },
                    region.cells.maxOf { cell -> cell.right }, region.cells.maxOf { cell -> cell.bottom }
                )
            } ?: cellRect(plant.slot.x, plant.slot.y, footprint.width, footprint.height)

            widget.x = rect.x
            widget.y = rect.y
            widget.width = rect.width
            widget.height = rect.height
            widget.waterEffect = GreenhouseGrid.waterEffectAt(layout, plant.slot)

            widget.drainingSoggybuds = layout.plantsSurrounding(plant).count { it.cropDef.drainsNeighbours && !it.isFullyGrown }

            if (plant.cropDef.drainsNeighbours && layout.kind != PlotLayout.Kind.GREENHOUSE_PRESET) {
                val grid = GreenhouseData.greenhouseGrids.find { it.layout.id == layout.id }
                if (grid != null && GreenhouseTickTime.tickMs != null) {
                    widget.soggybudTicksToGrow = grid.ticksUntilGrown(layout, plant.slot)
                    widget.isSoggybudSimulated = true
                }
            }

            widget.cropStack = itemStackFor(plant.cropDef)
            if (plant in plantsJustPlaced) widget.appearedAt = System.currentTimeMillis()
            if (plant in plantsJustMarked) widget.markedAt = System.currentTimeMillis()
            widget.isInPreset = layout.kind == PlotLayout.Kind.GREENHOUSE_PRESET
            plantWidgets.add(widget)
        }
        plantsJustPlaced.clear()
        plantsJustMarked.clear()
    }

    private fun renderVanishingPlants(graphics: GuiGraphicsExtractor) {
        val now = System.currentTimeMillis()
        vanishingPlants.removeAll { now - it.removedAt >= VANISH_MS }

        vanishingPlants.forEach { vanishingPlant ->
            val shrinkFraction = 1f - easedProgress(vanishingPlant.removedAt, VANISH_MS)
            val rect = vanishingPlant.rect
            val centerX = rect.x + rect.width / 2f
            val centerY = rect.y + rect.height / 2f
            val size = ((minOf(rect.width, rect.height) - slotSize / 5) * shrinkFraction).toInt()
            if (size <= 0) return@forEach

            graphics.drawItem(vanishingPlant.stack, (centerX - size / 2f).toInt(), (centerY - size / 2f).toInt(), size, size)
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        slotWidgets.forEach {
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }

        for (i in 1 until layout.size) {
            val at = pixelOffsetOfCell(i) - LINE_WIDTH
            graphics.fill(x + at, y, x + at + LINE_WIDTH, y + gridSpan, Common.UI.GRID_LINE_COLOR)
            graphics.fill(x, y + at, x + gridSpan, y + at + LINE_WIDTH, Common.UI.GRID_LINE_COLOR)
        }

        plantWidgets.forEach {
            it.isPlanMuted = isShowingUnplannedMutations
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }
        renderVanishingPlants(graphics)
        if (isShowingUnplannedMutations) renderUnplannedMutations(graphics)

        shownLabel?.let { label ->
            plantWidgets
                .filter { it.plant.cropDef.elementId !in cropIdsWithoutLabel }
                .forEach { it.renderLabel(graphics, label) }
        }
        chorusMarks?.let { renderChorusMarks(graphics, it) }
    }

    private fun renderChorusMarks(graphics: GuiGraphicsExtractor, marks: ChorusMarks) {
        val font = Minecraft.getInstance().font
        val footprint = ChorusFruit.definition.footprint

        marks.ripeCells.forEach { (cellX, cellY) ->
            val rect = footprintRect(cellX, cellY, footprint)
            graphics.drawBorder(rect.x, rect.y, rect.right, rect.bottom, CHORUS_MARK_BORDER_SIZE, RIPE_CHORUS_COLOR)
        }
        marks.breaks.forEachIndexed { index, chorusBreak ->
            val rect = footprintRect(chorusBreak.x, chorusBreak.y, footprint)
            graphics.drawBorder(rect.x, rect.y, rect.right, rect.bottom, CHORUS_MARK_BORDER_SIZE, BREAK_CHORUS_COLOR)
            graphics.text(font, Component.literal("${index + 1}"), rect.x + CHORUS_MARK_BORDER_SIZE + 1, rect.y + CHORUS_MARK_BORDER_SIZE + 1, BREAK_CHORUS_COLOR, true)
        }
    }

    fun chorusMarkTooltipAt(mouseX: Double, mouseY: Double): List<Component>? {
        val marks = chorusMarks ?: return null
        val cell = slotAt(mouseX, mouseY) ?: return null
        val footprint = ChorusFruit.definition.footprint
        fun covers(cornerX: Int, cornerY: Int) = cell.first in cornerX until cornerX + footprint.width && cell.second in cornerY until cornerY + footprint.height

        if (marks.ripeCells.any { (cornerX, cornerY) -> covers(cornerX, cornerY) }) {
            return listOf(Component.literal("Ripe chorus").withColor(RIPE_CHORUS_COLOR and 0xFFFFFF), Component.literal("Harvest it first, then the break order is worked out"))
        }
        val index = marks.breaks.indexOfFirst { covers(it.x, it.y) }
        if (index < 0) return null
        val chorusBreak = marks.breaks[index]
        val timing = if (marks.isNextTick) "at the next tick" else "by then"

        return listOf(
            Component.literal("Break #${index + 1} of ${marks.breaks.size}").withColor(BREAK_CHORUS_COLOR and 0xFFFFFF),
            Component.literal("Chorus stage ${chorusBreak.stage}${if (chorusBreak.isOnSpawnTile) " on a spawn tile" else ""}"),
            Component.literal("After this: ${PlantWarnings.chorusRiskText(chorusBreak.chanceAfter)} $timing")
        )
    }

    private fun renderUnplannedMutations(graphics: GuiGraphicsExtractor) {
        val font = Minecraft.getInstance().font
        val spots = unplannedMutationSpots()

        val regions = unplannedRegions(spots)
            .sortedWith(compareBy({ (_, region) -> if (region.size == 1) 0 else 1 }, { (_, region) -> -region.size }))
        val cellsByRegion = regions.map { (crop, region) -> claimedCells(region, crop.footprint) }
        val colorByCrop = colorsFor(regions.map { it.first }.distinct().sortedBy { it.name })

        cellRects(cellsByRegion.flatten().toSet()).forEach { graphics.fill(it.x, it.y, it.right, it.bottom, UNPLANNED_VEIL_COLOR) }

        val cellsWithIcon = mutableSetOf<Pair<Int, Int>>()
        regions.forEachIndexed { index, (crop, region) ->
            val cells = cellsByRegion[index]

            val color = colorByCrop.getValue(crop)
            val padding = slotSize / 10

            if (region.size == 1) {
                val rect = footprintRect(region[0].first, region[0].second, crop.footprint)
                graphics.drawItem(
                    itemStackFor(crop), rect.x + padding, rect.y + padding,
                    rect.width - padding * 2, rect.height - padding * 2
                )
                graphics.drawBorder(rect.x + padding, rect.y + padding, rect.right - padding, rect.bottom - padding, CROP_BORDER_SIZE, color)
                cellsWithIcon += cells
            } else {
                val cell = freeCellIn(cells, cellsWithIcon)
                val rect = cellRect(cell.first, cell.second, 1, 1)

                graphics.drawCountedCrop(font, itemStackFor(crop), rect, region.size, color)
                graphics.drawBorder(rect.x + padding, rect.y + padding, rect.right - padding, rect.bottom - padding, CROP_BORDER_SIZE, color)
                cellsWithIcon += cell
            }
        }

        cellsByRegion.forEachIndexed { index, cells ->
            val color = colorByCrop.getValue(regions[index].first)
            outlineOf(cells).forEach { graphics.fill(it.x, it.y, it.right, it.bottom, color) }
        }

        spots.keys.forEach { (cornerX, cornerY) ->
            val cell = cellRect(cornerX, cornerY, 1, 1)
            graphics.text(font, Component.literal("!"), cell.right - font.width("!") - 2, cell.y + 2, UNPLANNED_MARK_COLOR, true)
        }
    }

    private fun colorsFor(crops: List<CropDefinition>): Map<CropDefinition, Int> =
        crops.withIndex().associate { (index, crop) -> crop to UNPLANNED_CROP_COLORS[index % UNPLANNED_CROP_COLORS.size] }

    private fun unplannedRegions(
        spots: Map<Pair<Int, Int>, List<CropDefinition>>
    ): List<Pair<CropDefinition, List<Pair<Int, Int>>>> =
        spots.entries
            .flatMap { (corner, crops) -> crops.map { it to corner } }
            .groupBy({ it.first }, { it.second })
            .flatMap { (crop, corners) -> CropRegions.touchingRegions(corners, crop.footprint).map { crop to it } }

    fun unplannedTooltipAt(mouseX: Double, mouseY: Double): List<Component>? {
        if (!isShowingUnplannedMutations) return null
        val cell = slotAt(mouseX, mouseY) ?: return null

        val regionsOnCell = unplannedRegions(unplannedMutationSpots())
            .filter { (crop, region) -> cell in claimedCells(region, crop.footprint) }
        if (regionsOnCell.isEmpty()) return null

        return buildList {
            add(Component.literal("Can grow unplanned").withColor(UNPLANNED_MARK_COLOR and 0xFFFFFF))
            regionsOnCell.forEach { (crop, region) ->
                val places = if (region.size == 1) "1 place" else "${region.size} places"
                add(Component.literal(" - ${crop.name} ($places)"))
            }
        }
    }

    fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean =
        isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)

    fun elementAtPos(mouseX: Double, mouseY: Double): Plant? =
        plantWidgets.firstOrNull { it.isMouseOver(mouseX, mouseY) }?.plant

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, x, y, gridSpan, gridSpan)

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        hoveredElement = plantWidgets.firstOrNull { it.isMouseOver(mouseX, mouseY) }
            ?.takeUnless { isShowingUnplannedMutations && it.plant.slot.mark != null }
    }

    companion object {
        const val LINE_WIDTH: Int = 1

        private const val UNPLANNED_VEIL_COLOR: Int = 0x88202020.toInt()
        const val UNPLANNED_MARK_COLOR: Int = 0xFFFFAA33.toInt()

        private val UNPLANNED_CROP_COLORS: List<Int> = listOf(
            0xFFFF5FD2.toInt(), 0xFFA96BFF.toInt(), 0xFF2FE0C0.toInt(),
            0xFFFF8FA3.toInt(), 0xFFF0F0F0.toInt(), 0xFFB98A5A.toInt()
        )

        private const val CROP_BORDER_SIZE: Int = 1

        private const val CHORUS_MARK_BORDER_SIZE: Int = 2
        private const val BREAK_CHORUS_COLOR: Int = 0xFFFF4040.toInt()
        private const val RIPE_CHORUS_COLOR: Int = 0xFF40E040.toInt()

        private const val VANISH_MS: Long = 150

        fun slotSizeFor(availableSpan: Int, slots: Int): Int = availableSpan / slots - LINE_WIDTH

        fun gridSpanFor(slotSize: Int, slots: Int): Int = slots * (slotSize + LINE_WIDTH)
    }
}
