package org.magic.magicaddons.ui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.PlantDex
import org.magic.magicaddons.render.CropPreviewRenderState
import org.magic.magicaddons.render.StandInScene
import org.magic.magicaddons.ui.HoverableContainer
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.EnumWidget
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered

/**
 * A crop turned over in the hand: any crop, at any stage, drawn in three dimensions the way it
 * would stand in a greenhouse.
 *
 * The picker sits at the bottom and narrows as its search box is typed into; the slider above the
 * preview walks the stages. The plant spins slowly on its own and can be dragged around by hand. A
 * stage nobody has recorded shows a question mark instead of a guess, and a stage recorded without
 * all of its data carries a red mark saying exactly what is missing.
 */
class CropPreviewScreen(
    private val parent: Screen
) : Screen(Component.literal("Crop Preview")), OverlayContext, HoverableContainer {

    override val overlays: MutableList<OverlayRenderable> = mutableListOf()
    override var hoveredElement: GuiEventListener? = null

    private var selectedDef: CropDefinition? = null
    private var stage: Int = 1

    /** The stage as it will be drawn, or null while it is unrecorded and shown as a question. */
    private var sceneStage: CropStage? = null
    private var sceneData: CropStage.RenderData? = null

    private var yaw: Float = 45f
    private var pitch: Float = -20f

    private var draggingView = false
    private var draggingSlider = false

    private val selector = EnumWidget(
        values = CropRegistry.all.sortedBy { it.name },
        currentValue = null as CropDefinition?,
        includeSearch = true,
        overlayContext = this,
        valueChanged = { picked(it) }
    )

    private var previewX = 0
    private var previewY = 0
    private var previewSize = 0

    private var sliderX = 0
    private var sliderY = 0
    private var sliderW = 0

    override fun init() {
        super.init()

        selector.height = 22
        selector.fitToValues(240)
        selector.x = (width - selector.width) / 2
        selector.y = height - selector.height - BOTTOM_PADDING

        previewSize = (minOf(width, height) * 0.55).toInt()
        previewX = (width - previewSize) / 2
        previewY = ((height - previewSize) / 2).coerceAtLeast(SLIDER_ROOM)

        sliderW = previewSize - 40
        sliderX = previewX + 20
        sliderY = previewY - 14
    }

    private fun picked(def: CropDefinition) {
        selectedDef = def
        stage = stage.coerceIn(1, def.maxStage)
        rebuildScene()
    }

    private fun setStage(newStage: Int) {
        val def = selectedDef ?: return
        val clamped = newStage.coerceIn(1, def.maxStage)

        if (clamped == stage) return
        stage = clamped
        rebuildScene()
    }

    /**
     * Builds the plant out of the same pieces the holograms use, at a spot whose world rotation
     * and pose cycle both come out to zero, so what is shown is the crop's own canonical look.
     */
    private fun rebuildScene() {
        sceneStage = null
        sceneData = null

        val def = selectedDef ?: return
        val level = Minecraft.getInstance().level ?: return

        val stageDef = def.stageDefs
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }
            .firstOrNull { stage in it.stageRange } ?: return

        sceneStage = stageDef
        sceneData = stageDef.toRenderData(level, ORIGIN, def.footprint, def.standPoses)
    }

    /** Where the middle of the scene sits, and how many blocks it spans at its widest. */
    private fun centerAndExtent(data: CropStage.RenderData): Pair<Vec3, Double> {
        var minY = 0.0
        var maxY = 1.0

        data.blockMap.keys.forEach {
            minY = minOf(minY, it.y.toDouble())
            maxY = maxOf(maxY, it.y + 1.0)
        }
        data.stands.forEach {
            minY = minOf(minY, it.y)
            maxY = maxOf(maxY, it.y + 1.2)
        }

        val footprint = selectedDef?.footprint
        val w = footprint?.width ?: 1
        val h = footprint?.height ?: 1

        val center = Vec3(ORIGIN.x + w / 2.0, (minY + maxY) / 2.0, ORIGIN.z + h / 2.0)
        val extent = maxOf(maxY - minY, maxOf(w, h).toDouble(), 2.0)

        return center to extent
    }

    /** What this stage was recorded without, in the collector's own words. Empty when whole. */
    private fun missingData(): List<String> {
        val def = selectedDef ?: return emptyList()
        val stageDef = sceneStage ?: return emptyList()

        val missing = mutableListOf<String>()

        if (PlantDex.needsRotation(def, stage)) missing += "rotation data"
        if (stageDef.blocks.orEmpty().any { it.blockState == null }) missing += "block data"

        return missing
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val def = selectedDef

        // the slow turn, resting while the player is the one turning it
        if (!draggingView) yaw = (yaw + delta * 1.2f) % 360f

        graphics.drawBorder(
            previewX, previewY,
            previewX + previewSize, previewY + previewSize,
            1, Common.UI.BORDER_COLOR
        )

        when {
            def == null -> graphics.drawMultilineBoxCentered(
                "Pick a crop below",
                previewX + previewSize / 2,
                previewY + previewSize / 2
            )

            sceneStage == null || sceneData == null -> drawUnknown(graphics)

            else -> submitScene(graphics, delta)
        }

        if (def != null) {
            drawSlider(graphics, def)
            drawIncompleteMark(graphics, mouseX, mouseY)
        }

        selector.extractRenderState(graphics, mouseX, mouseY, delta)

        overlays.asReversed().forEach {
            it.renderOverlay(graphics, mouseX, mouseY, delta)
        }
    }

    /** The plant itself, handed to the gui pipeline to draw with real depth. */
    private fun submitScene(graphics: GuiGraphicsExtractor, delta: Float) {
        val data = sceneData ?: return
        val (center, extent) = centerAndExtent(data)

        val dispatcher = Minecraft.getInstance().entityRenderDispatcher

        val stands = data.stands.map { stand ->
            val state = dispatcher.extractEntity(stand, delta)

            // the scene floats in a void with no light of its own, and a head lit by where the
            // stand happens to technically be is a head drawn black
            state.lightCoords = FULL_BRIGHT

            StandInScene(
                state,
                stand.x - center.x,
                stand.y - center.y,
                stand.z - center.z
            )
        }

        graphics.guiRenderState.addPicturesInPictureState(
            CropPreviewRenderState(
                blocks = data.blockMap,
                stands = stands,
                sceneCenter = center,
                yawDeg = yaw,
                pitchDeg = pitch,
                bX0 = previewX + 1,
                bY0 = previewY + 1,
                bX1 = previewX + previewSize - 1,
                bY1 = previewY + previewSize - 1,
                pixelsPerBlock = (previewSize / (extent * 1.4)).toFloat(),
                scissor = null
            )
        )
    }

    /** A stage nobody has recorded shows a question rather than a guess. */
    private fun drawUnknown(graphics: GuiGraphicsExtractor) {
        val pose = graphics.pose()

        pose.pushMatrix()
        pose.translate(
            (previewX + previewSize / 2).toFloat(),
            (previewY + previewSize / 2).toFloat()
        )
        pose.scale(4f, 4f)

        graphics.text(
            font,
            Component.literal("?"),
            -font.width("?") / 2,
            -font.lineHeight / 2,
            Common.UI.TEXT_COLOR,
            false
        )
        pose.popMatrix()
    }

    private fun drawSlider(graphics: GuiGraphicsExtractor, def: CropDefinition) {
        if (def.maxStage <= 1) return

        val trackY = sliderY + SLIDER_HEIGHT / 2

        graphics.fill(sliderX, trackY - 1, sliderX + sliderW, trackY + 1, Common.UI.BORDER_COLOR)

        val handleX = sliderX + ((stage - 1) * (sliderW - HANDLE_WIDTH)) / (def.maxStage - 1)

        graphics.fill(
            handleX, sliderY,
            handleX + HANDLE_WIDTH, sliderY + SLIDER_HEIGHT,
            Common.UI.TEXT_COLOR
        )

        val label = "Stage $stage / ${def.maxStage}"

        graphics.text(
            font,
            Component.literal(label),
            previewX + (previewSize - font.width(label)) / 2,
            sliderY - font.lineHeight - 2,
            Common.UI.TEXT_COLOR,
            false
        )
    }

    /** The red mark on a stage recorded without all of itself, naming what is missing on hover. */
    private fun drawIncompleteMark(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val missing = missingData()
        if (missing.isEmpty()) return

        val markX = previewX + previewSize - 10
        val markY = previewY + 4

        graphics.text(font, Component.literal("!"), markX, markY, INCOMPLETE_COLOR, false)

        if (mouseX in markX - 3..markX + 8 && mouseY in markY - 2..markY + font.lineHeight + 2) {
            graphics.drawSimpleTooltip(
                "Data is incomplete for this stage, may be inaccurate\n" +
                        "data missing: ${missing.joinToString(", ")}",
                mouseX + 7,
                mouseY + 12
            )
        }
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, a)
        }
        this.extractMenuBackground(graphics)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        overlays.toList().forEach {
            if (it.mouseClicked(mouseButtonEvent, doubled)) return true
        }

        if (selector.mouseClicked(mouseButtonEvent, doubled)) return true

        closeOverlays()

        val mx = mouseButtonEvent.x.toInt()
        val my = mouseButtonEvent.y.toInt()
        val def = selectedDef

        if (def != null && def.maxStage > 1 &&
            my in sliderY - 2..sliderY + SLIDER_HEIGHT + 2 && mx in sliderX..sliderX + sliderW
        ) {
            draggingSlider = true
            dragSliderTo(mouseButtonEvent.x)
            return true
        }

        if (mouseButtonEvent.button() == 0 &&
            mx in previewX..previewX + previewSize && my in previewY..previewY + previewSize
        ) {
            draggingView = true
            return true
        }

        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (draggingSlider) {
            dragSliderTo(mouseButtonEvent.x)
            return true
        }

        if (draggingView) {
            yaw = (yaw + dragX.toFloat() * 0.8f) % 360f
            pitch = (pitch + dragY.toFloat() * 0.5f).coerceIn(-75f, 30f)
            return true
        }

        return super.mouseDragged(mouseButtonEvent, dragX, dragY)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        draggingSlider = false
        draggingView = false

        return super.mouseReleased(mouseButtonEvent)
    }

    private fun dragSliderTo(mouseX: Double) {
        val def = selectedDef ?: return
        if (def.maxStage <= 1) return

        val along = ((mouseX - sliderX) / sliderW).coerceIn(0.0, 1.0)

        setStage(1 + Math.round(along * (def.maxStage - 1)).toInt())
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        overlays.toList().forEach { it.mouseMoved(mouseX, mouseY) }
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        overlays.toList().forEach {
            if (it.charTyped(characterEvent)) return true
        }

        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        overlays.toList().forEach {
            if (it.keyPressed(keyEvent)) return true
        }

        return super.keyPressed(keyEvent)
    }

    /** Escape goes back to the greenhouse screen it came from, not out of everything. */
    override fun onClose() {
        ScreenUtil.setScreen(parent)
    }

    private companion object {
        /** Rotation-zero, pose-cycle-zero: the crop's canonical look. */
        val ORIGIN: BlockPos = BlockPos(0, 0, 0)

        const val FULL_BRIGHT: Int = 0xF000F0

        const val BOTTOM_PADDING: Int = 14
        const val SLIDER_ROOM: Int = 40
        const val SLIDER_HEIGHT: Int = 10
        const val HANDLE_WIDTH: Int = 5

        val INCOMPLETE_COLOR: Int = 0xFFFF4444.toInt()
    }
}
