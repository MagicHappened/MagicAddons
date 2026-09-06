package org.magic.magicaddons.ui.screens

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
import org.magic.magicaddons.ui.hud.HudLayoutStore
import org.magic.magicaddons.ui.hud.HudPainter
import org.magic.magicaddons.ui.hud.HudPart
import org.magic.magicaddons.ui.hud.HudScene
import org.magic.magicaddons.ui.hud.Placed
import org.magic.magicaddons.ui.hud.Positioning
import org.magic.magicaddons.ui.hud.Stacking
import org.magic.magicaddons.ui.hud.Tie
import org.magic.magicaddons.ui.widgets.hud.HudMenu
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawButtonPanel
import org.magic.magicaddons.util.ScreenUtil.drawLine
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.setScreen
import org.magic.magicaddons.util.compat.McCompat
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Every hud element on screen with its sample content, to be dragged, resized at the corners,
 * faded with the wheel, scaled with shift and the wheel, merged by dropping one on another,
 * tied to each other or to anchors through the right click menu, and reset with R.
 */
class HudEditorScreen : MagicScreen(Component.literal("HUD Editor"), "the hud editor"), OverlayContext {

    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    private val layout get() = HudLayoutStore.layout

    private var scene: HudScene = HudScene(emptyList(), emptyList())

    private enum class Edge { TOP, BOTTOM, LEFT, RIGHT }

    private sealed class Drag(val id: String) {
        class Move(id: String, val offsetX: Int, val offsetY: Int) : Drag(id)
        class Resize(id: String, val leftSide: Boolean, val topSide: Boolean, val fixedX: Int, val fixedY: Int) : Drag(id)

        /** The line between parts [index] and the next of a box, with where it started and how big each part was. */
        class Divider(id: String, val index: Int, val start: Int, val sizeA: Int, val sizeB: Int) : Drag(id)
    }

    /** The box or anchor last clicked, framed and moved by the arrow keys until something else is. */
    private var selectedId: String? = null

    private var drag: Drag? = null

    /** The box a dragged element would merge into, and the edge it would join at. */
    private var mergeTarget: Pair<HudBox, Edge>? = null

    /** The thing waiting for a click on what to tie it to. */
    private var picking: String? = null

    private var mouseX = 0
    private var mouseY = 0

    /** The place being laid out; only elements that can show there are drawn. */
    private var situation: HudSituation = HudSituation.current()

    /** Whether the tab at the top centre has its panel open. */
    private var panelOpen = false

    /** One switch per element in the panel, kept so the knob can slide when it is flipped. */
    private val panelSwitches = mutableMapOf<String, SwitchWidget>()

    private fun shownHere(element: HudElement): Boolean = element.showsIn(situation) && !layout.isHidden(situation, element.id)

    private fun rebuild() {
        scene = HudScene.build(layout, width, height, sample = true, include = ::shownHere)
    }

    override fun onInit() {
        super.onInit()
        closeOverlays()
        rebuild()
    }

    override fun isPauseScreen(): Boolean = false

    // ------------------------------------------------------------------ what is under the mouse

    /** The box under the mouse, or the one whose corner dot the mouse is on. */
    private fun boxNear(x: Double, y: Double): HudBox? =
        scene.boxAt(x, y) ?: scene.boxes.lastOrNull { cornerAt(it, x, y) != null }

    private fun hoveredBox(): HudBox? = boxNear(mouseX.toDouble(), mouseY.toDouble())
    private fun hoveredPart(): HudPart? = scene.partAt(mouseX.toDouble(), mouseY.toDouble())

    /** The box that drags as one: a lone element, or the group a part belongs to. */
    private fun draggedBox(): HudBox? = drag?.let { scene.boxOf(it.id) }

    /** Which corner dot the mouse is on, as whether it is a left one and whether a top one. */
    private fun cornerAt(box: HudBox, x: Double, y: Double): Pair<Boolean, Boolean>? {
        return listOf(true to true, true to false, false to true, false to false).firstOrNull { (left, top) ->
            val cx = if (left) box.x else box.x + box.width
            val cy = if (top) box.y else box.y + box.height
            abs(x - cx) <= HANDLE_REACH && abs(y - cy) <= HANDLE_REACH
        }
    }

    private fun nameOf(id: String): String =
        HudElements.byId(id)?.name
            ?: layout.group(id)?.let { group -> group.members.mapNotNull { HudElements.byId(it)?.name }.joinToString(" + ") }
            ?: layout.anchor(id)?.let { "Anchor ${it.id.substringAfter('-')}" }
            ?: id

    // ------------------------------------------------------------------ edits

    private fun moveTo(id: String, left: Int, top: Int) {
        val placed = layout.placed(id) ?: return
        val rect = scene.rectOf(id) ?: return
        val tie = placed.tie
        val target = tie?.let { scene.rectOf(it.target) }
        if (tie != null && target != null) {
            tie.dx = left - target[0]
            tie.dy = top - target[1]
        } else {
            placed.placeAt(left, top, rect[2], rect[3], width, height)
        }
        rebuild()
    }

