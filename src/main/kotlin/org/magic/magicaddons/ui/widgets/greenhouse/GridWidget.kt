package org.magic.magicaddons.ui.widgets.greenhouse

import kotlin.math.abs
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
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GrowthClock
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.util.ScreenUtil.component4
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawCountedCrop
import org.magic.magicaddons.util.ScreenUtil.eased
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem
import org.magic.magicaddons.util.ScreenUtil.itemStackFor

class GridWidget(
    val layout: PlotLayout,
    val slotSize: Int
) : Renderable, HoverableContainer {

    /** How far a slot sits from the grid's corner: a slot and the line after it, counted that often. */
    private fun offsetOf(index: Int): Int = index * (slotSize + LINE_WIDTH)

    /** Every slot and the line after each, which is the whole grid across or down. */
    val gridSpan: Int get() = offsetOf(layout.size)

    private val slotWidgets = mutableListOf<SlotWidget>()
    private val elementWidgets = mutableListOf<ElementWidget>()

    var x: Int = 0
    var y: Int = 0

    override var hoveredElement: GuiEventListener? = null

    /** The fact pinned on the hover controls, written over every plant while it is set. */
    var pinnedInfo: ElementWidget.HoverInfo? = null

    var cropIdsWithoutPinnedInfo: Set<String> = emptySet()

    var targetPlan: () -> PlotLayout? = { null }

    var showUnplannedMutations: Boolean = false

    private var unplannedSpotsKey: Int? = null
    private var unplannedSpotsCache: Map<Pair<Int, Int>, List<CropDefinition>> = emptyMap()

    fun unplannedMutationSpots(): Map<Pair<Int, Int>, List<CropDefinition>> {
        val plan = targetPlan() ?: return emptyMap()
        val plannedCropsBySlot = PlotPrediction.targetCropsBySlot(plan)
        var key = plannedCropsBySlot.hashCode()
        layout.slots.forEach { key = key * 31 + (it.soil?.block?.hashCode() ?: 0) }
        layout.plants.forEach { key = key * 31 + (it.slot.x * 64 + it.slot.y) * 31 + it.cropDef.name.hashCode() + (it.slot.mark?.ordinal ?: -1) }

        if (key != unplannedSpotsKey) {
            unplannedSpotsCache = PlotPrediction.unplannedMutationSpots(layout, plannedCropsBySlot)
            unplannedSpotsKey = key
        }
        return unplannedSpotsCache
    }

    /** Plants placed since the last build, which arrive with a little pop. Cleared by [init]. */
    val justPlaced: MutableSet<Plant> = mutableSetOf()

    /** Plants marked since the last build, which flash once. Cleared by [init]. */
    val justMarked: MutableSet<Plant> = mutableSetOf()

    /** A plant taken off the grid, drawn shrinking away where it stood for a moment after. */
    private class Vanishing(val rect: IntArray, val stack: ItemStack, val at: Long)

    private val vanishing = mutableListOf<Vanishing>()

    /** Notes a plant about to be taken off, so it can be drawn shrinking away rather than gone at once. */
    fun noteVanishing(instance: Plant) {
        val footprint = instance.cropDef.footprint
        val rect = cellRect(instance.slot.x, instance.slot.y, footprint.width, footprint.height)

        vanishing.add(Vanishing(rect, itemStackFor(instance.cropDef), System.currentTimeMillis()))
    }

    /** Quarter turns clockwise the picture is given; the slots underneath never move. */
    var turns: Int = 0

    /** Where slot ([x], [y]) is drawn, as a cell of the turned picture. */
    private fun turned(x: Int, y: Int): Pair<Int, Int> {
        val last = layout.size - 1
        return when (Math.floorMod(turns, 4)) {
            1 -> (last - y) to x
            2 -> (last - x) to (last - y)
            3 -> y to (last - x)
            else -> x to y
        }
    }

    /** The slot drawn at cell ([cx], [cy]) of the turned picture. */
    private fun unturned(cx: Int, cy: Int): Pair<Int, Int> {
        val last = layout.size - 1
        return when (Math.floorMod(turns, 4)) {
            1 -> cy to (last - cx)
            2 -> (last - cx) to (last - cy)
            3 -> (last - cy) to cx
            else -> cx to cy
        }
    }

    /** The slot under a point, or null off the grid. */
    fun slotAt(mouseX: Double, mouseY: Double): Pair<Int, Int>? {
        val step = slotSize + LINE_WIDTH
        val cx = (mouseX.toInt() - x) / step
        val cy = (mouseY.toInt() - y) / step
        if (mouseX < x || mouseY < y || cx !in 0 until layout.size || cy !in 0 until layout.size) return null
        return unturned(cx, cy)
    }

    /** The screen rectangle a plant [width] by [height] slots anchored at slot ([sx], [sy]) covers. */
    fun cellRect(sx: Int, sy: Int, width: Int, height: Int): IntArray {
        val (ax, ay) = turned(sx, sy)
        val (bx, by) = turned(sx + width - 1, sy + height - 1)
        val left = minOf(ax, bx)
        val top = minOf(ay, by)
        val across = maxOf(ax, bx) - left + 1
        val down = maxOf(ay, by) - top + 1
        return intArrayOf(
            x + offsetOf(left),
            y + offsetOf(top),
            x + offsetOf(left) + slotSize * across + (across - 1),
            y + offsetOf(top) + slotSize * down + (down - 1)
        )
    }

    fun footprintRect(sx: Int, sy: Int, footprint: Footprint): IntArray = cellRect(sx, sy, footprint.width, footprint.height)

    /** Targets of one crop whose footprints run into each other, drawn as one region between them. */
    private fun overlappingTargetRuns(): List<List<Plant>> =
        layout.plants
            .filter { it.slot.mark == LayoutSlot.Marking.Target && it.growthStage == null }
            .groupBy { it.cropDef }
            .flatMap { (crop, targets) ->
                overlappingRuns(targets.map { it.slot.x to it.slot.y }, crop.footprint)
                    .filter { it.size > 1 }
                    .map { run -> run.mapNotNull { corner -> targets.find { (it.slot.x to it.slot.y) == corner } } }
            }

    /**
     * Corners of one crop grouped so that everything in a run takes a cell another one wants. Only
     * targets that really fight for room are drawn as one region; a plan of separate targets stays
     * as separate targets however close together they stand.
     */
    private fun overlappingRuns(corners: List<Pair<Int, Int>>, footprint: Footprint): List<List<Pair<Int, Int>>> =
        runsOf(corners) { one, other ->
            one.first < other.first + footprint.width && other.first < one.first + footprint.width &&
                    one.second < other.second + footprint.height && other.second < one.second + footprint.height
        }

    /**
     * Corners of one crop grouped so that everything in a run claims a cell touching the rest of it.
     * Crops a single cell wide never overlap each other, so touching rather than overlapping is what
     * gathers a scatter of them into one region.
     */
    private fun touchingRuns(corners: List<Pair<Int, Int>>, footprint: Footprint): List<List<Pair<Int, Int>>> =
        runsOf(corners) { one, other ->
            cellsTouch(claimedCells(listOf(one), footprint), claimedCells(listOf(other), footprint))
        }

    private fun runsOf(
        corners: List<Pair<Int, Int>>,
        belongTogether: (Pair<Int, Int>, Pair<Int, Int>) -> Boolean
    ): List<List<Pair<Int, Int>>> {
        val runs = mutableListOf<MutableList<Pair<Int, Int>>>()

        corners.forEach { corner ->
            val joined = runs.filter { run -> run.any { belongTogether(it, corner) } }
            val run = joined.firstOrNull() ?: mutableListOf<Pair<Int, Int>>().also { runs += it }

            joined.drop(1).forEach { absorbed ->
                run += absorbed
                runs.removeAll { it === absorbed }
            }
            run += corner
        }

        return runs
    }

    private fun cellsTouch(one: Set<Pair<Int, Int>>, other: Set<Pair<Int, Int>>): Boolean =
        one.any { (cellX, cellY) ->
            (cellX to cellY) in other || (cellX + 1 to cellY) in other || (cellX - 1 to cellY) in other ||
                    (cellX to cellY + 1) in other || (cellX to cellY - 1) in other
        }

    /** Every cell a crop standing at any of [corners] would take. */
    private fun claimedCells(corners: List<Pair<Int, Int>>, footprint: Footprint): Set<Pair<Int, Int>> =
        corners.flatMapTo(mutableSetOf()) { (cornerX, cornerY) ->
            (0 until footprint.width).flatMap { across ->
                (0 until footprint.height).map { down -> (cornerX + across) to (cornerY + down) }
            }
        }

    private fun cellRects(cells: Set<Pair<Int, Int>>): List<IntArray> =
        cells.map { (cellX, cellY) -> cellRect(cellX, cellY, 1, 1) }


    /** Only the outside edges of [cells], so no line is drawn through the middle of a region. */
    private fun outlineOf(cells: Set<Pair<Int, Int>>): List<IntArray> {
        val drawn = cells.mapTo(mutableSetOf()) { (cellX, cellY) -> turned(cellX, cellY) }
        val border = Common.UI.BORDER_SIZE

        return drawn.flatMap { (cx, cy) ->
            val left = x + offsetOf(cx)
            val top = y + offsetOf(cy)

            // an edge carries on over the line to the next cell of the region, so it reads as one
            val right = left + slotSize + if ((cx + 1 to cy) in drawn) LINE_WIDTH else 0
            val bottom = top + slotSize + if ((cx to cy + 1) in drawn) LINE_WIDTH else 0

            buildList {
                if ((cx to cy - 1) !in drawn) add(intArrayOf(left, top, right, top + border))
                if ((cx to cy + 1) !in drawn) add(intArrayOf(left, bottom - border, right, bottom))
                if ((cx - 1 to cy) !in drawn) add(intArrayOf(left, top, left + border, bottom))
                if ((cx + 1 to cy) !in drawn) add(intArrayOf(right - border, top, right, bottom))
            }
        }
    }

    fun init() {
        slotWidgets.clear()
        elementWidgets.clear()

        for (sx in 0 until layout.size) {
            for (sy in 0 until layout.size) {
                val slot = layout.getSlot(sx, sy) ?: continue

                val widget = SlotWidget(slot, layout.kind == PlotLayout.Kind.MASTER_PRESET)

                widget.width = slotSize
                widget.height = slotSize

                val (cx, cy) = turned(sx, sy)
                widget.x = x + offsetOf(cx)
                widget.y = y + offsetOf(cy)

                widget.init()

                slotWidgets.add(widget)
            }
        }

        val targetRuns = overlappingTargetRuns()
        val drawnByTheirRun = targetRuns.flatMap { it.drop(1) }
        val cellsHoldingAnIcon = mutableSetOf<Pair<Int, Int>>()

        layout.plants.forEach { instance ->
            if (drawnByTheirRun.any { it === instance }) return@forEach

            val widget = ElementWidget(instance)
            val run = targetRuns.firstOrNull { it.first() === instance }

            if (instance.slot.mark == LayoutSlot.Marking.Target && instance.cropDef.spawnRule != null) {
                // a run of overlapping targets is clear for as long as one of its corners is
                widget.missingSpawnConditions = (run ?: listOf(instance))
                    .map { PlotPrediction.missingConditionsForTarget(layout, it) }
                    .minBy { it.size }
            }

            widget.padding = slotSize / 10

            run?.let {
                val corners = it.map { target -> target.slot.x to target.slot.y }
                val cells = claimedCells(corners, instance.cropDef.footprint)
                val iconCell = freeCellIn(cells, cellsHoldingAnIcon)
                cellsHoldingAnIcon += iconCell

                widget.mergedTargets = ElementWidget.MergedTargets(
                    corners = corners.sortedWith(compareBy({ corner -> corner.second }, { corner -> corner.first })),
                    cells = cellRects(cells),
                    outline = outlineOf(cells),
                    iconCell = cellRect(iconCell.first, iconCell.second, 1, 1)
                )
            }

            // each axis swallows the lines between the slots it covers, and a crop is not always
            // square, so the axes cannot share one border count; turned, a wide crop may stand tall
            val footprint = instance.cropDef.footprint
            val rect = widget.mergedTargets?.let { merged ->
                intArrayOf(
                    merged.cells.minOf { cell -> cell[0] }, merged.cells.minOf { cell -> cell[1] },
                    merged.cells.maxOf { cell -> cell[2] }, merged.cells.maxOf { cell -> cell[3] }
                )
            } ?: cellRect(instance.slot.x, instance.slot.y, footprint.width, footprint.height)

            widget.x = rect[0]
            widget.y = rect[1]
            widget.width = rect[2] - rect[0]
            widget.height = rect[3] - rect[1]
            widget.waterEffect = GreenhouseGrid.waterEffectAt(layout, instance.slot)

            // the soggybuds still growing around the plant, each taking its share of its water
            widget.drinkers = layout.plantsSurrounding(instance).count { it.cropDef.drainsNeighbours && !it.isFullyGrown }

            // a soggybud's time is walked on the whole greenhouse, its donors drying out as they
            // will, once per build rather than every frame
            if (instance.cropDef.drainsNeighbours && layout.kind != PlotLayout.Kind.MASTER_PRESET) {
                val grid = GreenhouseData.greenhouseGrids.find { it.layout.id == layout.id }
                val tickMs = GrowthClock.tickLengthMs()
                if (grid != null && tickMs != null) {
                    widget.soggybudTicksToGrow = grid.ticksUntilGrown(layout, instance.slot, tickMs)
                    widget.soggybudSimulated = true
                }
            }

            widget.renderedStack = itemStackFor(instance.cropDef)
            if (instance in justPlaced) widget.appearedAt = System.currentTimeMillis()
            if (instance in justMarked) widget.markedAt = System.currentTimeMillis()
            widget.inPreset = layout.kind == PlotLayout.Kind.MASTER_PRESET
            elementWidgets.add(widget)
        }
        justPlaced.clear()
        justMarked.clear()
    }

    /** The plants taken off since a moment ago, each drawn smaller the longer it has been gone. */
    private fun renderVanishing(graphics: GuiGraphicsExtractor) {
        val now = System.currentTimeMillis()
        vanishing.removeAll { now - it.at >= VANISH_MS }

        vanishing.forEach { gone ->
            val shrink = 1f - eased(gone.at, VANISH_MS)
            val (left, top, right, bottom) = gone.rect
            val centerX = (left + right) / 2f
            val centerY = (top + bottom) / 2f
            val size = ((minOf(right - left, bottom - top) - slotSize / 5) * shrink).toInt()
            if (size <= 0) return@forEach

            graphics.renderFakeItem(gone.stack, (centerX - size / 2f).toInt(), (centerY - size / 2f).toInt(), size, size)
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        slotWidgets.forEach {
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }

        // the lines live in the pixel between two slots, drawn over the soil so no slot covers them
        // and under the plants so a wide plant covers them
        for (i in 1 until layout.size) {
            val at = offsetOf(i) - LINE_WIDTH
            graphics.fill(x + at, y, x + at + LINE_WIDTH, y + gridSpan, Common.UI.GRID_LINE_COLOR)
            graphics.fill(x, y + at, x + gridSpan, y + at + LINE_WIDTH, Common.UI.GRID_LINE_COLOR)
        }

        elementWidgets.forEach {
            it.planMuted = showUnplannedMutations
            it.extractRenderState(graphics, mouseX, mouseY, delta)
        }
        renderVanishing(graphics)
        if (showUnplannedMutations) renderUnplannedMutations(graphics)

        // drawn after every plant so the text of one never ends up under the plant next to it
        pinnedInfo?.let { info ->
            elementWidgets
                .filter { it.instance.cropDef.elementId !in cropIdsWithoutPinnedInfo }
                .forEach { it.renderHoverButtonInfo(graphics, info) }
        }
    }

    private fun renderUnplannedMutations(graphics: GuiGraphicsExtractor) {
        val font = Minecraft.getInstance().font
        val spots = unplannedMutationSpots()

        // a run that fills its own footprint has no say in where it is drawn, so it is placed first,
        // and the rest take the free cell nearest their middle, widest run first
        val runs = unplannedRuns(spots)
            .sortedWith(compareBy({ (_, run) -> if (run.size == 1) 0 else 1 }, { (_, run) -> -run.size }))
        val cellsByRun = runs.map { (crop, run) -> claimedCells(run, crop.footprint) }
        val colorByCrop = colorsFor(runs.map { it.first }.distinct().sortedBy { it.name })

        // one veil over everything, so a cell two crops could both take is not darkened twice
        cellRects(cellsByRun.flatten().toSet()).forEach { graphics.fill(it[0], it[1], it[2], it[3], UNPLANNED_VEIL_COLOR) }

        val taken = mutableSetOf<Pair<Int, Int>>()
        runs.forEachIndexed { index, (crop, run) ->
            val cells = cellsByRun[index]

            val color = colorByCrop.getValue(crop)
            val padding = slotSize / 10

            if (run.size == 1) {
                val rect = footprintRect(run[0].first, run[0].second, crop.footprint)
                graphics.renderFakeItem(
                    itemStackFor(crop), rect[0] + padding, rect[1] + padding,
                    rect[2] - rect[0] - padding * 2, rect[3] - rect[1] - padding * 2
                )
                graphics.drawBorder(rect[0] + padding, rect[1] + padding, rect[2] - padding, rect[3] - padding, CROP_BORDER_SIZE, color)
                taken += cells
            } else {
                val cell = freeCellIn(cells, taken)
                val rect = cellRect(cell.first, cell.second, 1, 1)

                graphics.drawCountedCrop(font, itemStackFor(crop), rect, run.size, color)
                graphics.drawBorder(rect[0] + padding, rect[1] + padding, rect[2] - padding, rect[3] - padding, CROP_BORDER_SIZE, color)
                taken += cell
            }
        }

        cellsByRun.forEachIndexed { index, cells ->
            val color = colorByCrop.getValue(runs[index].first)
            outlineOf(cells).forEach { graphics.fill(it[0], it[1], it[2], it[3], color) }
        }

        // the mark sits on the corner the crop would grow from, so the spots stay countable
        spots.keys.forEach { (cornerX, cornerY) ->
            val cell = cellRect(cornerX, cornerY, 1, 1)
            graphics.text(font, Component.literal("!"), cell[2] - font.width("!") - 2, cell[1] + 2, UNPLANNED_MARK_COLOR, true)
        }
    }

    /** A colour each, so two crops claiming the same cells can still be told apart. */
    private fun colorsFor(crops: List<CropDefinition>): Map<CropDefinition, Int> =
        crops.withIndex().associate { (index, crop) -> crop to UNPLANNED_CROP_COLORS[index % UNPLANNED_CROP_COLORS.size] }

    /** Each crop that can grow where it was not planned, and the runs of corners it could grow from. */
    private fun unplannedRuns(
        spots: Map<Pair<Int, Int>, List<CropDefinition>>
    ): List<Pair<CropDefinition, List<Pair<Int, Int>>>> =
        spots.entries
            .flatMap { (corner, crops) -> crops.map { it to corner } }
            .groupBy({ it.first }, { it.second })
            .flatMap { (crop, corners) -> touchingRuns(corners, crop.footprint).map { crop to it } }

    /**
     * The cell of [cells] nearest their middle that no icon has taken. An icon covers exactly one
     * cell, so a free cell is one no other icon sits on; with every cell taken, the one standing
     * furthest from the nearest icon is used.
     */
    private fun freeCellIn(cells: Set<Pair<Int, Int>>, taken: Set<Pair<Int, Int>>): Pair<Int, Int> {
        val midX = cells.sumOf { it.first }.toDouble() / cells.size
        val midY = cells.sumOf { it.second }.toDouble() / cells.size
        val byDistanceFromMiddle = cells.sortedBy { abs(it.first - midX) + abs(it.second - midY) }

        byDistanceFromMiddle.firstOrNull { it !in taken }?.let { return it }

        return byDistanceFromMiddle.maxBy { cell ->
            taken.minOf { abs(cell.first - it.first) + abs(cell.second - it.second) }
        }
    }

    /** What can grow unplanned on the cell under the cursor, with how many corners each run has. */
    fun unplannedTooltipAt(mouseX: Double, mouseY: Double): List<Component>? {
        if (!showUnplannedMutations) return null
        val cell = slotAt(mouseX, mouseY) ?: return null

        val here = unplannedRuns(unplannedMutationSpots())
            .filter { (crop, run) -> cell in claimedCells(run, crop.footprint) }
        if (here.isEmpty()) return null

        return buildList {
            add(Component.literal("Can grow unplanned").withColor(UNPLANNED_MARK_COLOR and 0xFFFFFF))
            here.forEach { (crop, run) ->
                val places = if (run.size == 1) "1 place" else "${run.size} places"
                add(Component.literal(" - ${crop.name} ($places)"))
            }
        }
    }

    /** A click on the grid is taken here, so it never falls through to whatever lies under it. */
    fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean =
        isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)

    fun elementAtPos(mouseX: Double, mouseY: Double): Plant? =
        elementWidgets.firstOrNull { it.isMouseOver(mouseX, mouseY) }?.instance

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        inRect(mouseX, mouseY, x, y, gridSpan, gridSpan)

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        // while the unplanned overlay is up the plan has stepped back, its hover included
        hoveredElement = elementWidgets.firstOrNull { it.isMouseOver(mouseX, mouseY) }
            ?.takeUnless { showUnplannedMutations && it.instance.slot.mark != null }
    }

    companion object {
        /** The line drawn between one slot and the next, and around the outside. */
        const val LINE_WIDTH: Int = 1

        private const val UNPLANNED_VEIL_COLOR: Int = 0x88202020.toInt()
        const val UNPLANNED_MARK_COLOR: Int = 0xFFFFAA33.toInt()

        /** Kept clear of the blue and green the marks use, and of the orange of the unplanned text. */
        private val UNPLANNED_CROP_COLORS: List<Int> = listOf(
            0xFFFF5FD2.toInt(), 0xFFA96BFF.toInt(), 0xFF2FE0C0.toInt(),
            0xFFFF8FA3.toInt(), 0xFFF0F0F0.toInt(), 0xFFB98A5A.toInt()
        )

        private const val CROP_BORDER_SIZE: Int = 1

        /** How long a plant taken off keeps shrinking where it stood. */
        private const val VANISH_MS: Long = 150

        /**
         * The largest slot that fits a grid into the room available, lines included. Divided once,
         * here, so nothing else lands a pixel out.
         */
        fun slotSizeFor(room: Int, slots: Int): Int = room / slots - LINE_WIDTH

        /** What a grid of [slots] at [slotSize] takes up, which is the span plus its closing line. */
        fun spanFor(slotSize: Int, slots: Int): Int = slots * (slotSize + LINE_WIDTH)
    }
}
