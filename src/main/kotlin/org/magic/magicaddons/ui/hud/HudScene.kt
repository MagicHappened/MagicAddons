package org.magic.magicaddons.ui.hud

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.Common
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.render.OnHudRenderEvent
import org.magic.magicaddons.util.compat.McCompat
import kotlin.math.roundToInt

/** One element as it sits in a box: its content laid out, and its rectangle on screen. */
class HudPart(
    val element: HudElement,
    val state: ElementState,
    val laid: HudPainter.Laid,
    val innerUnits: Int
) {
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0

    val minWidth: Int get() = HudPainter.minWidth(state.scale)
    val minHeight: Int get() = HudPainter.minHeight(state.scale)

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height
}

/** A panel on screen: one element, or a group of them stacked with lines between. */
class HudBox(
    /** The element's id, or the group's. */
    val id: String,
    val placed: Placed,
    val group: GroupState?,
    val parts: List<HudPart>,
    val width: Int,
    val height: Int,
    val alpha: Float
) {
    var x: Int = 0
    var y: Int = 0

    val centerX: Int get() = x + width / 2
    val centerY: Int get() = y + height / 2

    val stacking: Stacking? get() = group?.stacking

    /** The narrowest and shortest the box can be dragged, its parts at their own limits. */
    val minWidth: Int
        get() = if (stacking == Stacking.HORIZONTAL) parts.sumOf { it.minWidth } + (parts.size - 1) * HudScene.DIVIDER else parts.maxOf { it.minWidth }
    val minHeight: Int
        get() = if (stacking == Stacking.VERTICAL) parts.sumOf { it.minHeight } + (parts.size - 1) * HudScene.DIVIDER else parts.maxOf { it.minHeight }

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height

    /** Which line between two parts the mouse is on, as the index of the part above or left of it. */
    fun dividerAt(mouseX: Double, mouseY: Double): Int? {
        if (!contains(mouseX, mouseY)) return null
        for (index in 1 until parts.size) {
            val part = parts[index]
            val along = if (stacking == Stacking.HORIZONTAL) mouseX.toInt() - (part.x - HudScene.DIVIDER) else mouseY.toInt() - (part.y - HudScene.DIVIDER)
            if (along in -HudScene.DIVIDER_REACH..HudScene.DIVIDER_REACH) return index - 1
        }
        return null
    }
}

class HudAnchorPoint(val state: AnchorState) {
    var x: Int = 0
    var y: Int = 0

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x - HudScene.ANCHOR_HALF..x + HudScene.ANCHOR_HALF && mouseY.toInt() in y - HudScene.ANCHOR_HALF..y + HudScene.ANCHOR_HALF
}

/**
 * Every box and anchor placed for one frame. Built from the layout each time, which is cheap,
 * so the game hud and the editor draw the very same thing.
 */
class HudScene(val boxes: List<HudBox>, val anchors: List<HudAnchorPoint>) {

    fun boxOf(id: String): HudBox? = boxes.firstOrNull { it.id == id }

    fun boxAt(mouseX: Double, mouseY: Double): HudBox? = boxes.lastOrNull { it.contains(mouseX, mouseY) }

    fun partAt(mouseX: Double, mouseY: Double): HudPart? =
        boxAt(mouseX, mouseY)?.parts?.firstOrNull { it.contains(mouseX, mouseY) }

    fun anchorAt(mouseX: Double, mouseY: Double): HudAnchorPoint? = anchors.lastOrNull { it.contains(mouseX, mouseY) }

    /** The rectangle of [id]: a box's, or an anchor's point with no size. */
    fun rectOf(id: String): IntArray? =
        boxOf(id)?.let { intArrayOf(it.x, it.y, it.width, it.height) }
            ?: anchors.firstOrNull { it.state.id == id }?.let { intArrayOf(it.x, it.y, 0, 0) }

    fun rects(): Map<String, IntArray> = buildMap {
        boxes.forEach { put(it.id, intArrayOf(it.x, it.y, it.width, it.height)) }
        anchors.forEach { put(it.state.id, intArrayOf(it.x, it.y, 0, 0)) }
    }

    /** The boxes and their content; a part's text is cut off at the part's own edge. */
    fun draw(graphics: GuiGraphicsExtractor) {
        boxes.forEach { box ->
            HudPainter.drawPanel(graphics, box.x, box.y, box.width, box.height, box.alpha)
            box.parts.forEachIndexed { index, part ->
                if (index > 0 && box.alpha > 0f) {
                    val line = HudPainter.faded(Common.UI.BORDER_COLOR, box.alpha)
                    if (box.stacking == Stacking.HORIZONTAL) {
                        graphics.fill(part.x - DIVIDER, box.y, part.x, box.y + box.height, line)
                    } else {
                        graphics.fill(box.x, part.y - DIVIDER, box.x + box.width, part.y, line)
                    }
                }
                graphics.enableScissor(part.x, part.y, part.x + part.width, part.y + part.height)
                HudPainter.draw(graphics, part.laid, part.x + HudPainter.PAD, part.y + HudPainter.PAD, part.innerUnits, part.state.scale, part.element.shadow)
                graphics.disableScissor()
            }
        }
    }