    private fun resizeTo(box: HudBox, drag: Drag.Resize, mouseX: Int, mouseY: Int) {
        val wantedWidth = (if (drag.leftSide) drag.fixedX - mouseX else mouseX - box.x).coerceIn(box.minWidth, width)
        val wantedHeight = (if (drag.topSide) drag.fixedY - mouseY else mouseY - box.y).coerceIn(box.minHeight, height)
        val placed = box.placed
        freeze(box)
        when (placed) {
            is ElementState -> {
                placed.width = wantedWidth
                placed.height = wantedHeight
            }
            is GroupState -> when (placed.stacking) {
                // the shared side is the group's; the stacked side is shared out in proportion
                Stacking.VERTICAL -> {
                    placed.width = wantedWidth
                    share(box.parts, wantedHeight - (box.parts.size - 1) * HudScene.DIVIDER, { it.height }, { it.minHeight }) { part, size -> part.state.height = size }
                }
                Stacking.HORIZONTAL -> {
                    placed.height = wantedHeight
                    share(box.parts, wantedWidth - (box.parts.size - 1) * HudScene.DIVIDER, { it.width }, { it.minWidth }) { part, size -> part.state.width = size }
                }
            }
            else -> return
        }
        val left = if (drag.leftSide) drag.fixedX - wantedWidth else box.x
        val top = if (drag.topSide) drag.fixedY - wantedHeight else box.y
        val tie = placed.tie
        val target = tie?.let { scene.rectOf(it.target) }
        if (tie != null && target != null) {
            tie.dx = left - target[0]
            tie.dy = top - target[1]
        } else {
            placed.placeAt(left, top, wantedWidth, wantedHeight, width, height)
        }
        rebuild()
    }

    /** Gives [parts] [total] between them in the proportion they have now, none under its minimum. */
    private fun share(parts: List<HudPart>, total: Int, size: (HudPart) -> Int, minimum: (HudPart) -> Int, set: (HudPart, Int) -> Unit) {
        val current = parts.sumOf { size(it) }.coerceAtLeast(1)
        var left = total
        parts.forEachIndexed { index, part ->
            val wanted = if (index == parts.size - 1) left else (size(part).toFloat() / current * total).roundToInt()
            val given = wanted.coerceAtLeast(minimum(part))
            set(part, given)
            left -= given
        }
    }

    /** Holds a box at the size it is drawn, so it no longer follows its content. */
    private fun freeze(box: HudBox) {
        when (val placed = box.placed) {
            is ElementState -> if (placed.dynamic) {
                placed.dynamic = false
                placed.width = box.width
                placed.height = box.height
            }
            is GroupState -> if (placed.dynamic) {
                placed.dynamic = false
                placed.width = box.width
                placed.height = box.height
                box.parts.forEach {
                    it.state.width = it.width
                    it.state.height = it.height
                }
            }
            else -> {}
        }
    }

    private fun isDynamic(box: HudBox): Boolean = when (val placed = box.placed) {
        is ElementState -> placed.dynamic
        is GroupState -> placed.dynamic
        else -> true
    }

    /** Switches a box between following its content and holding the size it has now. */
    private fun toggleDynamic(box: HudBox) {
        if (isDynamic(box)) {
            freeze(box)
        } else {
            when (val placed = box.placed) {
                is ElementState -> placed.dynamic = true
                is GroupState -> placed.dynamic = true
                else -> {}
            }
        }
        rebuild()
    }

    /** Slides the line between two parts, one growing by what the other gives up. */
    private fun dragDivider(box: HudBox, drag: Drag.Divider, mouse: Int) {
        freeze(box)
        val a = box.parts[drag.index]
        val b = box.parts[drag.index + 1]
        val vertical = box.stacking == Stacking.VERTICAL
        val minA = if (vertical) a.minHeight else a.minWidth
        val minB = if (vertical) b.minHeight else b.minWidth
        val moved = (mouse - drag.start).coerceIn(minA - drag.sizeA, drag.sizeB - minB)
        if (vertical) {
            a.state.height = drag.sizeA + moved
            b.state.height = drag.sizeB - moved
        } else {
            a.state.width = drag.sizeA + moved
            b.state.width = drag.sizeB - moved
        }
        rebuild()
    }

    /** Moves the selected box or anchor by a pixel in one direction, held keys repeating it. */
    private fun nudge(dx: Int, dy: Int) {
        val id = selectedId ?: return
        val rect = scene.rectOf(id) ?: return
        moveTo(id, rect[0] + dx, rect[1] + dy)
    }

    /**
     * Resizes the selected box by a pixel: the bottom right corner moves the way the arrow points,
     * or with control held the top left corner does, the other corner staying put.
     */
    private fun resizeByKey(dx: Int, dy: Int, topLeft: Boolean) {
        val box = selectedId?.let { scene.boxOf(it) } ?: return
        val drag = if (topLeft) {
            Drag.Resize(box.id, leftSide = true, topSide = true, fixedX = box.x + box.width, fixedY = box.y + box.height)
        } else {
            Drag.Resize(box.id, leftSide = false, topSide = false, fixedX = box.x, fixedY = box.y)
        }
        val cornerX = if (topLeft) box.x else box.x + box.width
        val cornerY = if (topLeft) box.y else box.y + box.height
        resizeTo(box, drag, cornerX + dx, cornerY + dy)
    }

