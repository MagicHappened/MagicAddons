package org.magic.magicaddons.ui.screens

import org.magic.magicaddons.util.ScreenUtil.modText
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.magic.magicaddons.Common
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.hud.AnchorState
import org.magic.magicaddons.ui.hud.ElementState
import org.magic.magicaddons.ui.hud.GroupState
import org.magic.magicaddons.ui.hud.HudBox
import org.magic.magicaddons.ui.hud.HudElement
import org.magic.magicaddons.ui.hud.HudElements
import org.magic.magicaddons.ui.hud.HudSituation
import org.magic.magicaddons.ui.widgets.SwitchWidget
import org.magic.magicaddons.ui.hud.HudLayoutFile
import org.magic.magicaddons.ui.hud.HudPainter
import org.magic.magicaddons.ui.hud.HudPart
import org.magic.magicaddons.ui.hud.HudScene
import org.magic.magicaddons.ui.hud.HudPlacement
import org.magic.magicaddons.ui.hud.Positioning
import org.magic.magicaddons.ui.hud.Stacking
import org.magic.magicaddons.ui.hud.PlacementTie
import org.magic.magicaddons.ui.ScreenRect
import org.magic.magicaddons.ui.hud.SizedPlacement
import org.magic.magicaddons.ui.widgets.hud.HudMenu
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.setScreen
import kotlin.math.abs
import kotlin.math.roundToInt

class HudEditorScreen : MagicAddonsScreen(Component.literal("HUD Editor"), "the hud editor"), OverlayContext {

    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    private val layout get() = HudLayoutFile.layout

    private var scene: HudScene = HudScene(emptyList(), emptyList())

    private enum class Edge { TOP, BOTTOM, LEFT, RIGHT }

    private class Corner(val isLeft: Boolean, val isTop: Boolean)

    private sealed class Drag(val id: String) {
        class Move(id: String, val offsetX: Int, val offsetY: Int) : Drag(id)
        class Resize(id: String, val isLeftSide: Boolean, val isTopSide: Boolean, val anchoredX: Int, val anchoredY: Int) : Drag(id)
        class Divider(id: String, val partIndex: Int, val startMouse: Int, val startSizeBefore: Int, val startSizeAfter: Int) : Drag(id)
    }

    private var selectedId: String? = null

    private var drag: Drag? = null

    private var mergeTarget: Pair<HudBox, Edge>? = null

    private var tieSourceId: String? = null

    private var mouseX = 0
    private var mouseY = 0

    private var situation: HudSituation = HudSituation.currentSituation()

    private var isSituationPanelOpen = false

    private val elementSwitches = mutableMapOf<String, SwitchWidget>()

    private fun isShownInSituation(element: HudElement): Boolean = element.showsIn(situation) && !layout.isHidden(situation, element.id)

    private fun rebuildScene() {
        scene = HudScene.buildScene(layout, width, height, useSamples = true, isIncluded = ::isShownInSituation)
    }

    override fun onInit() {
        super.onInit()
        closeOverlays()
        rebuildScene()
    }

    override fun isPauseScreen(): Boolean = false

    private fun boxOrCornerAt(x: Double, y: Double): HudBox? =
        scene.boxUnderCursor(x, y) ?: scene.boxes.lastOrNull { cornerAt(it, x, y) != null }

    private fun hoveredBox(): HudBox? = boxOrCornerAt(mouseX.toDouble(), mouseY.toDouble())
    private fun hoveredPart(): HudPart? = scene.partUnderCursor(mouseX.toDouble(), mouseY.toDouble())

    private fun draggedBox(): HudBox? = drag?.let { scene.boxOf(it.id) }

    private fun cornerAt(box: HudBox, x: Double, y: Double): Corner? =
        CORNERS.firstOrNull { corner ->
            val cornerX = if (corner.isLeft) box.x else box.x + box.width
            val cornerY = if (corner.isTop) box.y else box.y + box.height
            abs(x - cornerX) <= HANDLE_GRAB_DISTANCE && abs(y - cornerY) <= HANDLE_GRAB_DISTANCE
        }

    private fun displayNameOf(id: String): String =
        HudElements.elementById(id)?.name
            ?: layout.groupById(id)?.let { group -> group.members.mapNotNull { HudElements.elementById(it)?.name }.joinToString(" + ") }
            ?: layout.anchorById(id)?.let { "Anchor ${it.id.substringAfter('-')}" }
            ?: id

    private fun moveBoxTo(id: String, left: Int, top: Int) {
        val placement = layout.placementById(id) ?: return
        val rect = scene.rectOf(id) ?: return
        val tie = placement.tie
        val target = tie?.let { scene.rectOf(it.target) }
        if (tie != null && target != null) {
            tie.offsetX = left - target.x
            tie.offsetY = top - target.y
        } else {
            placement.placeRect(ScreenRect(left, top, rect.width, rect.height), width, height)
        }
        rebuildScene()
    }

