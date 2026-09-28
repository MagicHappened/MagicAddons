package org.magic.magicaddons.ui.hud

import net.minecraft.client.Minecraft
import org.magic.magicaddons.ui.fonts.ModFont
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.magic.magicaddons.Common
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.render.HudRenderEvent
import org.magic.magicaddons.ui.ScreenRect
import org.magic.magicaddons.util.compat.McCompat
import kotlin.math.roundToInt

class HudPart(
    val element: HudElement,
    val state: ElementState,
    val laidContent: HudPainter.LaidContent,
    val textWidthUnits: Int
) {
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0

    val minWidth: Int get() = HudPainter.minBoxWidth(state.scale)
    val minHeight: Int get() = HudPainter.minBoxHeight(state.scale)

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height
}

class HudBox(
    val id: String,
    val placement: HudPlacement,
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

    val minWidth: Int
        get() = if (stacking == Stacking.HORIZONTAL) parts.sumOf { it.minWidth } + (parts.size - 1) * HudScene.DIVIDER_THICKNESS else parts.maxOf { it.minWidth }
    val minHeight: Int
        get() = if (stacking == Stacking.VERTICAL) parts.sumOf { it.minHeight } + (parts.size - 1) * HudScene.DIVIDER_THICKNESS else parts.maxOf { it.minHeight }

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x until x + width && mouseY.toInt() in y until y + height

    fun dividerAt(mouseX: Double, mouseY: Double): Int? {
        if (!contains(mouseX, mouseY)) return null
        for (index in 1 until parts.size) {
            val part = parts[index]
            val distanceFromDivider = if (stacking == Stacking.HORIZONTAL) {
                mouseX.toInt() - (part.x - HudScene.DIVIDER_THICKNESS)
            } else {
                mouseY.toInt() - (part.y - HudScene.DIVIDER_THICKNESS)
            }
            if (distanceFromDivider in -HudScene.DIVIDER_GRAB_DISTANCE..HudScene.DIVIDER_GRAB_DISTANCE) return index - 1
        }
        return null
    }
}

class AnchorMarker(val state: AnchorState) {
    var x: Int = 0
    var y: Int = 0

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX.toInt() in x - HudScene.ANCHOR_HALF_SIZE..x + HudScene.ANCHOR_HALF_SIZE &&
                mouseY.toInt() in y - HudScene.ANCHOR_HALF_SIZE..y + HudScene.ANCHOR_HALF_SIZE
}

class HudScene(val boxes: List<HudBox>, val anchors: List<AnchorMarker>) {

    fun boxOf(id: String): HudBox? = boxes.firstOrNull { it.id == id }

    fun boxUnderCursor(mouseX: Double, mouseY: Double): HudBox? = boxes.lastOrNull { it.contains(mouseX, mouseY) }

    fun partUnderCursor(mouseX: Double, mouseY: Double): HudPart? =
        boxUnderCursor(mouseX, mouseY)?.parts?.firstOrNull { it.contains(mouseX, mouseY) }

    fun anchorUnderCursor(mouseX: Double, mouseY: Double): AnchorMarker? = anchors.lastOrNull { it.contains(mouseX, mouseY) }

    fun rectOf(id: String): ScreenRect? =
        boxOf(id)?.let { ScreenRect(it.x, it.y, it.width, it.height) }
            ?: anchors.firstOrNull { it.state.id == id }?.let { ScreenRect(it.x, it.y, 0, 0) }

    fun rectById(): Map<String, ScreenRect> = buildMap {
        boxes.forEach { put(it.id, ScreenRect(it.x, it.y, it.width, it.height)) }
        anchors.forEach { put(it.state.id, ScreenRect(it.x, it.y, 0, 0)) }
    }

