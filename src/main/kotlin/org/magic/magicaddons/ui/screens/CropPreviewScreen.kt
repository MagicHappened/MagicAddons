package org.magic.magicaddons.ui.screens

import net.minecraft.resources.Identifier
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.level.block.state.BlockState
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
import org.magic.magicaddons.util.ScreenUtil.drawWarningBadge
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered

/**
 * Any crop at any stage, drawn as it would stand in a greenhouse: the picker searches, the slider
 * walks the stages, and an unrecorded stage shows a question mark rather than a guess.
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

    /** The ground under the plant: the first soil its definition asks for, one per footprint cell. */
    private var soilBlocks: Map<BlockPos, BlockState> = emptyMap()

    private var yaw: Float = 45f
    private var pitch: Float = -20f

    private var draggingView = false

    /** The slow turn runs until the player turns the plant themselves, and again for the next crop. */
    private var spinning = true
    private var draggingSlider = false

    private val selector = EnumWidget(
        values = CropRegistry.all.sortedBy { it.name },
        currentValue = null as CropDefinition?,
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

        // eight percent of the screen above and below; everything between is the preview's
        previewY = height * 8 / 100
        previewSize = height - previewY * 2
        previewX = (width - previewSize) / 2

        // label and track just inside the box's top edge, on the backdrop, reaching across
        // until the incomplete-data mark's corner
        sliderX = previewX + 10
        sliderW = previewX + previewSize - 26 - sliderX
        sliderY = previewY + font.lineHeight + 8

        // the picker stands off to the left, its top lined up with the preview's
        selector.height = 22
        selector.fitToValues((previewX - Common.UI.SPACING_LARGE * 2).coerceAtLeast(80))
        selector.x = Common.UI.SPACING_LARGE
        selector.y = previewY

        // the list stops short of the chat, give or take: about six rows above the bottom
        selector.overlayBudget = height - (selector.y + selector.height) - selector.height * 6
    }

    private fun picked(def: CropDefinition) {
        selectedDef = def
        spinning = true
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

    /** Built from the same pieces the holograms use, at a spot whose rotation and pose both come out zero. */
    private fun rebuildScene() {
        sceneStage = null
        sceneData = null
        soilBlocks = emptyMap()

        val def = selectedDef ?: return
        val level = Minecraft.getInstance().level ?: return

        val stageDef = def.stageDefs
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }
            .firstOrNull { stage in it.stageRange } ?: return

        sceneStage = stageDef
        sceneData = stageDef.toRenderData(level, ORIGIN, def.footprint, def.standPoses, def.rotatesWithPlot)

        // so the plant is not left floating in a void: the ground it grows from, drawn under it
        soilBlocks = def.requiredSoil.firstOrNull()?.defaultBlockState()?.let { soil ->
            buildMap {
                for (dx in 0 until def.footprint.width) {
                    for (dz in 0 until def.footprint.height) {
                        put(ORIGIN.offset(dx, 0, dz), soil)
                    }
                }
            }
        } ?: emptyMap()
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

        // the scene sits a little under the middle of the box, since a plant's own middle looks high
        val center = Vec3(ORIGIN.x + w / 2.0, (minY + maxY) / 2.0 + SCENE_DROP, ORIGIN.z + h / 2.0)
        val extent = maxOf(maxY - minY, maxOf(w, h).toDouble(), 2.0)

        return center to extent
    }

    /** What this stage was recorded without, in the collector's own words. Empty when whole. */
    private fun missingData(): List<String> {
        val def = selectedDef ?: return emptyList()
        val stageDef = sceneStage ?: return emptyList()

        val missing = mutableListOf<String>()

        if (PlantDex.needsRotation(def, stage)) missing += "rotation data"
        PlantDex.neededSize(def, stage)?.let { missing += "isSmall = $it" }
        if (stageDef.blocks.orEmpty().any { it.blockState == null }) missing += "block data"

        return missing
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val def = selectedDef

        if (spinning && !draggingView) yaw = (yaw + delta * 1.2f) % 360f

        // the same backdrop the greenhouse screen boxes its grid with
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.fromNamespaceAndPath("minecraft", "popup/background"),
            previewX - BORDER_PAD,
            previewY - BORDER_PAD,
            previewSize + BORDER_PAD * 2,
            previewSize + BORDER_PAD * 2
        )

        when {
            def == null -> graphics.drawMultilineBoxCentered(
                "Pick a crop",
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
                blocks = soilBlocks + data.blockMap,
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

        val markX = previewX + previewSize - BORDER_PAD - BADGE_SIZE
        val markY = previewY + BORDER_PAD

        graphics.drawWarningBadge(markX, markY, BADGE_SIZE)

        if (mouseX in markX..markX + BADGE_SIZE && mouseY in markY..markY + BADGE_SIZE) {
            // under the badge, so it never covers what it is about
            graphics.drawSimpleTooltip(
                "Data is incomplete for this stage, may be inaccurate\n" +
                        "data missing: ${missing.joinToString(", ")}",
                markX + BADGE_SIZE - TOOLTIP_WIDTH_HINT,
                markY + BADGE_SIZE + Common.UI.SPACING
            )
        }
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, a)
        }
        graphics.fill(0, 0, width, height, Common.UI.SCREEN_DIM_COLOR)
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
            spinning = false
            // sideways dragging turned out to feel right the way it first was
            yaw = (yaw + dragX.toFloat() * 0.8f) % 360f
            pitch = (pitch - dragY.toFloat() * 0.5f).coerceIn(-75f, 30f)
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

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        overlays.toList().forEach {
            if (it.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
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

        /** How many blocks the scene is drawn below the box's middle. */
        const val SCENE_DROP: Double = 0.75

        /** The same breathing room the greenhouse screen gives its grid inside the backdrop. */
        const val BORDER_PAD: Int = 6

        const val SLIDER_HEIGHT: Int = 10
        const val HANDLE_WIDTH: Int = 5

        const val BADGE_SIZE: Int = 16

        /** The badge is at the box's right edge, so the tooltip is pulled left to stay inside it. */
        const val TOOLTIP_WIDTH_HINT: Int = 170
    }
}