    private fun resizeBoxTo(box: HudBox, drag: Drag.Resize, mouseX: Int, mouseY: Int) {
        val wantedWidth = (if (drag.isLeftSide) drag.anchoredX - mouseX else mouseX - box.x).coerceIn(box.minWidth, width)
        val wantedHeight = (if (drag.isTopSide) drag.anchoredY - mouseY else mouseY - box.y).coerceIn(box.minHeight, height)
        val placement = box.placement as? SizedPlacement ?: return
        holdCurrentSize(box)
        when {
            placement is GroupState && placement.stacking == Stacking.VERTICAL -> {
                placement.width = wantedWidth
                shareSizeInProportion(box.parts, wantedHeight - (box.parts.size - 1) * HudScene.DIVIDER_THICKNESS, { it.height }, { it.minHeight }) { part, size -> part.state.height = size }
            }
            placement is GroupState -> {
                placement.height = wantedHeight
                shareSizeInProportion(box.parts, wantedWidth - (box.parts.size - 1) * HudScene.DIVIDER_THICKNESS, { it.width }, { it.minWidth }) { part, size -> part.state.width = size }
            }
            else -> {
                placement.width = wantedWidth
                placement.height = wantedHeight
            }
        }
        val left = if (drag.isLeftSide) drag.anchoredX - wantedWidth else box.x
        val top = if (drag.isTopSide) drag.anchoredY - wantedHeight else box.y
        val tie = placement.tie
        val target = tie?.let { scene.rectOf(it.target) }
        if (tie != null && target != null) {
            tie.offsetX = left - target.x
            tie.offsetY = top - target.y
        } else {
            placement.placeRect(ScreenRect(left, top, wantedWidth, wantedHeight), width, height)
        }
        rebuildScene()
    }

    private fun shareSizeInProportion(parts: List<HudPart>, total: Int, size: (HudPart) -> Int, minimum: (HudPart) -> Int, set: (HudPart, Int) -> Unit) {
        val currentTotal = parts.sumOf { size(it) }.coerceAtLeast(1)
        var remaining = total
        parts.forEachIndexed { index, part ->
            val proportionalSize = if (index == parts.size - 1) remaining else (size(part).toFloat() / currentTotal * total).roundToInt()
            val givenSize = proportionalSize.coerceAtLeast(minimum(part))
            set(part, givenSize)
            remaining -= givenSize
        }
    }

    private fun holdCurrentSize(box: HudBox) {
        val placement = box.placement as? SizedPlacement ?: return
        if (!placement.isSizedByContent) return
        placement.isSizedByContent = false
        placement.width = box.width
        placement.height = box.height
        if (placement is GroupState) {
            box.parts.forEach {
                it.state.width = it.width
                it.state.height = it.height
            }
        }
    }

    private fun isSizedByContent(box: HudBox): Boolean = (box.placement as? SizedPlacement)?.isSizedByContent ?: true

    private fun toggleSizing(box: HudBox) {
        if (isSizedByContent(box)) {
            holdCurrentSize(box)
        } else {
            (box.placement as? SizedPlacement)?.isSizedByContent = true
        }
        rebuildScene()
    }

    private fun dragDivider(box: HudBox, drag: Drag.Divider, mousePosition: Int) {
        holdCurrentSize(box)
        val partBefore = box.parts[drag.partIndex]
        val partAfter = box.parts[drag.partIndex + 1]
        val isVertical = box.stacking == Stacking.VERTICAL
        val minBefore = if (isVertical) partBefore.minHeight else partBefore.minWidth
        val minAfter = if (isVertical) partAfter.minHeight else partAfter.minWidth
        val movedBy = (mousePosition - drag.startMouse).coerceIn(minBefore - drag.startSizeBefore, drag.startSizeAfter - minAfter)
        if (isVertical) {
            partBefore.state.height = drag.startSizeBefore + movedBy
            partAfter.state.height = drag.startSizeAfter - movedBy
        } else {
            partBefore.state.width = drag.startSizeBefore + movedBy
            partAfter.state.width = drag.startSizeAfter - movedBy
        }
        rebuildScene()
    }

    private fun nudgeSelected(dx: Int, dy: Int) {
        val id = selectedId ?: return
        val rect = scene.rectOf(id) ?: return
        moveBoxTo(id, rect.x + dx, rect.y + dy)
    }

    private fun resizeSelectedByKey(dx: Int, dy: Int, topLeft: Boolean) {
        val box = selectedId?.let { scene.boxOf(it) } ?: return
        val drag = if (topLeft) {
            Drag.Resize(box.id, isLeftSide = true, isTopSide = true, anchoredX = box.x + box.width, anchoredY = box.y + box.height)
        } else {
            Drag.Resize(box.id, isLeftSide = false, isTopSide = false, anchoredX = box.x, anchoredY = box.y)
        }
        val cornerX = if (topLeft) box.x else box.x + box.width
        val cornerY = if (topLeft) box.y else box.y + box.height
        resizeBoxTo(box, drag, cornerX + dx, cornerY + dy)
    }

    private fun mergeIntoBox(elementId: String, target: HudBox, edge: Edge) {
        val state = layout.elements[elementId] ?: return
        val group = target.group
        val atStart = edge == Edge.TOP || edge == Edge.LEFT
        if (group != null) {
            if (atStart) group.members.add(0, elementId) else group.members.add(elementId)
        } else {
            val stacking = if (edge == Edge.TOP || edge == Edge.BOTTOM) Stacking.VERTICAL else Stacking.HORIZONTAL
            val members = if (atStart) listOf(elementId, target.id) else listOf(target.id, elementId)
            val newGroup = layout.addGroup(members, stacking)
            val targetPlacement = target.placement
            newGroup.positioning = targetPlacement.positioning
            newGroup.x = targetPlacement.x
            newGroup.y = targetPlacement.y
            newGroup.fractionX = targetPlacement.fractionX
            newGroup.fractionY = targetPlacement.fractionY
            newGroup.tie = targetPlacement.tie
            (targetPlacement as? ElementState)?.let {
                newGroup.alpha = it.alpha
                newGroup.width = it.width
                it.tie = null
            }
            (layout.elements.values + layout.groups + layout.anchors).forEach { placement ->
                if (placement.tie?.target == target.id) placement.tie?.target = newGroup.id
            }
        }
        state.tie = null
        rebuildScene()
    }