    companion object {
        const val DIVIDER: Int = 1

        /** How far from a divider line the mouse still counts as on it. */
        const val DIVIDER_REACH: Int = 3

        /** Half the side of the box an anchor is drawn and grabbed as, in the editor. */
        const val ANCHOR_HALF: Int = 6

        /** Lays the whole hud out for a screen of [screenWidth] by [screenHeight]; with [sample], every element shows its sample. */
        fun build(layout: HudLayout, screenWidth: Int, screenHeight: Int, sample: Boolean, include: (HudElement) -> Boolean = { true }): HudScene {
            val contents = HudElements.all.filter(include).mapNotNull { element ->
                val content = if (sample) element.sample() else element.content()
                content?.let { element to it }
            }.toMap()

            val boxes = mutableListOf<HudBox>()
            val grouped = mutableSetOf<String>()

            layout.groups.forEach { group ->
                val members = group.members.mapNotNull { id -> HudElements.byId(id)?.let { element -> contents[element]?.let { element to it } } }
                if (members.isEmpty()) return@forEach
                members.forEach { grouped.add(it.first.id) }
                boxes.add(groupBox(layout, group, members))
            }

            contents.forEach { (element, content) ->
                if (element.id in grouped) return@forEach
                val state = layout.stateOf(element)
                val width = (state.width ?: HudPainter.naturalWidth(content, state.scale)).coerceAtLeast(HudPainter.minWidth(state.scale))
                val part = part(element, state, content, width, state.height)
                boxes.add(HudBox(element.id, state, null, listOf(part), width, part.height, state.alpha))
            }

            val anchors = layout.anchors.map { HudAnchorPoint(it) }
            val scene = HudScene(boxes, anchors)
            scene.place(layout, screenWidth, screenHeight)
            return scene
        }

        /** A part [width] wide, as tall as [height] asks or as its content needs, never under a line of text. */
        private fun part(element: HudElement, state: ElementState, content: HudContent, width: Int, height: Int?): HudPart {
            val inner = HudPainter.innerUnits(width, state.scale)
            val laid = HudPainter.lay(content, inner)
            return HudPart(element, state, laid, inner).also {
                it.width = width
                it.height = (height ?: HudPainter.height(laid, state.scale)).coerceAtLeast(it.minHeight)
            }
        }

        private fun groupBox(layout: HudLayout, group: GroupState, members: List<Pair<HudElement, HudContent>>): HudBox {
            val parts: List<HudPart>
            val width: Int
            val height: Int

            if (group.stacking == Stacking.VERTICAL) {
                // one width for all, each part as tall as it is
                val natural = members.maxOf { (element, content) -> HudPainter.naturalWidth(content, layout.stateOf(element).scale) }
                val minimum = members.maxOf { (element, _) -> HudPainter.minWidth(layout.stateOf(element).scale) }
                width = (group.width ?: natural).coerceAtLeast(minimum)
                parts = members.map { (element, content) -> part(element, layout.stateOf(element), content, width, layout.stateOf(element).height) }
                height = parts.sumOf { it.height } + (parts.size - 1) * DIVIDER
            } else {
                // each part as wide as it is, one height for all
                parts = members.map { (element, content) ->
                    val state = layout.stateOf(element)
                    val own = (state.width ?: HudPainter.naturalWidth(content, state.scale)).coerceAtLeast(HudPainter.minWidth(state.scale))
                    part(element, state, content, own, null)
                }
                val shared = (group.height ?: parts.maxOf { it.height }).coerceAtLeast(parts.maxOf { it.minHeight })
                parts.forEach { it.height = shared }
                width = parts.sumOf { it.width } + (parts.size - 1) * DIVIDER
                height = shared
            }
            return HudBox(group.id, group, group, parts, width, height, group.alpha)
        }
    }

    /** Settles where every box and anchor goes, ties resolved through whatever they hang from. */
    private fun place(layout: HudLayout, screenWidth: Int, screenHeight: Int) {
        val done = mutableMapOf<String, IntArray>()

        fun position(id: String, depth: Int): IntArray? {
            done[id]?.let { return it }
            val box = boxOf(id)
            val anchor = anchors.firstOrNull { it.state.id == id }
            val placed = box?.placed ?: anchor?.state ?: return null
            val width = box?.width ?: 0
            val height = box?.height ?: 0

            val tie = placed.tie
            val at = if (tie != null && depth < 16 && layout.placed(tie.target) != null) {
                val target = position(tie.target, depth + 1)
                if (target != null) intArrayOf(target[0] + tie.dx, target[1] + tie.dy) else null
            } else null

            val x: Int
            val y: Int
            if (at != null) {
                x = at[0]
                y = at[1]
            } else if (placed.positioning == Positioning.RELATIVE) {
                x = (placed.fx * (screenWidth - width)).roundToInt()
                y = (placed.fy * (screenHeight - height)).roundToInt()
            } else {
                x = placed.x
                y = placed.y
            }
            // kept on screen whatever the window did since it was placed
            val result = intArrayOf(x.coerceIn(0, (screenWidth - width).coerceAtLeast(0)), y.coerceIn(0, (screenHeight - height).coerceAtLeast(0)))
            done[id] = result
            return result
        }

        boxes.forEach { box ->
            val at = position(box.id, 0) ?: return@forEach
            box.x = at[0]
            box.y = at[1]
            var partX = box.x
            var partY = box.y
            box.parts.forEach { part ->
                part.x = partX
                part.y = partY
                if (box.stacking == Stacking.HORIZONTAL) partX += part.width + DIVIDER else partY += part.height + DIVIDER
            }
        }
        anchors.forEach { anchor ->
            val at = position(anchor.state.id, 0) ?: return@forEach
            anchor.x = at[0]
            anchor.y = at[1]
        }
    }
}

/** Draws the hud every frame from the saved layout. */
object HudRenderer {
    init {
        EventBus.register(this)
    }

    @EventHandler
    fun onHudRender(event: OnHudRenderEvent) {
        if (McCompat.hudHidden() || McCompat.currentScreen() != null) return
        val window = Minecraft.getInstance().window
        HudScene.build(HudLayoutStore.layout, window.guiScaledWidth, window.guiScaledHeight, sample = false).draw(event.graphics)
    }
}
