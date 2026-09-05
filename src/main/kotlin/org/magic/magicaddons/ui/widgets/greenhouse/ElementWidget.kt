package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import kotlin.math.absoluteValue
import org.magic.magicaddons.Common
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import org.magic.magicaddons.util.ScreenUtil.drawTooltipLines
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.data.greenhouse.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.WaterModel
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.fillRounded
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem

class ElementWidget(val instance: GreenhouseElementInstance) : Renderable, Focusable {
    var widgetX: Int = 0
    var widgetY: Int = 0
    var padding: Int = 0

    /** When this plant was dropped into its slot, for the pop it makes on arriving; zero for none. */
    var appearedAt: Long = 0L

    /** Whether this plant stands in a plan rather than a greenhouse, so it has no stage or water. */
    var inPreset: Boolean = false
    var width = 50
    var height = 50
    /** Resolved on demand: only the fire element draws one, and resolving can throw. */
    val sprite: TextureAtlasSprite? by lazy {
        runCatching {
            ScreenUtil.getSpriteForState(Blocks.FIRE.defaultBlockState(), Direction.NORTH)
        }.getOrNull()
    }
    var renderedStack: ItemStack = ItemStack.EMPTY
    var markingColor: Int? = null

    /** The water effects reaching this plant, set by whoever knows what stands beside it. */
    var waterEffect: Int = 0

    /** What a time worked out from a plant that may have been passed over is marked with. */
    private val DEBT_MARK: String = "*"

    /** Where that mark sits on screen, so hovering it can explain itself. Null when none was drawn. */
    private var debtMarkBox: IntArray? = null

    /** What the mark means, said in full rather than left as a symbol nobody can look up. */
    private val DEBT_EXPLANATION: String = """
        When the plant's water is negative, it has a chance to skip ticks entirely,
        therefore not draining water. This estimate assumes it never skips ticks,
        so your plants don't die.
    """.trimIndent()

    /** Worn in the corner of a plant the worst case has already killed. */
    private val DEAD_MARK: ItemStack = ItemStack(Items.DEAD_BUSH)

    /** The pop on arriving: how small it starts and how long it takes. */
    private val POP_MS: Long = 150
    private val POP_FROM: Float = 0.5f

    /** Behind the bush, so a slot that might already be dead reads as such at a glance. */
    private val DEAD_MARK_BACKGROUND: Int = 0xC0201010.toInt()

    /** Where the dead bush was drawn, so hovering it can explain itself. */
    private var deadMarkBox: IntArray? = null

    private val DEAD_EXPLANATION: String = """
        In the worst case scenario this plant is dead.
        Enter the greenhouse to verify.
    """.trimIndent()
    override var focusedState: Boolean = false

    /** One fact about a plant, small enough to write over it. The colour is how the controls stand for it. */
    enum class HoverInfo(val color: Int, val label: String) {
        GrowthStage(0xFF3FBF3F.toInt(), "Growth stage"),
        WaterLevel(0xFF3F7FDF.toInt(), "Water level"),
        DecayTime(0xFFCC3333.toInt(), "Decay time");