    /** Puts [elementId] into [target]'s box at [edge]: a new group of the two, or one more member of an existing group. */
    private fun merge(elementId: String, target: HudBox, edge: Edge) {
        val state = layout.elements[elementId] ?: return
        val group = target.group
        val atStart = edge == Edge.TOP || edge == Edge.LEFT
        if (group != null) {
            if (atStart) group.members.add(0, elementId) else group.members.add(elementId)
        } else {
            val stacking = if (edge == Edge.TOP || edge == Edge.BOTTOM) Stacking.VERTICAL else Stacking.HORIZONTAL
            val members = if (atStart) listOf(elementId, target.id) else listOf(target.id, elementId)
            val made = layout.newGroup(members, stacking)
            val from = target.placed
            made.positioning = from.positioning
            made.x = from.x
            made.y = from.y
            made.fx = from.fx
            made.fy = from.fy
            made.tie = from.tie
            (from as? ElementState)?.let {
                made.alpha = it.alpha
                made.width = it.width
                it.tie = null
            }
            // whatever hung off the element now hangs off the box it is in
            (layout.elements.values + layout.groups + layout.anchors).forEach { placed ->
                if (placed.tie?.target == target.id) placed.tie?.target = made.id
            }
        }
        state.tie = null
        rebuild()
    }

    /** Takes [elementId] out of its group, leaving it where it was drawn. */
    private fun split(elementId: String) {
        val group = layout.groupOf(elementId) ?: return
        val part = scene.boxOf(group.id)?.parts?.firstOrNull { it.element.id == elementId }
        val state = layout.elements[elementId] ?: return

        group.members.remove(elementId)
        if (part != null) state.placeAt(part.x, part.y, part.width, part.height, width, height)
        state.tie = null

        if (group.members.size <= 1) {
            val rects = scene.rects()
            group.members.firstOrNull()?.let { lastId ->
                val last = layout.elements[lastId] ?: return@let
                val lastPart = scene.boxOf(group.id)?.parts?.firstOrNull { it.element.id == lastId }
                last.positioning = group.positioning
                last.alpha = group.alpha
                if (lastPart != null) {
                    last.placeAt(lastPart.x, lastPart.y, lastPart.width, lastPart.height, width, height)
                    last.tie = group.tie?.let { Tie(it.target, it.dx + lastPart.x - group.x, it.dy + lastPart.y - group.y) }
                } else {
                    last.x = group.x
                    last.y = group.y
                    last.fx = group.fx
                    last.fy = group.fy
                    last.tie = group.tie
                }
                (layout.elements.values + layout.groups + layout.anchors).forEach { placed ->
                    if (placed.tie?.target == group.id) placed.tie?.target = lastId
                }
            }
            layout.untieFrom(group.id, rects, width, height)
            layout.groups.remove(group)
        }
        rebuild()
    }

    /** Back to how the element started: alone, untied, at its default place, size, scale and fade. */
    private fun reset(elementId: String) {
        val element = HudElements.byId(elementId) ?: return
        split(elementId)
        layout.untieFrom(elementId, scene.rects(), width, height)
        layout.elements[elementId] = layout.defaults(element)
        rebuild()
    }

    private fun untie(id: String) {
        val placed = layout.placed(id) ?: return
        val rect = scene.rectOf(id) ?: return
        placed.placeAt(rect[0], rect[1], rect[2], rect[3], width, height)
        placed.tie = null
        rebuild()
    }

    private fun togglePositioning(id: String) {
        val placed = layout.placed(id) ?: return
        val rect = scene.rectOf(id) ?: return
        placed.placeAt(rect[0], rect[1], rect[2], rect[3], width, height)
        placed.positioning = if (placed.positioning == Positioning.ABSOLUTE) Positioning.RELATIVE else Positioning.ABSOLUTE
        rebuild()
    }

    private fun tie(id: String, targetId: String) {
        if (id == targetId || layout.wouldLoop(id, targetId)) return
        val placed = layout.placed(id) ?: return
        val rect = scene.rectOf(id) ?: return
        val target = scene.rectOf(targetId) ?: return
        placed.tie = Tie(targetId, rect[0] - target[0], rect[1] - target[1])
        rebuild()
    }

    private fun removeAnchor(anchor: AnchorState) {
        layout.untieFrom(anchor.id, scene.rects(), width, height)
        layout.anchors.remove(anchor)
        rebuild()
    }

    private fun addAnchor(x: Int, y: Int) {
        layout.newAnchor().placeAt(x, y, 0, 0, width, height)
        rebuild()
    }

    /** Takes off every tie that hangs from [id]. */
    private fun untieAllFrom(id: String) {
        layout.untieFrom(id, scene.rects(), width, height)
        rebuild()
    }

    private fun openConfig(part: HudPart) {
        val target = part.element.configTarget ?: return
        HudLayoutStore.save()
        setScreen(ConfigScreen(this).apply { showSetting(target.feature, target.path) })
    }

    // ------------------------------------------------------------------ the menu

    private fun openMenu(x: Int, y: Int, title: String, entries: List<HudMenu.Entry>) {
        addContext(HudMenu(x, y, title, entries, this).also { it.init() })
    }

    private fun placementEntries(id: String): List<HudMenu.Entry> {
        val placed = layout.placed(id) ?: return emptyList()
        return buildList {
            if (placed.tie != null) {
                add(HudMenu.Entry("Untie from ${nameOf(placed.tie!!.target)}") { untie(id) })
            } else {
                val other = if (placed.positioning == Positioning.ABSOLUTE) "Relative" else "Absolute"
                add(HudMenu.Entry("Position: $other") { togglePositioning(id) })
            }
            add(HudMenu.Entry("Tie to…") { picking = id })
        }
    }