    private fun splitFromGroup(elementId: String) {
        val group = layout.groupContaining(elementId) ?: return
        val part = scene.boxOf(group.id)?.parts?.firstOrNull { it.element.id == elementId }
        val state = layout.elements[elementId] ?: return

        group.members.remove(elementId)
        if (part != null) state.placeRect(ScreenRect(part.x, part.y, part.width, part.height), width, height)
        state.tie = null

        if (group.members.size <= 1) {
            val rects = scene.rectById()
            group.members.firstOrNull()?.let { remainingId ->
                val remainingElement = layout.elements[remainingId] ?: return@let
                val remainingPart = scene.boxOf(group.id)?.parts?.firstOrNull { it.element.id == remainingId }
                remainingElement.positioning = group.positioning
                remainingElement.alpha = group.alpha
                if (remainingPart != null) {
                    remainingElement.placeRect(ScreenRect(remainingPart.x, remainingPart.y, remainingPart.width, remainingPart.height), width, height)
                    remainingElement.tie = group.tie?.let { PlacementTie(it.target, it.offsetX + remainingPart.x - group.x, it.offsetY + remainingPart.y - group.y) }
                } else {
                    remainingElement.x = group.x
                    remainingElement.y = group.y
                    remainingElement.fractionX = group.fractionX
                    remainingElement.fractionY = group.fractionY
                    remainingElement.tie = group.tie
                }
                (layout.elements.values + layout.groups + layout.anchors).forEach { placement ->
                    if (placement.tie?.target == group.id) placement.tie?.target = remainingId
                }
            }
            layout.untieEverythingFrom(group.id, rects, width, height)
            layout.groups.remove(group)
        }
        rebuildScene()
    }

    private fun resetElement(elementId: String) {
        val element = HudElements.elementById(elementId) ?: return
        splitFromGroup(elementId)
        layout.untieEverythingFrom(elementId, scene.rectById(), width, height)
        layout.elements[elementId] = layout.defaultStateOf(element)
        rebuildScene()
    }

    private fun untie(id: String) {
        val placement = layout.placementById(id) ?: return
        val rect = scene.rectOf(id) ?: return
        placement.placeRect(rect, width, height)
        placement.tie = null
        rebuildScene()
    }

    private fun togglePositioning(id: String) {
        val placement = layout.placementById(id) ?: return
        val rect = scene.rectOf(id) ?: return
        placement.placeRect(rect, width, height)
        placement.positioning = if (placement.positioning == Positioning.ABSOLUTE) Positioning.RELATIVE else Positioning.ABSOLUTE
        rebuildScene()
    }

    private fun tieTo(id: String, targetId: String) {
        if (id == targetId || layout.wouldTieLoop(id, targetId)) return
        val placement = layout.placementById(id) ?: return
        val rect = scene.rectOf(id) ?: return
        val target = scene.rectOf(targetId) ?: return
        placement.tie = PlacementTie(targetId, rect.x - target.x, rect.y - target.y)
        rebuildScene()
    }

    private fun removeAnchor(anchor: AnchorState) {
        layout.untieEverythingFrom(anchor.id, scene.rectById(), width, height)
        layout.anchors.remove(anchor)
        rebuildScene()
    }

    private fun addAnchor(x: Int, y: Int) {
        layout.addAnchor().placeRect(ScreenRect(x, y, 0, 0), width, height)
        rebuildScene()
    }

    private fun untieEverythingFrom(id: String) {
        layout.untieEverythingFrom(id, scene.rectById(), width, height)
        rebuildScene()
    }

    private fun openConfig(part: HudPart) {
        val target = part.element.configTarget ?: return
        setScreen(ConfigScreen(this).apply { showSetting(target.feature, target.path) })
    }

    private fun openMenu(x: Int, y: Int, title: String, entries: List<HudMenu.Entry>) {
        addContext(HudMenu(x, y, title, entries, this).also { it.init() })
    }

    private fun placementMenuEntries(id: String): List<HudMenu.Entry> {
        val placement = layout.placementById(id) ?: return emptyList()
        return buildList {
            val tie = placement.tie
            if (tie != null) {
                add(HudMenu.Entry("Untie from ${displayNameOf(tie.target)}") { untie(id) })
            } else {
                val otherPositioning = if (placement.positioning == Positioning.ABSOLUTE) "Relative" else "Absolute"
                add(HudMenu.Entry("Position: $otherPositioning") { togglePositioning(id) })
            }
            add(HudMenu.Entry("Tie to…") { tieSourceId = id })
        }
    }

    private fun openPartMenu(part: HudPart, box: HudBox, x: Int, y: Int) {
        val entries = buildList {
            if (part.element.configTarget != null) add(HudMenu.Entry("Open config") { openConfig(part) })
            addAll(placementMenuEntries(box.id))
            add(HudMenu.Entry(if (isSizedByContent(box)) "Sizing: fixed" else "Sizing: dynamic") { toggleSizing(box) })
            if (box.group != null) add(HudMenu.Entry("Split off") { splitFromGroup(part.element.id) })
            add(HudMenu.Entry("Reset") { resetElement(part.element.id) })
            if (idsTiedTo(box.id).isNotEmpty()) add(HudMenu.Entry("Untie all from this") { untieEverythingFrom(box.id) })
        }
        openMenu(x, y, part.element.name, entries)
    }

    private fun openAnchorMenu(anchor: AnchorState, x: Int, y: Int) {
        val entries = buildList {
            addAll(placementMenuEntries(anchor.id))
            add(HudMenu.Entry("Remove") { removeAnchor(anchor) })
            if (idsTiedTo(anchor.id).isNotEmpty()) add(HudMenu.Entry("Untie all from this") { untieEverythingFrom(anchor.id) })
        }
        openMenu(x, y, displayNameOf(anchor.id), entries)
    }