        /** This fact about [instance], or null while the game has not told us the value yet. */
        fun valueFor(instance: GreenhouseElementInstance): String? = when (this) {
            // a plant with one stage never grows, so there is no progress to report on it. Fire,
            // dead plants and the mutations placed by hand are all like this
            GrowthStage -> if (instance.finishedByPlacing) "Placed" else if (instance.fullyGrown) "Fully grown" else if (instance.cropDef.maxStage <= 1) null else
                when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> "${stage.stage}/${instance.cropDef.maxStage}"
                // a guessed stage is worth showing, as long as it does not look measured
                is GrowthStageInfo.Estimated -> "~${stage.range.first}-${stage.range.last}"
                null -> null
            }
            WaterLevel -> if (!instance.needsWater) null else instance.waterLevel?.let { "$it%" }
            DecayTime -> decayRemainingMs(instance)?.let { readableDuration(it) }
        }
    }

    fun init(){
        markingColor = instance.slot.slotMark?.color
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        markingColor?.let { colour ->
            // right up against the grid lines, with no soil showing between
            graphics.drawBorder(widgetX, widgetY, widgetX + width, widgetY + height, Common.UI.BORDER_SIZE, colour)

            // a tag folded over the top right corner, hard to miss at any slot size
            val tag = (width / 3).coerceAtLeast(5)
            for (row in 0 until tag) {
                graphics.fill(widgetX + width - tag + row, widgetY + row, widgetX + width, widgetY + row + 1, colour)
            }
        }
        if (instance.elementId == "Fire") {
            renderFire(graphics)
            return
        }
        // a plant just dropped in grows from small to full over its first moments
        val elapsed = System.currentTimeMillis() - appearedAt
        val scale = if (appearedAt == 0L || elapsed >= POP_MS) 1f else POP_FROM + (1f - POP_FROM) * elapsed / POP_MS

        graphics.pose().pushMatrix()
        graphics.pose().translate(widgetX + width / 2f, widgetY + height / 2f)
        graphics.pose().scale(scale, scale)
        graphics.pose().translate(-(widgetX + width / 2f), -(widgetY + height / 2f))
        graphics.renderFakeItem(
            renderedStack,
            widgetX + padding,
            widgetY + padding,
            width - padding * 2,
            height - padding * 2
        )
        graphics.pose().popMatrix()

        deadMarkBox = null

        // the worst case has this plant dead already, and only a scan can settle it: it either finds
        // a dead bush or finds the plant standing, one tick from death
        if (instance.needsWater && (instance.waterLevel ?: 0) <= WaterModel.DEATH) {
            // a third of a single slot, half a slot on anything wider
            val footprint = instance.cropDef.footprint
            val size = (if (footprint.width > 1) width / footprint.width / 2 else width / 3).coerceAtLeast(8)
            val markX = widgetX + width - size
            val markY = widgetY

            graphics.fill(markX, markY, markX + size, markY + size, DEAD_MARK_BACKGROUND)
            graphics.renderFakeItem(DEAD_MARK, markX, markY, size, size)
            deadMarkBox = intArrayOf(markX, markY, markX + size, markY + size)
        }
    }

    fun renderFire(graphics: GuiGraphicsExtractor){
        val sprite = sprite ?: return
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            widgetX,
            widgetY,
            width,
            height
        )
    }

    /** Writes the pinned fact over the plant. Nothing is drawn while that fact is unknown. */
    fun renderHoverButtonInfo(graphics: GuiGraphicsExtractor, info: HoverInfo) {
        // water is a level, and a meter says that faster than a number. A plant that never drinks is
        // left alone rather than shown an empty meter
        if (info == HoverInfo.WaterLevel) {
            if (!instance.needsWater) return

            instance.waterLevel?.let {
                renderWaterBar(graphics, it.coerceAtLeast(WaterModel.DEATH))
            }
            return
        }

        val text = info.valueFor(instance) ?: return
        val font = Minecraft.getInstance().font

        // a hundred slots share the grid, full size text would not fit inside one of them
        val textWidth = font.width(text) * INFO_TEXT_SCALE
        val textHeight = font.lineHeight * INFO_TEXT_SCALE
        val textX = widgetX + (width - textWidth) / 2f
        val textY = widgetY + height - textHeight - 1f

        // the plant behind it is busy, the text needs its own ground to stay readable
        graphics.fill(
            (textX - 1f).toInt(),
            (textY - 1f).toInt(),
            (textX + textWidth + 1f).toInt(),
            (textY + textHeight).toInt(),
            Common.UI.OVERLAY_BACKGROUND_COLOR
        )

        val pose = graphics.pose()
        pose.pushMatrix()

        try {
            pose.translate(textX, textY)
            pose.scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE)
            graphics.text(font, text, 0, 0, Common.UI.OVERLAY_TEXT_COLOR, false)
        } finally {
            pose.popMatrix()
        }
    }

    /**
     * The water meter: a positive level fills from the left in blue, a negative one from the right
     * in red, as the game's own bar does.
     */
    private fun renderWaterBar(graphics: GuiGraphicsExtractor, waterLevel: Int) {
        val barWidth = width - WATER_BAR_INSET * 2
        if (barWidth < WATER_BAR_MIN_WIDTH) return

        val left = widgetX + WATER_BAR_INSET
        val right = left + barWidth
        val bottom = widgetY + height - WATER_BAR_INSET
        val top = bottom - WATER_BAR_HEIGHT

        renderWaterVerdict(graphics, waterLevel, top)

        graphics.fillRounded(left, top, right, bottom, WATER_BAR_RADIUS, Common.UI.WATER_TRACK_COLOR)

        val filled = barWidth * waterLevel.absoluteValue.coerceAtMost(100) / 100
        if (filled <= 0) return

        if (waterLevel >= 0) {
            graphics.fillRounded(
                left,
                top,
                left + filled,
                bottom,
                WATER_BAR_RADIUS,
                Common.UI.WATER_FULL_COLOR
            )
        } else {
            graphics.fillRounded(
                right - filled,
                top,
                right,
                bottom,
                WATER_BAR_RADIUS,
                Common.UI.WATER_DEBT_COLOR
            )
        }
    }

    /**
     * Ticks of water left, above the meter. Green when that sees the plant to its last stage, red
     * when it runs dry first, white when something is unknown. Judged by the lowest stage it might be at.
     */
    private fun renderWaterVerdict(graphics: GuiGraphicsExtractor, waterLevel: Int, barTop: Int) {
        debtMarkBox = null

        // past death in the estimate there is no time left to state; the dead bush says it instead
        if (waterLevel <= WaterModel.DEATH) return

        val ticksLeft = WaterModel.ticksUntilDeath(waterLevel, waterEffect)
        val remainingMs = GreenhouseData.remainingTickMs()

        val stage = when (val known = instance.growthStage) {
            is GrowthStageInfo.Known -> known.stage
            is GrowthStageInfo.Estimated -> known.range.first
            null -> null
        }

        // nothing to outlast when the plant is already at its only stage
        val ticksNeeded = stage
            ?.takeIf { instance.cropDef.maxStage > 1 }
            ?.let { instance.cropDef.maxStage - it }
        val tickMs = GreenhouseData.currentGrowthTickMs()

        val text: String
        val color: Int

        if (ticksLeft == null || ticksNeeded == null || tickMs == null || remainingMs == null) {
            text = "?"
            color = Common.UI.TEXT_COLOR
        } else {
            text = readableDuration(remainingMs + (ticksLeft - 1) * tickMs) +
                    if (instance.waterPredictedInDebt) DEBT_MARK else ""
            color = if (ticksLeft > ticksNeeded) Common.UI.SUCCESS_COLOR else Common.UI.DANGER_COLOR
        }

        val font = Minecraft.getInstance().font
        val textWidth = font.width(text) * INFO_TEXT_SCALE
        val textHeight = font.lineHeight * INFO_TEXT_SCALE
        val textX = widgetX + (width - textWidth) / 2f
        val textY = barTop - textHeight - 1f

        graphics.fill(
            (textX - 1f).toInt(),
            (textY - 1f).toInt(),
            (textX + textWidth + 1f).toInt(),
            (textY + textHeight).toInt(),
            Common.UI.OVERLAY_BACKGROUND_COLOR
        )

        val pose = graphics.pose()
        pose.pushMatrix()

        try {
            pose.translate(textX, textY)
            pose.scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE)
            graphics.text(font, text, 0, 0, color, false)

            if (instance.waterPredictedInDebt) {
                val markWidth = font.width(DEBT_MARK) * INFO_TEXT_SCALE

                debtMarkBox = intArrayOf(
                    (textX + textWidth - markWidth).toInt(),
                    textY.toInt(),
                    (textX + textWidth).toInt(),
                    (textY + textHeight).toInt()
                )
            }
        } finally {
            pose.popMatrix()
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        return isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in widgetX until widgetX + width &&
                mouseY.toInt() in widgetY until widgetY + height
    }

    /** The dead bush's own tooltip, when the mouse is on it rather than on the plant. */
    fun deadTooltipAt(mouseX: Int, mouseY: Int): String? {
        val box = deadMarkBox ?: return null

        return DEAD_EXPLANATION.takeIf {
            mouseX in box[0]..box[2] && mouseY in box[1]..box[3]
        }
    }

    /** The star's own tooltip, when the mouse is on the star rather than the plant. */
    fun debtTooltipAt(mouseX: Int, mouseY: Int): String? {
        val box = debtMarkBox ?: return null

        return DEBT_EXPLANATION.takeIf {
            mouseX in box[0]..box[2] && mouseY in box[1]..box[3]
        }
    }

    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font
        val cropDefinition = instance.cropDef

        val lines = buildList {
            add(Component.literal(cropDefinition.name).withStyle(ChatFormatting.GREEN))

            instance.slot.slotMark?.let { marking ->
                add(labelled("Role", marking.name))
            }

            val growthText = when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known ->
                    "${stage.stage}/${cropDefinition.maxStage}"

                is GrowthStageInfo.Estimated ->
                    "${stage.range.first}-${stage.range.last}/${cropDefinition.maxStage} (estimated)"

                null -> null
            }

            if (!inPreset) {
                when {
                    instance.finishedByPlacing -> add(labelled("Growth", "Placed"))
                    instance.fullyGrown -> add(labelled("Growth", "Fully grown"))
                    else -> growthText?.let { add(labelled("Growth", it)) }
                }

                // a plant that never drinks has no water level worth a line of its own
                if (instance.needsWater) {
                    add(labelled("Water", instance.waterLevel?.let { "$it%" } ?: "Unknown"))
                }

                decayRemainingMs(instance)?.let { add(labelled("Decays in", readableDuration(it))) }
            }

            val footprint = cropDefinition.footprint
            if (footprint.width > 1 || footprint.height > 1) {
                add(labelled("Size", "${footprint.width}x${footprint.height}"))
            }

            if (inPreset) add(Component.literal("Right click to mark").withStyle(ChatFormatting.GRAY))
        }

        graphics.drawTooltipLines(lines.map { it.visualOrderText }, mouseX, mouseY)
    }




    companion object {
        /** A slot is small, but the numbers still have to be legible from across the grid. */
        private const val INFO_TEXT_SCALE: Float = 0.75f

        private const val WATER_BAR_HEIGHT: Int = 4
        private const val WATER_BAR_INSET: Int = 3
        private const val WATER_BAR_RADIUS: Int = 1

        /** Below this the meter is too short to read a level off, so nothing is drawn. */
        private const val WATER_BAR_MIN_WIDTH: Int = 8

        /** Past this, a duration is shown in whole hours. */
        private const val COARSE_AFTER_SECONDS: Long = 6 * 60 * 60

        private fun labelled(label: String, value: String): Component =
            Component.literal("$label: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE))

        /** Time before this plant decays, null when it never does or its age was never read. */
        private fun decayRemainingMs(instance: GreenhouseElementInstance): Long? {
            val decayTime = instance.cropDef.decayTimeMs
            if (decayTime == NEVER_DECAYS) return null

            val age = instance.age ?: return null

            return (decayTime - age).coerceAtLeast(0L)
        }

        /**
         * A duration cut off rather than rounded, so it never reads longer than the time left. Past
         * six hours the minutes are dropped.
         */
        private fun readableDuration(ms: Long): String {
            val seconds = ms / 1000

            val days = seconds / 86400
            val hours = seconds % 86400 / 3600
            val minutes = seconds % 3600 / 60

            return when {
                days > 0 -> "${days}d ${hours}h"
                seconds >= COARSE_AFTER_SECONDS -> "${hours}h"
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "${seconds}s"
            }
        }
    }
}