    fun drawBoxes(graphics: GuiGraphicsExtractor) {
        boxes.forEach { box ->
            HudPainter.drawBoxPanel(graphics, box.x, box.y, box.width, box.height, box.alpha)
            box.parts.forEachIndexed { index, part ->
                if (index > 0 && box.alpha > 0f) {
                    val dividerColor = HudPainter.withScaledAlpha(Common.UI.BORDER_COLOR, box.alpha)
                    if (box.stacking == Stacking.HORIZONTAL) {
                        graphics.fill(part.x - DIVIDER_THICKNESS, box.y, part.x, box.y + box.height, dividerColor)
                    } else {
                        graphics.fill(box.x, part.y - DIVIDER_THICKNESS, box.x + box.width, part.y, dividerColor)
                    }
                }
                graphics.enableScissor(part.x, part.y, part.x + part.width, part.y + part.height)
                HudPainter.drawLaidContent(
                    graphics, part.laidContent, part.x + HudPainter.BOX_PADDING, part.y + HudPainter.BOX_PADDING,
                    part.textWidthUnits, part.state.scale, part.element.hasTextShadow
                )
                graphics.disableScissor()
            }
        }
    }

    companion object {
        const val DIVIDER_THICKNESS: Int = 1

        const val DIVIDER_GRAB_DISTANCE: Int = 3

        const val ANCHOR_HALF_SIZE: Int = 6

        fun buildScene(
            layout: HudLayout,
            screenWidth: Int,
            screenHeight: Int,
            useSamples: Boolean,
            isIncluded: (HudElement) -> Boolean = { true }
        ): HudScene {
            val contentByElement = HudElements.all.filter(isIncluded).mapNotNull { element ->
                val content = element.currentContent() ?: if (useSamples) element.sampleContent() else null
                content?.let { element to it }
            }.toMap()

            val boxes = mutableListOf<HudBox>()
            val groupedElementIds = mutableSetOf<String>()

            layout.groups.forEach { group ->
                val members = group.members.mapNotNull { id ->
                    HudElements.elementById(id)?.let { element -> contentByElement[element]?.let { element to it } }
                }
                if (members.isEmpty()) return@forEach
                members.forEach { groupedElementIds.add(it.first.id) }
                boxes.add(buildGroupBox(layout, group, members))
            }

            contentByElement.forEach { (element, content) ->
                if (element.id in groupedElementIds) return@forEach
                val state = layout.elementStateOf(element)
                val draggedWidth = state.width.takeUnless { state.isSizedByContent }
                val boxWidth = (draggedWidth ?: HudPainter.naturalBoxWidth(content, state.scale)).coerceAtLeast(HudPainter.minBoxWidth(state.scale))
                val part = layPart(element, state, content, boxWidth, state.height.takeUnless { state.isSizedByContent })
                boxes.add(HudBox(element.id, state, null, listOf(part), boxWidth, part.height, state.alpha))
            }

            val anchors = layout.anchors.map { AnchorMarker(it) }
            val scene = HudScene(boxes, anchors)
            scene.placeBoxesAndAnchors(layout, screenWidth, screenHeight)
            return scene
        }

        private fun layPart(element: HudElement, state: ElementState, content: HudContent, partWidth: Int, partHeight: Int?): HudPart {
            val textWidthUnits = HudPainter.textWidthUnits(partWidth, state.scale)
            val laidContent = HudPainter.layContent(content, textWidthUnits)
            return HudPart(element, state, laidContent, textWidthUnits).also {
                it.width = partWidth
                it.height = (partHeight ?: HudPainter.boxHeight(laidContent, state.scale)).coerceAtLeast(it.minHeight)
            }
        }

        private fun buildGroupBox(layout: HudLayout, group: GroupState, members: List<Pair<HudElement, HudContent>>): HudBox {
            val parts: List<HudPart>
            val boxWidth: Int
            val boxHeight: Int

            if (group.stacking == Stacking.VERTICAL) {
                val naturalWidth = members.maxOf { (element, content) -> HudPainter.naturalBoxWidth(content, layout.elementStateOf(element).scale) }
                val minimumWidth = members.maxOf { (element, _) -> HudPainter.minBoxWidth(layout.elementStateOf(element).scale) }
                boxWidth = (group.width.takeUnless { group.isSizedByContent } ?: naturalWidth).coerceAtLeast(minimumWidth)
                parts = members.map { (element, content) ->
                    val state = layout.elementStateOf(element)
                    layPart(element, state, content, boxWidth, state.height.takeUnless { group.isSizedByContent })
                }
                boxHeight = parts.sumOf { it.height } + (parts.size - 1) * DIVIDER_THICKNESS
            } else {
                parts = members.map { (element, content) ->
                    val state = layout.elementStateOf(element)
                    val partWidth = (state.width.takeUnless { group.isSizedByContent } ?: HudPainter.naturalBoxWidth(content, state.scale))
                        .coerceAtLeast(HudPainter.minBoxWidth(state.scale))
                    layPart(element, state, content, partWidth, null)
                }
                val sharedHeight = (group.height.takeUnless { group.isSizedByContent } ?: parts.maxOf { it.height })
                    .coerceAtLeast(parts.maxOf { it.minHeight })
                parts.forEach { it.height = sharedHeight }
                boxWidth = parts.sumOf { it.width } + (parts.size - 1) * DIVIDER_THICKNESS
                boxHeight = sharedHeight
            }
            return HudBox(group.id, group, group, parts, boxWidth, boxHeight, group.alpha)
        }
    }

