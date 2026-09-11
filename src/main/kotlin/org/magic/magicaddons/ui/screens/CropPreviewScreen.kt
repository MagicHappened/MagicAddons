package org.magic.magicaddons.ui.screens

import net.minecraft.util.LightCoordsUtil
import org.magic.magicaddons.data.greenhouse.CropStandReader
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.ui.widgets.SliderWidget
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.PlantDex
import org.magic.magicaddons.render.CropPreviewRenderState
import org.magic.magicaddons.render.StandInScene
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.EnumWidget
import org.magic.magicaddons.util.compat.McCompat
import org.magic.magicaddons.util.ScreenUtil.drawSimpleTooltip
import org.magic.magicaddons.util.ScreenUtil.drawWarningBadge
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered

/**
 * Any crop at any stage, drawn as it would stand in a greenhouse: the picker searches, the slider
 * walks the stages, and an unrecorded stage shows a question mark rather than a guess.
 */
class CropPreviewScreen(
    private val parent: Screen?,
    /** A crop to open on, or null for the empty stage. */
    private val initial: CropDefinition? = null
) : MagicScreen(Component.literal("Crop Preview"), "the crop preview"), OverlayContext {

    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    override val backgroundName: String = Customization.PREVIEW_SCREEN

    private var selectedDef: CropDefinition? = null
    private var stage: Int = 1

    /** The stage as it will be drawn, or null while it is unrecorded and shown as a question. */
    private var sceneStage: CropStage? = null
    private var sceneData: CropStage.RenderData? = null

    /** The ground under the plant: the first soil its definition asks for, one per footprint cell. */
    private var soilBlocks: Map<BlockPos, BlockState> = emptyMap()

    /** The lowest and highest point over every stage of the crop, so all stages draw at one scale. */
    private var cropMinY = 0.0
    private var cropMaxY = 1.0

    private var yaw: Float = 45f
    private var pitch: Float = -20f

    private var draggingView = false

    /** The slow turn runs until the player turns the plant themselves, and again for the next crop. */
    private var spinning = true

    /** The looks a plant has more than one of at a stage: what it craves, or whether it sleeps. */
    enum class Variant(private val label: String) {
        Day("Day"), Night("Night"), Awake("Awake"), Asleep("Asleep");

        override fun toString(): String = label
    }

    /** Shown under the crop picker only for crops with such looks, swapping the scene between them. */
    private val variantSelector = EnumWidget(
        values = emptyList<Variant>(),
        currentValue = null as Variant?,
        overlayContext = this,
        searchable = false,
        valueChanged = { rebuildScene() }
    )

    /** The look on show, null for a crop with only one. */
    private val variant: Variant? get() = variantSelector.currentValue

    private fun variantsFor(def: CropDefinition): List<Variant> = when {
        def.stageDefs.any { CropStandReader.CRAVES in it.traits } -> listOf(Variant.Day, Variant.Night)
        def.sleepStages.isNotEmpty() -> listOf(Variant.Awake, Variant.Asleep)
        else -> emptyList()
    }

    /**
     * Whether a look is the one the variant asks for; every look passes when nothing is asked.
     * Asleep only means anything at the stages a plant sleeps at; elsewhere it is awake either way.
     */
    private fun CropStage.wears(variant: Variant?, def: CropDefinition, stage: Int): Boolean = when (variant) {
        Variant.Day -> traits[CropStandReader.CRAVES] == CropStandReader.CRAVES_DAY
        Variant.Night -> traits[CropStandReader.CRAVES] == CropStandReader.CRAVES_NIGHT
        Variant.Asleep -> if (stage in def.sleepStages) readers.any { it.key == CropStandReader.ASLEEP } else readers.none { it.key == CropStandReader.ASLEEP }
        Variant.Awake -> readers.none { it.key == CropStandReader.ASLEEP }
        null -> true
    }

    private val selector = EnumWidget(
        values = CropRegistry.all.sortedBy { it.name },
        currentValue = null as CropDefinition?,
        overlayContext = this,
        valueChanged = { picked(it) }
    )

    private var previewX = 0
    private var previewY = 0
    private var previewSize = 0

    /** The stage picker across the top of the box; its label is drawn above it. */
    private val slider = SliderWidget { setStage(it) }

    override fun onInit() {
        super.onInit()
        if (selectedDef == null && initial != null) {
            selector.currentValue = initial
            picked(initial)
        }

        // a margin of the screen above and below; everything between is the preview's
        previewY = height * PREVIEW_MARGIN_PERCENT / 100
        previewSize = height - previewY * 2
        previewX = (width - previewSize) / 2

        // label and track just inside the box's top edge, on the backdrop, reaching across
        // until the incomplete-data mark's corner
        slider.x = previewX + SLIDER_LEFT_INSET
        slider.width = previewX + previewSize - SLIDER_RIGHT_INSET - slider.x
        slider.y = previewY + font.lineHeight + SLIDER_TOP_GAP

        // the picker stands off to the left, its top lined up with the preview's
        selector.height = SELECTOR_HEIGHT
        selector.fitToValues((previewX - Common.UI.SPACING_LARGE * 2).coerceAtLeast(SELECTOR_MIN_WIDTH))
        selector.x = Common.UI.SPACING_LARGE
        selector.y = previewY

        // the list stops short of the chat, give or take
        selector.overlayBudget = height - (selector.y + selector.height) - selector.height * LIST_ROWS_ABOVE_CHAT

        variantSelector.height = selector.height
        variantSelector.width = selector.width
        variantSelector.x = selector.x
        variantSelector.y = selector.y + selector.height + Common.UI.SPACING
    }

    private fun picked(def: CropDefinition) {
        selectedDef = def
        spinning = true
        variantSelector.values = variantsFor(def)
        variantSelector.currentValue = variantSelector.values.firstOrNull()
        stage = stage.coerceIn(1, def.maxStage)
        slider.range(1, def.maxStage)
        slider.show(stage)
        measureCrop(def)
        rebuildScene()
    }

    private fun measureCrop(def: CropDefinition) {
        cropMinY = 0.0
        cropMaxY = 1.0
        val level = Minecraft.getInstance().level ?: return

        def.stageDefs
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }
            .mapNotNull { it.toRenderData(level, ORIGIN, def.footprint, def.standPoses, def.rotatesWithPlot) }
            .forEach { data ->
                data.blockMap.keys.forEach {
                    cropMinY = minOf(cropMinY, it.y.toDouble())
                    cropMaxY = maxOf(cropMaxY, it.y + 1.0)
                }
                data.stands.forEach {
                    cropMinY = minOf(cropMinY, it.y)
                    cropMaxY = maxOf(cropMaxY, it.y + STAND_HEIGHT)
                }
            }
    }

    private fun setStage(newStage: Int) {
        val def = selectedDef ?: return
        val clamped = newStage.coerceIn(1, def.maxStage)

        if (clamped == stage) return

        slider.show(clamped)

        // one look covers a whole range of stages, so stepping inside that range draws the same scene
        val look = sceneStage
        if (look != null && clamped in look.stageRange && look.wears(variant, def, clamped)) {
            stage = clamped
            return
        }

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
            .filter { stage in it.stageRange && it.wears(variant, def, stage) }
            .let { looks -> looks.firstOrNull { !it.placed } ?: looks.firstOrNull() }
            ?: return

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

    /** Where the middle of the scene sits, and how many blocks the crop spans at its widest stage. */
    private fun centerAndExtent(): Pair<Vec3, Double> {
        val minY = cropMinY
        val maxY = cropMaxY

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
        if (sceneStage == null) return emptyList()

        val missing = mutableListOf<String>()

        if (PlantDex.needsRotation(def, stage)) missing += "rotation data"
        PlantDex.neededSize(def, stage)?.let { missing += "isSmall = $it" }

        return missing
    }

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val def = selectedDef

        if (spinning && !draggingView) yaw = (yaw + delta * SPIN_DEGREES_PER_TICK) % 360f

        // the same panel every other screen boxes its content with
        graphics.drawPanel(
            previewX - BORDER_PAD,
            previewY - BORDER_PAD,
            previewX + previewSize + BORDER_PAD,
            previewY + previewSize + BORDER_PAD
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
            drawSlider(graphics, def, mouseX, mouseY)
            drawIncompleteMark(graphics, mouseX, mouseY)
        }

        selector.extractRenderState(graphics, mouseX, mouseY, delta)
        if (variantSelector.values.isNotEmpty()) variantSelector.extractRenderState(graphics, mouseX, mouseY, delta)

        renderOverlays(graphics, mouseX, mouseY, delta)
    }

    /** The plant itself, handed to the gui pipeline to draw with real depth. */
    private fun submitScene(graphics: GuiGraphicsExtractor, delta: Float) {
        val data = sceneData ?: return
        val (center, extent) = centerAndExtent()

        val dispatcher = Minecraft.getInstance().entityRenderDispatcher

        val stands = data.stands.map { stand ->
            val state = dispatcher.extractEntity(stand, delta)

            // the scene floats in a void with no light of its own, and a head lit by where the
            // stand happens to technically be is a head drawn black
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT

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
                pixelsPerBlock = (previewSize / (extent * VIEW_MARGIN)).toFloat(),
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
        pose.scale(UNKNOWN_MARK_SCALE, UNKNOWN_MARK_SCALE)

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

    private fun drawSlider(graphics: GuiGraphicsExtractor, def: CropDefinition, mouseX: Int, mouseY: Int) {
        if (def.maxStage <= 1) return

        slider.render(graphics, mouseX, mouseY)

        val label = "Stage $stage / ${def.maxStage}"

        graphics.text(
            font,
            Component.literal(label),
            previewX + (previewSize - font.width(label)) / 2,
            slider.y - font.lineHeight - 2,
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

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (overlaysMouseClicked(event, doubled)) return true

        if (selector.mouseClicked(event, doubled)) return true
        if (variantSelector.values.isNotEmpty() && variantSelector.mouseClicked(event, doubled)) return true

        closeOverlays()

        val mx = event.x.toInt()
        val my = event.y.toInt()
        val def = selectedDef

        if (def != null && slider.mouseClicked(event.x, event.y)) return true

        if (event.button() == 0 &&
            mx in previewX..previewX + previewSize && my in previewY..previewY + previewSize
        ) {
            draggingView = true
            return true
        }

        return super.onMouseClicked(event, doubled)
    }

    override fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (slider.mouseDragged(event.x)) return true

        if (draggingView) {
            spinning = false
            yaw = (yaw + dragX.toFloat() * DRAG_YAW_PER_PIXEL) % 360f
            pitch = (pitch - dragY.toFloat() * DRAG_PITCH_PER_PIXEL).coerceIn(MIN_PITCH, MAX_PITCH)
            return true
        }

        return super.onMouseDragged(event, dragX, dragY)
    }

    override fun onMouseReleased(event: MouseButtonEvent): Boolean {
        slider.mouseReleased()
        draggingView = false

        return super.onMouseReleased(event)
    }

    override fun onMouseMoved(mouseX: Double, mouseY: Double) {
        overlaysMouseMoved(mouseX, mouseY)
        selector.mouseMoved(mouseX, mouseY)
        variantSelector.mouseMoved(mouseX, mouseY)
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        overlaysMouseScrolled(mouseX, mouseY, scrollX, scrollY) || super.onMouseScrolled(mouseX, mouseY, scrollX, scrollY)

    override fun onCharTyped(characterEvent: CharacterEvent): Boolean =
        overlaysCharTyped(characterEvent) || super.onCharTyped(characterEvent)

    override fun onKeyPressed(keyEvent: KeyEvent): Boolean =
        overlaysKeyPressed(keyEvent) || super.onKeyPressed(keyEvent)

    /** Escape goes back to the screen it came from, or out to the game when opened by command. */
    override fun finishClose() {
        McCompat.setScreen(parent)
    }

    private companion object {
        /** Rotation-zero, pose-cycle-zero: the crop's canonical look. */
        val ORIGIN: BlockPos = BlockPos(0, 0, 0)


        /** How many blocks the scene is drawn below the box's middle. */
        const val SCENE_DROP: Double = 0.75

        /** How much taller than its block an armour stand is measured as. */
        const val STAND_HEIGHT: Double = 1.2

        /** How much of the box the crop's widest extent is given; the rest is air around it. */
        const val VIEW_MARGIN: Double = 1.4

        /** The screen height, in percent, left above and below the preview box. */
        const val PREVIEW_MARGIN_PERCENT: Int = 8

        /** The same breathing room the greenhouse screen gives its grid inside the backdrop. */
        const val BORDER_PAD: Int = 6

        /** Where the slider's track sits inside the box: from its left and right edges, and under the label. */
        const val SLIDER_LEFT_INSET: Int = 10
        const val SLIDER_RIGHT_INSET: Int = 26
        const val SLIDER_TOP_GAP: Int = 8

        const val SELECTOR_HEIGHT: Int = 22
        const val SELECTOR_MIN_WIDTH: Int = 80

        /** How many list rows the picker's dropdown stops short of the bottom, to keep off the chat. */
        const val LIST_ROWS_ABOVE_CHAT: Int = 6

        /** Degrees of yaw a tick of the idle spin adds. */
        const val SPIN_DEGREES_PER_TICK: Float = 1.2f

        /** Degrees of yaw and pitch a pixel of dragging adds, and how far the pitch may go. */
        const val DRAG_YAW_PER_PIXEL: Float = 0.8f
        const val DRAG_PITCH_PER_PIXEL: Float = 0.5f
        const val MIN_PITCH: Float = -75f
        const val MAX_PITCH: Float = 30f

        /** How many times its size the question mark for an unrecorded stage is drawn at. */
        const val UNKNOWN_MARK_SCALE: Float = 4f

        const val BADGE_SIZE: Int = 16

        /** The badge is at the box's right edge, so the tooltip is pulled left to stay inside it. */
        const val TOOLTIP_WIDTH_HINT: Int = 170
    }
}
