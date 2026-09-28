package org.magic.magicaddons.ui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.render.CropPreviewRenderState
import org.magic.magicaddons.render.StandInScene
import org.magic.magicaddons.ui.OverlayContext
import org.magic.magicaddons.ui.OverlayRenderable
import org.magic.magicaddons.ui.widgets.DropdownWidget
import org.magic.magicaddons.ui.widgets.SliderWidget
import org.magic.magicaddons.util.ScreenUtil.drawCenteredTextBox
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.compat.McCompat

class CropPreviewScreen(
    private val parent: Screen?,
    private val initialCrop: CropDefinition? = null
) : MagicAddonsScreen(Component.literal("Crop Preview"), "the crop preview"), OverlayContext {

    override val overlays: MutableList<OverlayRenderable> = mutableListOf()

    override val backgroundImageName: String = Customization.PREVIEW_SCREEN

    private var selectedCrop: CropDefinition? = null
    private var shownStage: Int = 1

    private var shownCropStage: CropStage? = null
    private var shownCrop: CropStage.HologramStage? = null

    private var soilBlocks: Map<BlockPos, BlockState> = emptyMap()

    private var sceneMinY = 0.0
    private var sceneMaxY = 1.0

    private var yaw: Float = 45f
    private var pitch: Float = -20f

    private var isDraggingView = false

    private var isSpinning = true

    enum class Variant(private val label: String) {
        Day("Day"), Night("Night"), Awake("Awake"), Asleep("Asleep");

        override fun toString(): String = label
    }

    private val variantSelector = DropdownWidget(
        values = emptyList<Variant>(),
        currentValue = null as Variant?,
        overlayContext = this,
        isSearchable = false,
        onValueChanged = { rebuildScene() }
    )

    private val shownVariant: Variant? get() = variantSelector.currentValue

    private fun variantsFor(def: CropDefinition): List<Variant> = when {
        def.stages.any { StandReader.NEEDS_TIME in it.traits } -> listOf(Variant.Day, Variant.Night)
        def.sleepStages.isNotEmpty() -> listOf(Variant.Awake, Variant.Asleep)
        else -> emptyList()
    }

    private fun CropStage.matchesVariant(variant: Variant?, def: CropDefinition, stage: Int): Boolean = when (variant) {
        Variant.Day -> traits[StandReader.NEEDS_TIME] == StandReader.NEEDS_DAY
        Variant.Night -> traits[StandReader.NEEDS_TIME] == StandReader.NEEDS_NIGHT
        Variant.Asleep -> if (stage in def.sleepStages) readers.any { it.key == StandReader.ASLEEP } else readers.none { it.key == StandReader.ASLEEP }
        Variant.Awake -> readers.none { it.key == StandReader.ASLEEP }
        null -> true
    }

    private val cropSelector = DropdownWidget(
        values = CropRegistry.allCrops.sortedBy { it.name },
        currentValue = null as CropDefinition?,
        overlayContext = this,
        onValueChanged = { selectCrop(it) }
    )

    private var previewX = 0
    private var previewY = 0
    private var previewSize = 0

    private val stageSlider = SliderWidget { showStage(it) }

    override fun onInit() {
        super.onInit()
        if (selectedCrop == null && initialCrop != null) {
            cropSelector.currentValue = initialCrop
            selectCrop(initialCrop)
        }

        previewY = height * PREVIEW_MARGIN_PERCENT / 100
        previewSize = height - previewY * 2
        previewX = (width - previewSize) / 2

        stageSlider.x = previewX + SLIDER_SIDE_INSET
        stageSlider.width = previewX + previewSize - SLIDER_SIDE_INSET - stageSlider.x
        stageSlider.y = previewY + font.lineHeight + SLIDER_TOP_GAP

        cropSelector.height = SELECTOR_HEIGHT
        cropSelector.fitToValues((previewX - Common.UI.SPACING_LARGE * 2).coerceAtLeast(SELECTOR_MIN_WIDTH))
        cropSelector.x = Common.UI.SPACING_LARGE
        cropSelector.y = previewY

        cropSelector.overlayBudget = height - (cropSelector.y + cropSelector.height) - cropSelector.height * LIST_ROWS_ABOVE_CHAT

        variantSelector.height = cropSelector.height
        variantSelector.width = cropSelector.width
        variantSelector.x = cropSelector.x
        variantSelector.y = cropSelector.y + cropSelector.height + Common.UI.SPACING
    }

    private fun selectCrop(def: CropDefinition) {
        selectedCrop = def
        isSpinning = true
        variantSelector.values = variantsFor(def)
        variantSelector.currentValue = variantSelector.values.firstOrNull()
        shownStage = shownStage.coerceIn(1, def.maxStage)
        stageSlider.setRange(1, def.maxStage)
        stageSlider.setValueWithoutNotifying(shownStage)
        measureSceneHeight(def)
        rebuildScene()
    }

    private fun measureSceneHeight(def: CropDefinition) {
        sceneMinY = 0.0
        sceneMaxY = 1.0
        val level = Minecraft.getInstance().level ?: return

        def.stages
            .mapNotNull { it.hologramStageAt(level, ORIGIN, def) }
            .forEach { hologram ->
                hologram.blockMap.keys.forEach {
                    sceneMinY = minOf(sceneMinY, it.y.toDouble())
                    sceneMaxY = maxOf(sceneMaxY, it.y + 1.0)
                }
                hologram.stands.forEach {
                    sceneMinY = minOf(sceneMinY, it.y)
                    sceneMaxY = maxOf(sceneMaxY, it.y + STAND_HEIGHT)
                }
            }
    }

    private fun showStage(newStage: Int) {
        val def = selectedCrop ?: return
        val clampedStage = newStage.coerceIn(1, def.maxStage)

        if (clampedStage == shownStage) return

        stageSlider.setValueWithoutNotifying(clampedStage)

        val shownLook = shownCropStage
        if (shownLook != null && clampedStage in shownLook.stageRange && shownLook.matchesVariant(shownVariant, def, clampedStage)) {
            shownStage = clampedStage
            return
        }

        shownStage = clampedStage
        rebuildScene()
    }

    private fun rebuildScene() {
        shownCropStage = null
        shownCrop = null
        soilBlocks = emptyMap()

        val def = selectedCrop ?: return
        val level = Minecraft.getInstance().level ?: return

        val stageDef = def.stages
            .firstOrNull { shownStage in it.stageRange && it.matchesVariant(shownVariant, def, shownStage) }
            ?: return

        shownCropStage = stageDef
        shownCrop = stageDef.hologramStageAt(level, ORIGIN, def)

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

    private fun sceneCenterAndExtent(): Pair<Vec3, Double> {
        val minY = sceneMinY
        val maxY = sceneMaxY

        val footprint = selectedCrop?.footprint
        val footprintWidth = footprint?.width ?: 1
        val footprintDepth = footprint?.height ?: 1

        val center = Vec3(ORIGIN.x + footprintWidth / 2.0, (minY + maxY) / 2.0 + SCENE_DROP, ORIGIN.z + footprintDepth / 2.0)
        val extent = maxOf(maxY - minY, maxOf(footprintWidth, footprintDepth).toDouble(), 2.0)

        return center to extent
    }

    override fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val def = selectedCrop

        if (isSpinning && !isDraggingView) yaw = (yaw + delta * SPIN_DEGREES_PER_TICK) % 360f

        graphics.drawPanel(
            previewX - BORDER_PAD,
            previewY - BORDER_PAD,
            previewX + previewSize + BORDER_PAD,
            previewY + previewSize + BORDER_PAD
        )

        when {
            def == null -> graphics.drawCenteredTextBox(
                "Pick a crop",
                previewX + previewSize / 2,
                previewY + previewSize / 2
            )

            shownCropStage == null || shownCrop == null -> drawUnrecordedStageMark(graphics)

            else -> submitCropScene(graphics, delta)
        }

        if (def != null) drawSlider(graphics, def, mouseX, mouseY)

        cropSelector.extractRenderState(graphics, mouseX, mouseY, delta)
        if (variantSelector.values.isNotEmpty()) variantSelector.extractRenderState(graphics, mouseX, mouseY, delta)

        renderOverlays(graphics, mouseX, mouseY, delta)
    }

    private fun submitCropScene(graphics: GuiGraphicsExtractor, delta: Float) {
        val hologram = shownCrop ?: return
        val (center, extent) = sceneCenterAndExtent()

        val dispatcher = Minecraft.getInstance().entityRenderDispatcher

        val stands = hologram.stands.map { stand ->
            val state = dispatcher.extractEntity(stand, delta)

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
                blocks = soilBlocks + hologram.blockMap,
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

    private fun drawUnrecordedStageMark(graphics: GuiGraphicsExtractor) {
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

        stageSlider.render(graphics, mouseX, mouseY)

        val label = "Stage $shownStage / ${def.maxStage}"

        graphics.text(
            font,
            Component.literal(label),
            previewX + (previewSize - font.width(label)) / 2,
            stageSlider.y - font.lineHeight - 2,
            Common.UI.TEXT_COLOR,
            false
        )
    }

    override fun onMouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (overlaysMouseClicked(event, doubled)) return true

        if (cropSelector.mouseClicked(event, doubled)) return true
        if (variantSelector.values.isNotEmpty() && variantSelector.mouseClicked(event, doubled)) return true

        closeOverlays()

        val clickX = event.x.toInt()
        val clickY = event.y.toInt()
        val def = selectedCrop

        if (def != null && def.maxStage > 1 && stageSlider.mouseClicked(event.x, event.y)) return true

        if (event.button() == 0 &&
            clickX in previewX..previewX + previewSize && clickY in previewY..previewY + previewSize
        ) {
            isDraggingView = true
            return true
        }

        return super.onMouseClicked(event, doubled)
    }

    override fun onMouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (stageSlider.mouseDragged(event.x)) return true

        if (isDraggingView) {
            isSpinning = false
            yaw = (yaw + dragX.toFloat() * DRAG_YAW_PER_PIXEL) % 360f
            pitch = (pitch - dragY.toFloat() * DRAG_PITCH_PER_PIXEL).coerceIn(MIN_PITCH, MAX_PITCH)
            return true
        }

        return super.onMouseDragged(event, dragX, dragY)
    }

    override fun onMouseReleased(event: MouseButtonEvent): Boolean {
        stageSlider.mouseReleased()
        isDraggingView = false

        return super.onMouseReleased(event)
    }

    override fun onMouseMoved(mouseX: Double, mouseY: Double) {
        overlaysMouseMoved(mouseX, mouseY)
        cropSelector.mouseMoved(mouseX, mouseY)
        variantSelector.mouseMoved(mouseX, mouseY)
    }

    override fun onMouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        overlaysMouseScrolled(mouseX, mouseY, scrollX, scrollY) || super.onMouseScrolled(mouseX, mouseY, scrollX, scrollY)

    override fun onCharTyped(event: CharacterEvent): Boolean =
        overlaysCharTyped(event) || super.onCharTyped(event)

    override fun onKeyPressed(event: KeyEvent): Boolean =
        overlaysKeyPressed(event) || super.onKeyPressed(event)

    override fun onCloseFinished() {
        McCompat.setScreen(parent)
    }

    private companion object {
        val ORIGIN: BlockPos = BlockPos(0, 0, 0)

        const val SCENE_DROP: Double = 0.75

        const val STAND_HEIGHT: Double = 1.2

        const val VIEW_MARGIN: Double = 1.4

        const val PREVIEW_MARGIN_PERCENT: Int = 8

        const val BORDER_PAD: Int = 6

        const val SLIDER_SIDE_INSET: Int = 10
        const val SLIDER_TOP_GAP: Int = 8

        const val SELECTOR_HEIGHT: Int = 22
        const val SELECTOR_MIN_WIDTH: Int = 80

        const val LIST_ROWS_ABOVE_CHAT: Int = 6

        const val SPIN_DEGREES_PER_TICK: Float = 1.2f

        const val DRAG_YAW_PER_PIXEL: Float = 0.8f
        const val DRAG_PITCH_PER_PIXEL: Float = 0.5f
        const val MIN_PITCH: Float = -75f
        const val MAX_PITCH: Float = 30f

        const val UNKNOWN_MARK_SCALE: Float = 4f
    }
}