    private fun placeBoxesAndAnchors(layout: HudLayout, screenWidth: Int, screenHeight: Int) {
        val positionById = mutableMapOf<String, Pair<Int, Int>>()

        fun resolvePosition(id: String, tieDepth: Int): Pair<Int, Int>? {
            positionById[id]?.let { return it }
            val box = boxOf(id)
            val anchor = anchors.firstOrNull { it.state.id == id }
            val placement = box?.placement ?: anchor?.state ?: return null
            val width = box?.width ?: 0
            val height = box?.height ?: 0

            val tie = placement.tie
            val tiedPosition = if (tie != null && tieDepth < HudLayout.MAX_TIE_CHAIN && layout.placementById(tie.target) != null) {
                resolvePosition(tie.target, tieDepth + 1)?.let { (targetX, targetY) -> (targetX + tie.offsetX) to (targetY + tie.offsetY) }
            } else {
                null
            }

            val (x, y) = when {
                tiedPosition != null -> tiedPosition
                placement.positioning == Positioning.RELATIVE ->
                    (placement.fractionX * (screenWidth - width)).roundToInt() to (placement.fractionY * (screenHeight - height)).roundToInt()
                else -> placement.x to placement.y
            }
            val onScreenPosition = x.coerceIn(0, (screenWidth - width).coerceAtLeast(0)) to y.coerceIn(0, (screenHeight - height).coerceAtLeast(0))
            positionById[id] = onScreenPosition
            return onScreenPosition
        }

        boxes.forEach { box ->
            val (boxX, boxY) = resolvePosition(box.id, 0) ?: return@forEach
            box.x = boxX
            box.y = boxY
            var partX = box.x
            var partY = box.y
            box.parts.forEach { part ->
                part.x = partX
                part.y = partY
                if (box.stacking == Stacking.HORIZONTAL) partX += part.width + DIVIDER_THICKNESS else partY += part.height + DIVIDER_THICKNESS
            }
        }
        anchors.forEach { anchor ->
            val (anchorX, anchorY) = resolvePosition(anchor.state.id, 0) ?: return@forEach
            anchor.x = anchorX
            anchor.y = anchorY
        }
    }
}

object HudRenderer {
    init {
        EventBus.register(this)
    }

    @EventHandler
    fun onHudRender(event: HudRenderEvent) {
        if (McCompat.hudHidden() || McCompat.currentScreen() != null) return
        val window = Minecraft.getInstance().window
        ModFont.replaceDefaultFont {
            HudScene.buildScene(HudLayoutFile.layout, window.guiScaledWidth, window.guiScaledHeight, useSamples = false).drawBoxes(event.graphics)
        }
    }
}