    override fun onExtractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, Common.UI.SCREEN_DIM_COLOR)
    }

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.onRender(graphics, mouseX, mouseY, delta)
        this.mouseX = mouseX
        this.mouseY = mouseY
        rebuildScene()

        scene.drawBoxes(graphics)

        val hovered = if (overlays.isEmpty()) hoveredBox() else null
        val shown = draggedBox() ?: hovered
        val selected = selectedId?.let { scene.boxOf(it) }
        selected?.let { drawHandles(graphics, it) }
        shown?.takeIf { it !== selected }?.let { drawHandles(graphics, it) }
        drawAnchors(graphics)
        drawTies(graphics, listOfNotNull(shown?.id, selectedId).distinct())
        shown?.let { drawDividerHandle(graphics, it) }
        drawTieBeingMade(graphics)
        mergeTarget?.let { (box, edge) -> drawMergeEdge(graphics, box, edge) }

        if (overlays.isEmpty()) {
            val part = hoveredPart()
            val anchor = scene.anchorUnderCursor(mouseX.toDouble(), mouseY.toDouble())
            when {
                shown != null && part != null -> drawPartInfo(graphics, shown, part)
                anchor != null -> drawAnchorInfo(graphics, anchor.state, anchor.x, anchor.y)
            }
        }
        drawHints(graphics)
        drawSituationPanel(graphics)
        renderOverlays(graphics, mouseX, mouseY, delta)
    }

    private class SituationPanelRow(val top: Int, val situation: HudSituation? = null, val element: HudElement? = null)

    private fun situationPanelRows(): List<SituationPanelRow> {
        var rowY = situationPanelTop() + PANEL_PADDING + PANEL_ROW_HEIGHT
        return buildList {
            HudSituation.situationsToOffer().forEach { candidate ->
                add(SituationPanelRow(rowY, situation = candidate))
                rowY += PANEL_ROW_HEIGHT
            }
            rowY += PANEL_PADDING * 2 + 1
            HudElements.all.filter { it.showsIn(situation) }.forEach { element ->
                add(SituationPanelRow(rowY, element = element))
                rowY += PANEL_ROW_HEIGHT
            }
        }
    }

    private fun situationPanelHeight(): Int {
        val rows = situationPanelRows()
        return PANEL_PADDING * 2 + PANEL_ROW_HEIGHT * (1 + rows.size) + PANEL_PADDING * 2 + 1
    }

    private fun situationPanelLeft(): Int = (width - PANEL_WIDTH) / 2
    private fun situationPanelRight(): Int = situationPanelLeft() + PANEL_WIDTH
    private fun situationPanelTop(): Int = TAB_HEIGHT
    private fun tabLeft(): Int = (width - TAB_WIDTH) / 2

    private fun isOverPanelTab(x: Int, y: Int): Boolean = y < TAB_HEIGHT && x in tabLeft() until tabLeft() + TAB_WIDTH

    private fun isOverSituationPanel(x: Int, y: Int): Boolean =
        isSituationPanelOpen && x in situationPanelLeft() until situationPanelRight() && y in situationPanelTop() until situationPanelTop() + situationPanelHeight()

    private fun drawSituationPanel(graphics: GuiGraphicsExtractor) {
        val tabLeft = tabLeft()
        val isTabHovered = isOverPanelTab(mouseX, mouseY)
        graphics.drawButtonPanel(tabLeft, 0, tabLeft + TAB_WIDTH, TAB_HEIGHT, isTabHovered, pressed = isSituationPanelOpen)
        val midX = tabLeft + TAB_WIDTH / 2
        val midY = TAB_HEIGHT / 2
        val tipY = if (isSituationPanelOpen) midY - 3 else midY + 3
        val baseY = if (isSituationPanelOpen) midY + 3 else midY - 3
        graphics.drawLine(midX - 3, baseY, midX, tipY, 1, Common.UI.TEXT_COLOR)
        graphics.drawLine(midX, tipY, midX + 3, baseY, 1, Common.UI.TEXT_COLOR)

        if (!isSituationPanelOpen) return
        val left = situationPanelLeft()
        val right = situationPanelRight()
        val panelTop = situationPanelTop()
        graphics.drawPanel(left, panelTop, right, panelTop + situationPanelHeight())

        val rows = situationPanelRows()
        graphics.modText(font, Component.literal("Show"), left + PANEL_PADDING, panelTop + PANEL_PADDING + (PANEL_ROW_HEIGHT - font.lineHeight) / 2, Common.UI.TEXT_DIM_COLOR)

        rows.firstOrNull { it.element != null }?.let { firstElementRow ->
            val lineY = firstElementRow.top - PANEL_PADDING - 1
            graphics.fill(left + PANEL_PADDING, lineY, right - PANEL_PADDING, lineY + 1, Common.UI.THIN_DIVIDER_COLOR)
        }

        rows.forEach { row ->
            val rowY = row.top
            val isRowHovered = mouseX in left until right && mouseY in rowY until rowY + PANEL_ROW_HEIGHT
            val textY = rowY + (PANEL_ROW_HEIGHT - font.lineHeight) / 2

            row.situation?.let { candidate ->
                if (candidate == situation) {
                    graphics.fill(left + Common.UI.BORDER_SIZE, rowY, right - Common.UI.BORDER_SIZE, rowY + PANEL_ROW_HEIGHT, Common.UI.PRESSED_SHADE)
                    graphics.fill(left + Common.UI.BORDER_SIZE, rowY, left + Common.UI.BORDER_SIZE + 2, rowY + PANEL_ROW_HEIGHT, Common.UI.SELECTED_FRAME_COLOR)
                } else if (isRowHovered) {
                    graphics.fill(left + Common.UI.BORDER_SIZE, rowY, right - Common.UI.BORDER_SIZE, rowY + PANEL_ROW_HEIGHT, Common.UI.HOVER_WASH)
                }
                graphics.modText(font, Component.literal(candidate.label), left + PANEL_PADDING + 3, textY, if (candidate == situation) Common.UI.TEXT_COLOR else Common.UI.TEXT_DIM_COLOR)
            }

            row.element?.let { element ->
                val isShown = !layout.isHidden(situation, element.id)
                if (isRowHovered) graphics.fill(left + Common.UI.BORDER_SIZE, rowY, right - Common.UI.BORDER_SIZE, rowY + PANEL_ROW_HEIGHT, Common.UI.HOVER_WASH)
                graphics.modText(font, Component.literal(element.name), left + PANEL_PADDING + 3, textY, if (isShown) Common.UI.TEXT_COLOR else Common.UI.DISABLED_TEXT_COLOR)
                val switch = elementSwitches.getOrPut(element.id) { SwitchWidget(isShown, PANEL_SWITCH_WIDTH, PANEL_SWITCH_HEIGHT) }
                switch.set(isShown)
                switch.x = right - PANEL_PADDING - switch.width
                switch.y = rowY + (PANEL_ROW_HEIGHT - switch.height) / 2
                switch.render(graphics)
            }
        }
    }

    private fun situationPanelClicked(x: Int, y: Int, button: Int): Boolean {
        if (isOverPanelTab(x, y)) {
            if (button == 0) isSituationPanelOpen = !isSituationPanelOpen
            return true
        }
        if (!isSituationPanelOpen) return false
        if (!isOverSituationPanel(x, y)) {
            isSituationPanelOpen = false
            return true
        }
        if (button != 0) return true

        val row = situationPanelRows().firstOrNull { y in it.top until it.top + PANEL_ROW_HEIGHT } ?: return true
        row.situation?.let { candidate ->
            situation = candidate
            selectedId = null
            rebuildScene()
        }
        row.element?.let { element ->
            layout.setHidden(situation, element.id, !layout.isHidden(situation, element.id))
            if (selectedId == element.id) selectedId = null
            rebuildScene()
            HudLayoutFile.save()
        }
        return true
    }

    private fun drawHandles(graphics: GuiGraphicsExtractor, box: HudBox) {
        graphics.drawBorder(box.x, box.y, box.x + box.width, box.y + box.height, 1, Common.UI.SELECTED_FRAME_COLOR)
        listOf(box.x to box.y, box.x + box.width to box.y, box.x to box.y + box.height, box.x + box.width to box.y + box.height).forEach { (cornerX, cornerY) ->
            graphics.fill(cornerX - HANDLE_HALF_SIZE, cornerY - HANDLE_HALF_SIZE, cornerX + HANDLE_HALF_SIZE + 1, cornerY + HANDLE_HALF_SIZE + 1, Common.UI.SELECTED_FRAME_COLOR)
        }
    }

    private fun drawAnchors(graphics: GuiGraphicsExtractor) {
        val half = HudScene.ANCHOR_HALF_SIZE
        scene.anchors.forEach { anchor ->
            val isHighlighted = scene.anchorUnderCursor(mouseX.toDouble(), mouseY.toDouble()) === anchor || anchor.state.id == selectedId
            HudPainter.drawBoxPanel(graphics, anchor.x - half, anchor.y - half, half * 2 + 1, half * 2 + 1, anchor.state.alpha)
            if (isHighlighted) graphics.drawBorder(anchor.x - half, anchor.y - half, anchor.x + half + 1, anchor.y + half + 1, 1, Common.UI.SELECTED_FRAME_COLOR)
            val color = if (isHighlighted) Common.UI.TEXT_COLOR else Common.UI.SELECTED_FRAME_COLOR
            graphics.fill(anchor.x - ANCHOR_CROSS_ARM, anchor.y, anchor.x + ANCHOR_CROSS_ARM + 1, anchor.y + 1, color)
            graphics.fill(anchor.x, anchor.y - ANCHOR_CROSS_ARM, anchor.x + 1, anchor.y + ANCHOR_CROSS_ARM + 1, color)
            graphics.text(font, Component.literal(displayNameOf(anchor.state.id)), anchor.x + half + 3, anchor.y - font.lineHeight / 2, Common.UI.TEXT_DIM_COLOR, true)
        }
    }

    private fun drawDividerHandle(graphics: GuiGraphicsExtractor, box: HudBox) {
        val partIndex = (drag as? Drag.Divider)?.partIndex ?: box.dividerAt(mouseX.toDouble(), mouseY.toDouble()) ?: return
        val partAfter = box.parts.getOrNull(partIndex + 1) ?: return
        val color = Common.UI.SELECTED_FRAME_COLOR
        if (box.stacking == Stacking.HORIZONTAL) {
            val lineX = partAfter.x - HudScene.DIVIDER_THICKNESS
            graphics.fill(lineX - 1, box.y, lineX + 2, box.y + box.height, color)

            for (bar in -1..1) {
                val barY = box.centerY + bar * GRIP_GAP
                graphics.fill(lineX - GRIP_REACH, barY, lineX + GRIP_REACH + 1, barY + 1, color)
            }
        } else {
            val lineY = partAfter.y - HudScene.DIVIDER_THICKNESS
            graphics.fill(box.x, lineY - 1, box.x + box.width, lineY + 2, color)

            for (bar in -1..1) {
                val barX = box.centerX + bar * GRIP_GAP
                graphics.fill(barX, lineY - GRIP_REACH, barX + 1, lineY + GRIP_REACH + 1, color)
            }
        }
    }

    private fun centerOf(id: String): Pair<Int, Int>? =
        scene.boxOf(id)?.let { it.centerX to it.centerY }
            ?: scene.anchors.firstOrNull { it.state.id == id }?.let { it.x to it.y }

    private fun idsTiedTo(id: String): List<String> = buildList {
        layout.elements.forEach { (elementId, state) -> if (state.tie?.target == id) add(elementId) }
        layout.groups.forEach { if (it.tie?.target == id) add(it.id) }
        layout.anchors.forEach { if (it.tie?.target == id) add(it.id) }
    }

    private fun drawTies(graphics: GuiGraphicsExtractor, shownIds: List<String>) {
        val tieIds = shownIds + shownIds.flatMap { idsTiedTo(it) }
        (tieIds + scene.anchors.map { it.state.id }).distinct().forEach { id ->
            val tie = layout.placementById(id)?.tie ?: return@forEach
            val (fromX, fromY) = centerOf(id) ?: return@forEach
            val (toX, toY) = centerOf(tie.target) ?: return@forEach
            graphics.drawLine(fromX, fromY, toX, toY, 1, Common.UI.SELECTED_FRAME_COLOR)
            graphics.fill(toX - 2, toY - 2, toX + 3, toY + 3, Common.UI.SELECTED_FRAME_COLOR)
        }
    }

    private fun drawTieBeingMade(graphics: GuiGraphicsExtractor) {
        val sourceId = tieSourceId ?: return
        val (fromX, fromY) = centerOf(sourceId) ?: return
        graphics.drawLine(fromX, fromY, mouseX, mouseY, 1, Common.UI.SELECTED_FRAME_COLOR)
        val targetId = tieTargetUnderCursor() ?: return
        scene.boxOf(targetId)?.let { box ->
            graphics.drawBorder(box.x - 2, box.y - 2, box.x + box.width + 2, box.y + box.height + 2, 2, Common.UI.SELECTED_FRAME_COLOR)
        }
        scene.anchors.firstOrNull { it.state.id == targetId }?.let { anchor ->
            graphics.drawBorder(anchor.x - ANCHOR_CROSS_ARM - 2, anchor.y - ANCHOR_CROSS_ARM - 2, anchor.x + ANCHOR_CROSS_ARM + 3, anchor.y + ANCHOR_CROSS_ARM + 3, 1, Common.UI.SELECTED_FRAME_COLOR)
        }
    }

    private fun tieTargetUnderCursor(): String? {
        val sourceId = tieSourceId ?: return null
        val id = scene.anchorUnderCursor(mouseX.toDouble(), mouseY.toDouble())?.state?.id ?: hoveredBox()?.id ?: return null
        if (id == sourceId || layout.wouldTieLoop(sourceId, id)) return null
        return id
    }

    private fun drawMergeEdge(graphics: GuiGraphicsExtractor, box: HudBox, edge: Edge) {
        val thickness = MERGE_EDGE_THICKNESS
        when (edge) {
            Edge.TOP -> graphics.fill(box.x, box.y - thickness, box.x + box.width, box.y, Common.UI.SELECTED_FRAME_COLOR)
            Edge.BOTTOM -> graphics.fill(box.x, box.y + box.height, box.x + box.width, box.y + box.height + thickness, Common.UI.SELECTED_FRAME_COLOR)
            Edge.LEFT -> graphics.fill(box.x - thickness, box.y, box.x, box.y + box.height, Common.UI.SELECTED_FRAME_COLOR)
            Edge.RIGHT -> graphics.fill(box.x + box.width, box.y, box.x + box.width + thickness, box.y + box.height, Common.UI.SELECTED_FRAME_COLOR)
        }
    }

    private fun positionDescription(placement: HudPlacement, rect: ScreenRect): String {
        val tie = placement.tie
        if (tie != null && layout.placementById(tie.target) != null) {
            return "Tied to ${displayNameOf(tie.target)}: ${signedText(tie.offsetX)}, ${signedText(tie.offsetY)}"
        }
        if (placement.positioning == Positioning.ABSOLUTE) return "x - ${rect.x}, y - ${rect.y}"

        val fromLeft = rect.x
        val fromRight = width - (rect.x + rect.width)
        val fromTop = rect.y
        val fromBottom = height - (rect.y + rect.height)
        val across = if (fromLeft <= fromRight) "$fromLeft px from left" else "$fromRight px from right"
        val down = if (fromTop <= fromBottom) "$fromTop px from top" else "$fromBottom px from bottom"
        return "$across, $down"
    }

    private fun signedText(n: Int): String = if (n >= 0) "+$n" else "$n"

    private fun transparencyPercent(alpha: Float): Int = ((1f - alpha) * 100).roundToInt()

    private fun drawPartInfo(graphics: GuiGraphicsExtractor, box: HudBox, part: HudPart) {
        val lines = buildList {
            add(part.element.name to Common.UI.ACCENT_COLOR)
            if (box.group != null) add("In a box with ${box.parts.size - 1} other${if (box.parts.size == 2) "" else "s"}" to Common.UI.TEXT_DIM_COLOR)
            add((if (box.placement.positioning == Positioning.RELATIVE && box.placement.tie == null) "Relative: " else "") + positionDescription(box.placement, ScreenRect(box.x, box.y, box.width, box.height)) to Common.UI.TEXT_COLOR)
            add("Scale ${"%.1f".format(part.state.scale)}" to Common.UI.TEXT_COLOR)
            add("Transparency ${transparencyPercent(box.alpha)}%" to Common.UI.TEXT_COLOR)
            add((if (isSizedByContent(box)) "Sizing dynamic" else "Sizing fixed") to Common.UI.TEXT_COLOR)
            idsTiedTo(box.id).takeIf { it.isNotEmpty() }?.let { add("Holds ${it.joinToString { id -> displayNameOf(id) }}" to Common.UI.TEXT_DIM_COLOR) }
        }
        drawInfoPanel(graphics, lines, box.x, box.y + box.height + Common.UI.SPACING, box.y)
    }

    private fun drawAnchorInfo(graphics: GuiGraphicsExtractor, anchor: AnchorState, x: Int, y: Int) {
        val lines = buildList {
            add(displayNameOf(anchor.id) to Common.UI.ACCENT_COLOR)
            add(positionDescription(anchor, ScreenRect(x, y, 0, 0)) to Common.UI.TEXT_COLOR)
            add("Transparency ${transparencyPercent(anchor.alpha)}%" to Common.UI.TEXT_COLOR)
            idsTiedTo(anchor.id).takeIf { it.isNotEmpty() }?.let { add("Holds ${it.joinToString { id -> displayNameOf(id) }}" to Common.UI.TEXT_DIM_COLOR) }
        }
        drawInfoPanel(graphics, lines, x + HudScene.ANCHOR_HALF_SIZE + 3, y + HudScene.ANCHOR_HALF_SIZE + Common.UI.SPACING, y - HudScene.ANCHOR_HALF_SIZE)
    }

    private fun drawInfoPanel(graphics: GuiGraphicsExtractor, lines: List<Pair<String, Int>>, atX: Int, below: Int, above: Int) {
        val padding = Common.UI.SPACING
        val panelWidth = lines.maxOf { font.width(it.first) } + padding * 2
        val panelHeight = lines.size * (font.lineHeight + 1) + padding * 2
        val left = atX.coerceIn(0, (width - panelWidth).coerceAtLeast(0))
        val top = if (below + panelHeight <= height) below else (above - Common.UI.SPACING - panelHeight).coerceAtLeast(0)

        graphics.drawPanel(left, top, left + panelWidth, top + panelHeight)
        lines.forEachIndexed { index, (text, color) ->
            graphics.modText(font, Component.literal(text), left + padding, top + padding + index * (font.lineHeight + 1), color)
        }
    }

    private fun drawHints(graphics: GuiGraphicsExtractor) {
        val sourceId = tieSourceId
        val targetId = tieTargetUnderCursor()
        val hint = when {
            sourceId != null && targetId != null -> "Click to tie ${displayNameOf(sourceId)} to ${displayNameOf(targetId)}"
            sourceId != null -> "Click what to tie ${displayNameOf(sourceId)} to, Escape to stop"
            selectedId != null -> "Arrows nudge ${displayNameOf(selectedId!!)} · shift arrows resize from the bottom right, with ctrl from the top left · wheel fades · shift wheel scales · middle click absolute or relative · shift middle click dynamic or fixed size · R resets"
            else -> "Click to select · drag to move · corners resize · drop on another to merge · wheel fades · shift wheel scales · right click for more · R resets"
        }
        val color = if (sourceId != null) Common.UI.SELECTED_FRAME_COLOR else Common.UI.TEXT_DIM_COLOR
        val hintLines = font.split(Component.literal(hint), width - Common.UI.SPACING_LARGE * 2)
        val bottom = height - Common.UI.SPACING_LARGE
        hintLines.forEachIndexed { index, line ->
            val lineY = bottom - (hintLines.size - index) * font.lineHeight
            graphics.text(font, line, (width - font.width(line)) / 2, lineY, color, true)
        }
    }

    override fun onMouseMoved(mouseX: Double, mouseY: Double) {
        this.mouseX = mouseX.toInt()
        this.mouseY = mouseY.toInt()
        overlaysMouseMoved(mouseX, mouseY)
    }

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (overlaysMouseClicked(event, doubled)) return true
        if (overlays.isNotEmpty()) {
            closeOverlays()
            return true
        }

        val x = event.x
        val y = event.y
        val anchor = scene.anchorUnderCursor(x, y)
        val box = boxOrCornerAt(x, y)

        if (situationPanelClicked(x.toInt(), y.toInt(), event.button())) return true

        tieSourceId?.let { sourceId ->
            if (event.button() == 0) tieTargetUnderCursor()?.let { tieTo(sourceId, it) }
            tieSourceId = null
            return true
        }

        if (event.button() == 1) {
            val part = scene.partUnderCursor(x, y)
            when {
                anchor != null -> openAnchorMenu(anchor.state, x.toInt(), y.toInt())
                box != null && part != null -> openPartMenu(part, box, x.toInt(), y.toInt())
                else -> openMenu(x.toInt(), y.toInt(), "Here", listOf(HudMenu.Entry("Add anchor") { addAnchor(x.toInt(), y.toInt()) }))
            }
            return true
        }
        if (event.button() == 2) {
            val id = anchor?.state?.id ?: box?.id ?: return false
            when {
                shiftDown() -> box?.let { toggleSizing(it) }
                layout.placementById(id)?.tie != null -> untie(id)
                else -> togglePositioning(id)
            }
            HudLayoutFile.save()
            return true
        }
        if (event.button() != 0) return false

        if (anchor != null) {
            selectedId = anchor.state.id
            drag = Drag.Move(anchor.state.id, x.toInt() - anchor.x, y.toInt() - anchor.y)
            return true
        }
        if (box == null) {
            selectedId = null
            return false
        }
        selectedId = box.id

        cornerAt(box, x, y)?.let { corner ->
            drag = Drag.Resize(
                box.id,
                corner.isLeft,
                corner.isTop,
                if (corner.isLeft) box.x + box.width else box.x,
                if (corner.isTop) box.y + box.height else box.y
            )
            return true
        }
        box.dividerAt(x, y)?.let { partIndex ->
            val partBefore = box.parts[partIndex]
            val partAfter = box.parts[partIndex + 1]
            drag = if (box.stacking == Stacking.VERTICAL) Drag.Divider(box.id, partIndex, y.toInt(), partBefore.height, partAfter.height)
            else Drag.Divider(box.id, partIndex, x.toInt(), partBefore.width, partAfter.width)
            return true
        }
        drag = Drag.Move(box.id, x.toInt() - box.x, y.toInt() - box.y)
        return true
    }

    override fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val current = drag ?: return false
        when (current) {
            is Drag.Move -> {
                moveBoxTo(current.id, event.x.toInt() - current.offsetX, event.y.toInt() - current.offsetY)
                mergeTarget = mergeTargetFor(current.id, event.x, event.y)
            }
            is Drag.Resize -> scene.boxOf(current.id)?.let { resizeBoxTo(it, current, event.x.toInt(), event.y.toInt()) }
            is Drag.Divider -> scene.boxOf(current.id)?.let { dragDivider(it, current, if (it.stacking == Stacking.VERTICAL) event.y.toInt() else event.x.toInt()) }
        }
        return true
    }

    private fun mergeTargetFor(draggedId: String, x: Double, y: Double): Pair<HudBox, Edge>? {
        val dragged = scene.boxOf(draggedId) ?: return null
        if (dragged.group != null) return null
        val target = scene.boxes.lastOrNull { it.id != draggedId && it.contains(x, y) } ?: return null
        val distances = mapOf(
            Edge.TOP to y - target.y,
            Edge.BOTTOM to target.y + target.height - y,
            Edge.LEFT to x - target.x,
            Edge.RIGHT to target.x + target.width - x
        )
        return target to distances.minByOrNull { it.value }!!.key
    }

    override fun onMouseReleased(event: MouseButtonEvent): Boolean {
        val current = drag ?: return false
        drag = null
        mergeTarget?.let { (box, edge) -> if (current is Drag.Move) mergeIntoBox(current.id, box, edge) }
        mergeTarget = null
        HudLayoutFile.save()
        return true
    }

    private fun stepAlpha(placement: HudPlacement, step: Int) {
        placement.alpha = ((placement.alpha + step * ALPHA_STEP) * 100).roundToInt().coerceIn(0, 100) / 100f
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (overlaysMouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true
        if (isOverPanelTab(mouseX.toInt(), mouseY.toInt()) || isOverSituationPanel(mouseX.toInt(), mouseY.toInt())) return true
        val step = if (scrollY > 0) 1 else -1
        scene.anchorUnderCursor(mouseX, mouseY)?.let { anchor ->
            stepAlpha(anchor.state, step)
            HudLayoutFile.save()
            return true
        }
        val box = scene.boxUnderCursor(mouseX, mouseY) ?: return false

        if (shiftDown()) {
            val part = scene.partUnderCursor(mouseX, mouseY) ?: return true
            part.state.scale = (part.state.scale + step * SCALE_STEP).coerceIn(MIN_SCALE, MAX_SCALE).let { (it * 10).roundToInt() / 10f }
        } else {
            stepAlpha(box.placement, step)
        }
        rebuildScene()
        HudLayoutFile.save()
        return true
    }

    override fun onKeyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (tieSourceId != null) {
                tieSourceId = null
                return true
            }
            if (overlays.isNotEmpty()) {
                closeOverlays()
                return true
            }
        }
        if (overlays.isEmpty() && selectedId != null) {
            val step = when (event.key()) {
                GLFW.GLFW_KEY_LEFT -> -1 to 0
                GLFW.GLFW_KEY_RIGHT -> 1 to 0
                GLFW.GLFW_KEY_UP -> 0 to -1
                GLFW.GLFW_KEY_DOWN -> 0 to 1
                else -> null
            }
            if (step != null) {
                if (shiftDown()) resizeSelectedByKey(step.first, step.second, topLeft = controlDown()) else nudgeSelected(step.first, step.second)
                HudLayoutFile.save()
                return true
            }
        }
        if (event.key() == GLFW.GLFW_KEY_R && overlays.isEmpty()) {
            hoveredPart()?.let {
                resetElement(it.element.id)
                HudLayoutFile.save()
                return true
            }
        }
        return super.onKeyPressed(event)
    }

    override fun removed() {
        HudLayoutFile.save()
    }

    private fun controlDown(): Boolean {
        val window = Minecraft.getInstance().window
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
    }

    private fun shiftDown(): Boolean {
        val window = Minecraft.getInstance().window
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
    }

    private companion object {
        val CORNERS: List<Corner> = listOf(Corner(true, true), Corner(true, false), Corner(false, true), Corner(false, false))

        const val TAB_WIDTH: Int = 30
        const val TAB_HEIGHT: Int = 12
        const val PANEL_WIDTH: Int = 120
        const val PANEL_ROW_HEIGHT: Int = 13
        const val PANEL_PADDING: Int = 4

        const val PANEL_SWITCH_WIDTH: Int = 16
        const val PANEL_SWITCH_HEIGHT: Int = 9

        const val HANDLE_HALF_SIZE: Int = 2
        const val HANDLE_GRAB_DISTANCE: Int = 5
        const val ANCHOR_CROSS_ARM: Int = 4

        const val MERGE_EDGE_THICKNESS: Int = 3

        const val GRIP_REACH: Int = 3
        const val GRIP_GAP: Int = 3
        const val ALPHA_STEP: Float = 0.05f
        const val SCALE_STEP: Float = 0.1f
        const val MIN_SCALE: Float = 0.5f
        const val MAX_SCALE: Float = 3f
    }
}