    private fun menuFor(part: HudPart, box: HudBox, x: Int, y: Int) {
        val entries = buildList {
            if (part.element.configTarget != null) add(HudMenu.Entry("Open config") { openConfig(part) })
            addAll(placementEntries(box.id))
            add(HudMenu.Entry(if (isDynamic(box)) "Sizing: fixed" else "Sizing: dynamic") { toggleDynamic(box) })
            if (box.group != null) add(HudMenu.Entry("Split off") { split(part.element.id) })
            add(HudMenu.Entry("Reset") { reset(part.element.id) })
            if (tiedTo(box.id).isNotEmpty()) add(HudMenu.Entry("Untie all from this") { untieAllFrom(box.id) })
        }
        openMenu(x, y, part.element.name, entries)
    }

    private fun menuForAnchor(anchor: AnchorState, x: Int, y: Int) {
        val entries = buildList {
            addAll(placementEntries(anchor.id))
            add(HudMenu.Entry("Remove") { removeAnchor(anchor) })
            if (tiedTo(anchor.id).isNotEmpty()) add(HudMenu.Entry("Untie all from this") { untieAllFrom(anchor.id) })
        }
        openMenu(x, y, nameOf(anchor.id), entries)
    }

    // ------------------------------------------------------------------ drawing

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, Common.UI.SCREEN_DIM_COLOR)
    }

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.onRender(graphics, mouseX, mouseY, delta)
        this.mouseX = mouseX
        this.mouseY = mouseY
        rebuild()

        scene.draw(graphics)

        val hovered = if (overlays.isEmpty()) hoveredBox() else null
        val shown = draggedBox() ?: hovered
        val selected = selectedId?.let { scene.boxOf(it) }
        selected?.let { drawHandles(graphics, it) }
        shown?.takeIf { it !== selected }?.let { drawHandles(graphics, it) }
        drawAnchors(graphics)
        drawTies(graphics, listOfNotNull(shown?.id, selectedId).distinct())
        (draggedBox() ?: hovered)?.let { drawDividerHandle(graphics, it) }
        drawPicking(graphics)
        mergeTarget?.let { (box, edge) -> drawMergeEdge(graphics, box, edge) }

        if (overlays.isEmpty()) {
            val part = hoveredPart()
            val anchor = scene.anchorAt(mouseX.toDouble(), mouseY.toDouble())
            when {
                shown != null && part != null -> drawReadout(graphics, shown, part)
                anchor != null -> drawAnchorReadout(graphics, anchor.state, anchor.x, anchor.y)
            }
        }
        drawHints(graphics)
        drawPanel(graphics)
        renderOverlays(graphics, mouseX, mouseY, delta)
    }

    // ------------------------------------------------------------------ the tab at the top centre

    /** One row of the panel: a situation to lay out for, or an element to show or hide there. */
    private class PanelRow(val top: Int, val situation: HudSituation? = null, val element: HudElement? = null)

    /** The panel's rows from the top: the situations under the Show heading, a line, then the elements. */
    private fun panelRows(): List<PanelRow> {
        var rowY = panelTop() + PANEL_PAD + PANEL_ROW
        return buildList {
            HudSituation.offered().forEach { candidate ->
                add(PanelRow(rowY, situation = candidate))
                rowY += PANEL_ROW
            }
            rowY += PANEL_PAD * 2 + 1
            HudElements.all.filter { it.showsIn(situation) }.forEach { element ->
                add(PanelRow(rowY, element = element))
                rowY += PANEL_ROW
            }
        }
    }

    private fun panelHeight(): Int {
        val rows = panelRows()
        return PANEL_PAD * 2 + PANEL_ROW * (1 + rows.size) + PANEL_PAD * 2 + 1
    }

    private fun panelLeft(): Int = (width - PANEL_WIDTH) / 2
    private fun panelRight(): Int = panelLeft() + PANEL_WIDTH
    private fun panelTop(): Int = TAB_HEIGHT
    private fun tabLeft(): Int = (width - TAB_WIDTH) / 2

    private fun overTab(x: Int, y: Int): Boolean = y < TAB_HEIGHT && x in tabLeft() until tabLeft() + TAB_WIDTH

    private fun overPanel(x: Int, y: Int): Boolean =
        panelOpen && x in panelLeft() until panelRight() && y in panelTop() until panelTop() + panelHeight()

    /** The tab at the top centre, and the panel of situations and elements it drops down. */
    private fun drawPanel(graphics: GuiGraphicsExtractor) {
        val tabLeft = tabLeft()
        val overTab = overTab(mouseX, mouseY)
        graphics.drawButtonPanel(tabLeft, 0, tabLeft + TAB_WIDTH, TAB_HEIGHT, overTab, pressed = panelOpen)
        val midX = tabLeft + TAB_WIDTH / 2
        // a small chevron in the middle of the tab, whatever the tab's height
        val midY = TAB_HEIGHT / 2
        val tipY = if (panelOpen) midY - 3 else midY + 3
        val baseY = if (panelOpen) midY + 3 else midY - 3
        graphics.drawLine(midX - 3, baseY, midX, tipY, 1, Common.UI.TEXT_COLOR)
        graphics.drawLine(midX, tipY, midX + 3, baseY, 1, Common.UI.TEXT_COLOR)

        if (!panelOpen) return
        val left = panelLeft()
        val panelTop = panelTop()
        graphics.drawPanel(left, panelTop, panelRight(), panelTop + panelHeight())

        val rows = panelRows()
        graphics.text(font, Component.literal("Show"), left + PANEL_PAD, panelTop + PANEL_PAD + (PANEL_ROW - font.lineHeight) / 2, Common.UI.TEXT_DIM_COLOR, false)

        // the line between the situations and the elements
        rows.firstOrNull { it.element != null }?.let { first ->
            val lineY = first.top - PANEL_PAD - 1
            graphics.fill(left + PANEL_PAD, lineY, panelRight() - PANEL_PAD, lineY + 1, Common.UI.THIN_DIVIDER_COLOR)
        }

        rows.forEach { row ->
            val rowY = row.top
            val over = mouseX in left until panelRight() && mouseY in rowY until rowY + PANEL_ROW
            val textY = rowY + (PANEL_ROW - font.lineHeight) / 2

            row.situation?.let { candidate ->
                if (candidate == situation) {
                    graphics.fill(left + Common.UI.BORDER_SIZE, rowY, panelRight() - Common.UI.BORDER_SIZE, rowY + PANEL_ROW, Common.UI.PRESSED_SHADE)
                    graphics.fill(left + Common.UI.BORDER_SIZE, rowY, left + Common.UI.BORDER_SIZE + 2, rowY + PANEL_ROW, Common.UI.SELECTED_FRAME_COLOR)
                } else if (over) {
                    graphics.fill(left + Common.UI.BORDER_SIZE, rowY, panelRight() - Common.UI.BORDER_SIZE, rowY + PANEL_ROW, Common.UI.HOVER_WASH)
                }
                graphics.text(font, Component.literal(candidate.label), left + PANEL_PAD + 3, textY, if (candidate == situation) Common.UI.TEXT_COLOR else Common.UI.TEXT_DIM_COLOR, false)
            }

            row.element?.let { element ->
                val shown = !layout.isHidden(situation, element.id)
                if (over) graphics.fill(left + Common.UI.BORDER_SIZE, rowY, panelRight() - Common.UI.BORDER_SIZE, rowY + PANEL_ROW, Common.UI.HOVER_WASH)
                graphics.text(font, Component.literal(element.name), left + PANEL_PAD + 3, textY, if (shown) Common.UI.TEXT_COLOR else Common.UI.DISABLED_TEXT_COLOR, false)
                val switch = panelSwitches.getOrPut(element.id) { SwitchWidget(shown, PANEL_SWITCH_WIDTH, PANEL_SWITCH_HEIGHT) }
                switch.set(shown)
                switch.x = panelRight() - PANEL_PAD - switch.width
                switch.y = rowY + (PANEL_ROW - switch.height) / 2
                switch.render(graphics)
            }
        }
    }

    /** A click on the tab or its panel, taken before anything under them. */
    private fun panelClicked(x: Int, y: Int, button: Int): Boolean {
        if (overTab(x, y)) {
            if (button == 0) panelOpen = !panelOpen
            return true
        }
        if (!panelOpen) return false
        if (!overPanel(x, y)) {
            panelOpen = false
            return true
        }
        if (button != 0) return true

        val row = panelRows().firstOrNull { y in it.top until it.top + PANEL_ROW } ?: return true
        row.situation?.let { candidate ->
            situation = candidate
            selectedId = null
            rebuild()
        }
        row.element?.let { element ->
            layout.setHidden(situation, element.id, !layout.isHidden(situation, element.id))
            if (selectedId == element.id) selectedId = null
            rebuild()
            HudLayoutStore.save()
        }
        return true
    }

    private fun drawHandles(graphics: GuiGraphicsExtractor, box: HudBox) {
        graphics.drawBorder(box.x, box.y, box.x + box.width, box.y + box.height, 1, Common.UI.SELECTED_FRAME_COLOR)
        listOf(box.x to box.y, box.x + box.width to box.y, box.x to box.y + box.height, box.x + box.width to box.y + box.height).forEach { (cx, cy) ->
            graphics.fill(cx - HANDLE, cy - HANDLE, cx + HANDLE + 1, cy + HANDLE + 1, Common.UI.SELECTED_FRAME_COLOR)
        }
    }

    /** Each anchor as a small box with a cross in it, framed amber when it is hovered or selected. */
    private fun drawAnchors(graphics: GuiGraphicsExtractor) {
        val half = HudScene.ANCHOR_HALF
        scene.anchors.forEach { anchor ->
            val over = scene.anchorAt(mouseX.toDouble(), mouseY.toDouble()) === anchor || anchor.state.id == selectedId
            HudPainter.drawPanel(graphics, anchor.x - half, anchor.y - half, half * 2 + 1, half * 2 + 1, anchor.state.alpha)
            if (over) graphics.drawBorder(anchor.x - half, anchor.y - half, anchor.x + half + 1, anchor.y + half + 1, 1, Common.UI.SELECTED_FRAME_COLOR)
            val color = if (over) Common.UI.TEXT_COLOR else Common.UI.SELECTED_FRAME_COLOR
            graphics.fill(anchor.x - ANCHOR_ARM, anchor.y, anchor.x + ANCHOR_ARM + 1, anchor.y + 1, color)
            graphics.fill(anchor.x, anchor.y - ANCHOR_ARM, anchor.x + 1, anchor.y + ANCHOR_ARM + 1, color)
            graphics.text(font, Component.literal(nameOf(anchor.state.id)), anchor.x + half + 3, anchor.y - font.lineHeight / 2, Common.UI.TEXT_DIM_COLOR, true)
        }
    }

    /** The line between two parts lit with arrows either way while the mouse is on it. */
    private fun drawDividerHandle(graphics: GuiGraphicsExtractor, box: HudBox) {
        val index = (drag as? Drag.Divider)?.index ?: box.dividerAt(mouseX.toDouble(), mouseY.toDouble()) ?: return
        val next = box.parts.getOrNull(index + 1) ?: return
        val color = Common.UI.SELECTED_FRAME_COLOR
        if (box.stacking == Stacking.HORIZONTAL) {
            val lineX = next.x - HudScene.DIVIDER
            graphics.fill(lineX - 1, box.y, lineX + 2, box.y + box.height, color)
            val cy = box.centerY
            for (i in 0..2) {
                graphics.fill(lineX - 4 - i, cy - i, lineX - 3 - i, cy + i + 1, color)
                graphics.fill(lineX + 4 + i, cy - i, lineX + 5 + i, cy + i + 1, color)
            }
        } else {
            val lineY = next.y - HudScene.DIVIDER
            graphics.fill(box.x, lineY - 1, box.x + box.width, lineY + 2, color)
            val cx = box.centerX
            for (i in 0..2) {
                graphics.fill(cx - i, lineY - 4 - i, cx + i + 1, lineY - 3 - i, color)
                graphics.fill(cx - i, lineY + 4 + i, cx + i + 1, lineY + 5 + i, color)
            }
        }
    }

    /** The middle of a box, or an anchor's point. */
    private fun centerOf(id: String): IntArray? =
        scene.boxOf(id)?.let { intArrayOf(it.centerX, it.centerY) }
            ?: scene.anchors.firstOrNull { it.state.id == id }?.let { intArrayOf(it.x, it.y) }

    /** Everything tied to [id]. */
    private fun tiedTo(id: String): List<String> = buildList {
        layout.elements.forEach { (key, state) -> if (state.tie?.target == id) add(key) }
        layout.groups.forEach { if (it.tie?.target == id) add(it.id) }
        layout.anchors.forEach { if (it.tie?.target == id) add(it.id) }
    }

    /** A line from the middle of a tied thing to the middle of what it hangs from, with a dot at that end. */
    private fun drawTies(graphics: GuiGraphicsExtractor, shownIds: List<String>) {
        val both = shownIds + shownIds.flatMap { tiedTo(it) }
        (both + scene.anchors.map { it.state.id }).distinct().forEach { id ->
            val tie = layout.placed(id)?.tie ?: return@forEach
            val from = centerOf(id) ?: return@forEach
            val to = centerOf(tie.target) ?: return@forEach
            graphics.drawLine(from[0], from[1], to[0], to[1], 1, Common.UI.SELECTED_FRAME_COLOR)
            graphics.fill(to[0] - 2, to[1] - 2, to[0] + 3, to[1] + 3, Common.UI.SELECTED_FRAME_COLOR)
        }
    }

    /** What a tie being made would join: the thing under the mouse framed, and a line from the source to the mouse. */
    private fun drawPicking(graphics: GuiGraphicsExtractor) {
        val source = picking ?: return
        val from = centerOf(source) ?: return
        graphics.drawLine(from[0], from[1], mouseX, mouseY, 1, Common.UI.SELECTED_FRAME_COLOR)
        val candidate = pickCandidate() ?: return
        scene.boxOf(candidate)?.let { box ->
            graphics.drawBorder(box.x - 2, box.y - 2, box.x + box.width + 2, box.y + box.height + 2, 2, Common.UI.SELECTED_FRAME_COLOR)
        }
        scene.anchors.firstOrNull { it.state.id == candidate }?.let { anchor ->
            graphics.drawBorder(anchor.x - ANCHOR_ARM - 2, anchor.y - ANCHOR_ARM - 2, anchor.x + ANCHOR_ARM + 3, anchor.y + ANCHOR_ARM + 3, 1, Common.UI.SELECTED_FRAME_COLOR)
        }
    }

    /** What the mouse is on that the thing being tied could hang from. */
    private fun pickCandidate(): String? {
        val source = picking ?: return null
        val id = scene.anchorAt(mouseX.toDouble(), mouseY.toDouble())?.state?.id ?: hoveredBox()?.id ?: return null
        if (id == source || layout.wouldLoop(source, id)) return null
        return id
    }

    private fun drawMergeEdge(graphics: GuiGraphicsExtractor, box: HudBox, edge: Edge) {
        val t = 3
        when (edge) {
            Edge.TOP -> graphics.fill(box.x, box.y - t, box.x + box.width, box.y, Common.UI.SELECTED_FRAME_COLOR)
            Edge.BOTTOM -> graphics.fill(box.x, box.y + box.height, box.x + box.width, box.y + box.height + t, Common.UI.SELECTED_FRAME_COLOR)
            Edge.LEFT -> graphics.fill(box.x - t, box.y, box.x, box.y + box.height, Common.UI.SELECTED_FRAME_COLOR)
            Edge.RIGHT -> graphics.fill(box.x + box.width, box.y, box.x + box.width + t, box.y + box.height, Common.UI.SELECTED_FRAME_COLOR)
        }
    }

    /** How a placed thing's position reads: plain x and y, the nearest edges, or what it is tied to. */
    private fun describe(placed: Placed, rect: IntArray): String {
        val tie = placed.tie
        if (tie != null && layout.placed(tie.target) != null) {
            return "Tied to ${nameOf(tie.target)}: ${signed(tie.dx)}, ${signed(tie.dy)}"
        }
        if (placed.positioning == Positioning.ABSOLUTE) return "x - ${rect[0]}, y - ${rect[1]}"

        val fromLeft = rect[0]
        val fromRight = width - (rect[0] + rect[2])
        val fromTop = rect[1]
        val fromBottom = height - (rect[1] + rect[3])
        val across = if (fromLeft <= fromRight) "$fromLeft px from left" else "$fromRight px from right"
        val down = if (fromTop <= fromBottom) "$fromTop px from top" else "$fromBottom px from bottom"
        return "$across, $down"
    }

    private fun signed(n: Int): String = if (n >= 0) "+$n" else "$n"

    private fun transparency(alpha: Float): Int = ((1f - alpha) * 100).roundToInt()

    private fun drawReadout(graphics: GuiGraphicsExtractor, box: HudBox, part: HudPart) {
        val lines = buildList {
            add(part.element.name to Common.UI.ACCENT_COLOR)
            if (box.group != null) add("In a box with ${box.parts.size - 1} other${if (box.parts.size == 2) "" else "s"}" to Common.UI.TEXT_DIM_COLOR)
            add((if (box.placed.positioning == Positioning.RELATIVE && box.placed.tie == null) "Relative: " else "") + describe(box.placed, intArrayOf(box.x, box.y, box.width, box.height)) to Common.UI.TEXT_COLOR)
            add("Scale ${"%.1f".format(part.state.scale)}" to Common.UI.TEXT_COLOR)
            add("Transparency ${transparency(box.alpha)}%" to Common.UI.TEXT_COLOR)
            add((if (isDynamic(box)) "Sizing dynamic" else "Sizing fixed") to Common.UI.TEXT_COLOR)
            tiedTo(box.id).takeIf { it.isNotEmpty() }?.let { add("Holds ${it.joinToString { name -> nameOf(name) }}" to Common.UI.TEXT_DIM_COLOR) }
        }
        drawLines(graphics, lines, box.x, box.y + box.height + Common.UI.SPACING, box.y)
    }

    private fun drawAnchorReadout(graphics: GuiGraphicsExtractor, anchor: AnchorState, x: Int, y: Int) {
        val lines = buildList {
            add(nameOf(anchor.id) to Common.UI.ACCENT_COLOR)
            add(describe(anchor, intArrayOf(x, y, 0, 0)) to Common.UI.TEXT_COLOR)
            add("Transparency ${transparency(anchor.alpha)}%" to Common.UI.TEXT_COLOR)
            tiedTo(anchor.id).takeIf { it.isNotEmpty() }?.let { add("Holds ${it.joinToString { name -> nameOf(name) }}" to Common.UI.TEXT_DIM_COLOR) }
        }
        drawLines(graphics, lines, x + HudScene.ANCHOR_HALF + 3, y + HudScene.ANCHOR_HALF + Common.UI.SPACING, y - HudScene.ANCHOR_HALF)
    }

    /** A small panel of lines under something, or above it when the bottom of the screen is near. */
    private fun drawLines(graphics: GuiGraphicsExtractor, lines: List<Pair<String, Int>>, atX: Int, below: Int, above: Int) {
        val pad = Common.UI.SPACING
        val boxWidth = lines.maxOf { font.width(it.first) } + pad * 2
        val boxHeight = lines.size * (font.lineHeight + 1) + pad * 2
        val left = atX.coerceIn(0, (width - boxWidth).coerceAtLeast(0))
        val top = if (below + boxHeight <= height) below else (above - Common.UI.SPACING - boxHeight).coerceAtLeast(0)

        graphics.drawPanel(left, top, left + boxWidth, top + boxHeight)
        lines.forEachIndexed { index, (text, color) ->
            graphics.text(font, Component.literal(text), left + pad, top + pad + index * (font.lineHeight + 1), color, false)
        }
    }

    private fun drawHints(graphics: GuiGraphicsExtractor) {
        val candidate = pickCandidate()
        val hint = when {
            picking != null && candidate != null -> "Click to tie ${nameOf(picking!!)} to ${nameOf(candidate)}"
            picking != null -> "Click what to tie ${nameOf(picking!!)} to, Escape to stop"
            selectedId != null -> "Arrows nudge ${nameOf(selectedId!!)} · shift arrows resize from the bottom right, with ctrl from the top left · wheel fades · shift wheel scales · middle click absolute or relative · shift middle click dynamic or fixed size · R resets"
            else -> "Click to select · drag to move · corners resize · drop on another to merge · wheel fades · shift wheel scales · right click for more · R resets"
        }
        val hintWidth = font.width(hint)
        graphics.text(font, Component.literal(hint), (width - hintWidth) / 2, height - font.lineHeight - Common.UI.SPACING_LARGE, if (picking != null) Common.UI.SELECTED_FRAME_COLOR else Common.UI.TEXT_DIM_COLOR, true)
    }

    // ------------------------------------------------------------------ input

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
        val anchor = scene.anchorAt(x, y)
        val box = boxNear(x, y)

        if (panelClicked(x.toInt(), y.toInt(), event.button())) return true

        picking?.let { source ->
            if (event.button() == 0) pickCandidate()?.let { tie(source, it) }
            picking = null
            return true
        }

        if (event.button() == 1) {
            val part = scene.partAt(x, y)
            when {
                anchor != null -> menuForAnchor(anchor.state, x.toInt(), y.toInt())
                box != null && part != null -> menuFor(part, box, x.toInt(), y.toInt())
                else -> openMenu(x.toInt(), y.toInt(), "Here", listOf(HudMenu.Entry("Add anchor") { addAnchor(x.toInt(), y.toInt()) }))
            }
            return true
        }
        if (event.button() == 2) {
            val id = anchor?.state?.id ?: box?.id ?: return false
            when {
                shiftDown() -> box?.let { toggleDynamic(it) }
                layout.placed(id)?.tie != null -> untie(id)
                else -> togglePositioning(id)
            }
            HudLayoutStore.save()
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

        cornerAt(box, x, y)?.let { (leftSide, topSide) ->
            drag = Drag.Resize(box.id, leftSide, topSide, if (leftSide) box.x + box.width else box.x, if (topSide) box.y + box.height else box.y)
            return true
        }
        box.dividerAt(x, y)?.let { index ->
            val a = box.parts[index]
            val b = box.parts[index + 1]
            drag = if (box.stacking == Stacking.VERTICAL) Drag.Divider(box.id, index, y.toInt(), a.height, b.height)
            else Drag.Divider(box.id, index, x.toInt(), a.width, b.width)
            return true
        }
        drag = Drag.Move(box.id, x.toInt() - box.x, y.toInt() - box.y)
        return true
    }

    override fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val current = drag ?: return false
        when (current) {
            is Drag.Move -> {
                moveTo(current.id, event.x.toInt() - current.offsetX, event.y.toInt() - current.offsetY)
                mergeTarget = mergeTargetFor(current.id, event.x, event.y)
            }
            is Drag.Resize -> scene.boxOf(current.id)?.let { resizeTo(it, current, event.x.toInt(), event.y.toInt()) }
            is Drag.Divider -> scene.boxOf(current.id)?.let { dragDivider(it, current, if (it.stacking == Stacking.VERTICAL) event.y.toInt() else event.x.toInt()) }
        }
        return true
    }

    /** The box a lone element being dragged would join, and the edge of it nearest the mouse. */
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
        mergeTarget?.let { (box, edge) -> if (current is Drag.Move) merge(current.id, box, edge) }
        mergeTarget = null
        HudLayoutStore.save()
        return true
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (overlaysMouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true
        val step = if (scrollY > 0) 1 else -1
        scene.anchorAt(mouseX, mouseY)?.let { anchor ->
            anchor.state.alpha = ((anchor.state.alpha + step * ALPHA_STEP) * 100).roundToInt().coerceIn(0, 100) / 100f
            HudLayoutStore.save()
            return true
        }
        val box = scene.boxAt(mouseX, mouseY) ?: return false

        if (shiftDown()) {
            val part = scene.partAt(mouseX, mouseY) ?: return true
            part.state.scale = (part.state.scale + step * SCALE_STEP).coerceIn(MIN_SCALE, MAX_SCALE).let { (it * 10).roundToInt() / 10f }
        } else {
            val placed = box.placed
            val alpha = ((when (placed) {
                is GroupState -> placed.alpha
                is ElementState -> placed.alpha
                else -> 1f
            } + step * ALPHA_STEP) * 100).roundToInt().coerceIn(0, 100) / 100f
            when (placed) {
                is GroupState -> placed.alpha = alpha
                is ElementState -> placed.alpha = alpha
                else -> {}
            }
        }
        rebuild()
        HudLayoutStore.save()
        return true
    }

    override fun onKeyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (picking != null) {
                picking = null
                return true
            }
            if (overlays.isNotEmpty()) {
                closeOverlays()
                return true
            }
        }
        if (overlays.isEmpty() && selectedId != null) {
            val step = when (keyEvent.key()) {
                GLFW.GLFW_KEY_LEFT -> -1 to 0
                GLFW.GLFW_KEY_RIGHT -> 1 to 0
                GLFW.GLFW_KEY_UP -> 0 to -1
                GLFW.GLFW_KEY_DOWN -> 0 to 1
                else -> null
            }
            if (step != null) {
                if (shiftDown()) resizeByKey(step.first, step.second, topLeft = controlDown()) else nudge(step.first, step.second)
                HudLayoutStore.save()
                return true
            }
        }
        if (keyEvent.key() == GLFW.GLFW_KEY_R && overlays.isEmpty()) {
            hoveredPart()?.let {
                reset(it.element.id)
                HudLayoutStore.save()
                return true
            }
        }
        return super.onKeyPressed(keyEvent)
    }

    override fun onClose() {
        HudLayoutStore.save()
        McCompat.setScreen(null)
    }

    override fun removed() {
        HudLayoutStore.save()
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
        const val TAB_WIDTH: Int = 30
        const val TAB_HEIGHT: Int = 12
        const val PANEL_WIDTH: Int = 120
        const val PANEL_ROW: Int = 13
        const val PANEL_PAD: Int = 4

        /** The switch on an element row, a step smaller than a setting's. */
        const val PANEL_SWITCH_WIDTH: Int = 16
        const val PANEL_SWITCH_HEIGHT: Int = 9

        const val HANDLE: Int = 2

        /** How far from a corner dot the mouse still grabs it. */
        const val HANDLE_REACH: Int = 5
        const val ANCHOR_ARM: Int = 4
        const val ALPHA_STEP: Float = 0.05f
        const val SCALE_STEP: Float = 0.1f
        const val MIN_SCALE: Float = 0.5f
        const val MAX_SCALE: Float = 3f
    }
}
