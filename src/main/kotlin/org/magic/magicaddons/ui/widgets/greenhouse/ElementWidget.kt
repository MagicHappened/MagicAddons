package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.util.ScreenUtil.withAlpha
import org.magic.magicaddons.util.ScreenUtil.eased
import org.magic.magicaddons.util.ScreenUtil.modText
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import kotlin.math.absoluteValue
import org.magic.magicaddons.Common
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import org.magic.magicaddons.util.ScreenUtil.drawTooltipLines
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.WaterModel
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.fillCornerTriangle
import org.magic.magicaddons.util.ScreenUtil.fillRounded
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.renderFakeItem

class ElementWidget(val instance: GreenhouseElementInstance) : Renderable, GuiEventListener {
    var x: Int = 0
    var y: Int = 0
    var padding: Int = 0

    /** When this plant was dropped into its slot, for the pop it makes on arriving; zero for none. */
    var appearedAt: Long = 0L

    /** When the plant was last marked, for the flash; zero for one never marked while on screen. */
    var markedAt: Long = 0L

    /** Whether this plant stands in a plan rather than a greenhouse, so it has no stage or water. */
    var inPreset: Boolean = false
    var width = 0
    var height = 0

    var renderedStack: ItemStack = ItemStack.EMPTY

    /** The colour of the mark on this plant's slot, null for an unmarked one. */
    private val markingColor: Int? get() = instance.slot.slotMark?.color

    /** The water effects reaching this plant, set by whoever knows what stands beside it. */
    var waterEffect: Int = 0

    /** Where the debt mark sits on screen, so hovering it can explain itself. Null when none was drawn. */
    private var debtMarkBox: IntArray? = null

    /** Where the dead bush was drawn, so hovering it can explain itself. */
    private var deadMarkBox: IntArray? = null

    /** One fact about a plant, small enough to write over it. The colour is how the controls stand for it. */
    enum class HoverInfo(val color: Int, val label: String) {
        GrowthStage(0xFF3FBF3F.toInt(), "Growth stage"),
        WaterLevel(Common.UI.WATER_FULL_COLOR, "Water level"),
        DecayTime(Common.UI.WATER_DEBT_COLOR, "Decay time");

        /** This fact about [instance], or null while the game has not told us the value yet. */
        fun valueFor(instance: GreenhouseElementInstance): String? = when (this) {
            // a plant with one stage never grows, so there is no progress to report on it. Fire,
            // dead plants and the mutations placed by hand are all like this
            GrowthStage -> if (instance.finishedByPlacing) "Placed" else if (instance.fullyGrown) "Harvestable" else if (instance.cropDef.maxStage <= 1) null else
                when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> "${stage.stage}/${instance.cropDef.maxStage}"
                // a guessed stage is worth showing, as long as it does not look measured
                is GrowthStageInfo.Estimated -> "~${stage.range.first}-${stage.range.last}"
                null -> null
            }
            WaterLevel -> if (!instance.needsWater) null else instance.waterLevel?.let { "$it%" }
            DecayTime -> instance.decayRemainingMs?.let { readableDuration(it) }
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        markingColor?.let { color ->
            // right up against the grid lines, with no soil showing between
            graphics.drawBorder(x, y, x + width, y + height, Common.UI.BORDER_SIZE, color)

            // a tag folded over the top right corner, hard to miss at any slot size
            val tag = (width / 3).coerceAtLeast(5)
            graphics.fillCornerTriangle(x + width, y, tag, color)
        }
        if (instance.elementId == "Fire") {
            renderFire(graphics)
            return
        }
        // a plant just marked flashes white for a moment, so a stroke of marks reads one by one
        if (markedAt != 0L) {
            val faded = eased(markedAt, FLASH_MS)
            if (faded < 1f) graphics.fill(x, y, x + width, y + height, withAlpha(FLASH_COLOR, 1f - faded))
        }

        // a plant just dropped in grows from small to full over its first moments
        val elapsed = System.currentTimeMillis() - appearedAt
        val scale = if (appearedAt == 0L || elapsed >= POP_MS) 1f else POP_FROM + (1f - POP_FROM) * elapsed / POP_MS

        graphics.pose().pushMatrix()
        graphics.pose().translate(x + width / 2f, y + height / 2f)
        graphics.pose().scale(scale, scale)
        graphics.pose().translate(-(x + width / 2f), -(y + height / 2f))
        renderCrops(graphics)
        graphics.pose().popMatrix()

        deadMarkBox = null

        // the worst case has this plant dead already, and only a scan can settle it: it either finds
        // a dead bush or finds the plant standing, one tick from death
        if (instance.needsWater && (instance.waterLevel ?: 0) <= WaterModel.DEATH) {
            // a third of a single slot, half a slot on anything wider
            val footprint = instance.cropDef.footprint
            val size = (if (footprint.width > 1) width / footprint.width / 2 else width / 3).coerceAtLeast(8)
            val markX = x + width - size
            val markY = y

            graphics.fill(markX, markY, markX + size, markY + size, DEAD_MARK_BACKGROUND)
            graphics.renderFakeItem(DEAD_MARK, markX, markY, size, size)
            deadMarkBox = intArrayOf(markX, markY, markX + size, markY + size)
        }
    }

    /** The crop, or for a merged slot two split across a diagonal, more taking turns. */
    private fun renderCrops(graphics: GuiGraphicsExtractor) {
        val others = alternativeStacks
        val inner = width - padding * 2

        when (others.size) {
            0 -> graphics.renderFakeItem(renderedStack, x + padding, y + padding, inner, height - padding * 2)
            1 -> {
                val half = (inner * SPLIT_SHARE).toInt().coerceAtLeast(4)
                graphics.renderFakeItem(renderedStack, x + padding, y + padding, half, half)
                graphics.renderFakeItem(others[0], x + width - padding - half, y + height - padding - half, half, half)
                renderDiagonal(graphics)
            }
            else -> {
                val shown = ((System.currentTimeMillis() / CYCLE_MS) % (others.size + 1)).toInt()
                val stack = if (shown == 0) renderedStack else others[shown - 1]
                graphics.renderFakeItem(stack, x + padding, y + padding, inner, height - padding * 2)
            }
        }
    }

    /** a diagonal from the bottom left corner to the top right */
    private fun renderDiagonal(graphics: GuiGraphicsExtractor) {
        val inset = padding
        val span = width - inset * 2
        for (step in 0 until span) {
            val lineX = x + inset + step
            val lineY = y + height - inset - 1 - step * (height - inset * 2) / span
            graphics.fill(lineX, lineY, lineX + 1, lineY + DIAGONAL_WIDTH, Common.UI.TEXT_COLOR)
        }
    }

    /** icons of the crops merged into this slot */
    private val alternativeStacks: List<ItemStack> by lazy { instance.alternatives.map { ScreenUtil.stackFor(it) } }

    private fun renderFire(graphics: GuiGraphicsExtractor) {
        val sprite = FIRE_SPRITE ?: return
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height)
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
        val textHeight = font.lineHeight * INFO_TEXT_SCALE

        drawScaledLabel(graphics, text, y + height - textHeight - 1f, Common.UI.OVERLAY_TEXT_COLOR)
    }

    /**
     * [text] at [INFO_TEXT_SCALE] on a dark ground, centred across the plant with its top at
     * [top]. Returns the box the text took: left, top, right, bottom.
     */
    private fun drawScaledLabel(graphics: GuiGraphicsExtractor, text: String, top: Float, color: Int): IntArray {
        val font = Minecraft.getInstance().font

        // a hundred slots share the grid, full size text would not fit inside one of them
        val textWidth = font.width(text) * INFO_TEXT_SCALE
        val textHeight = font.lineHeight * INFO_TEXT_SCALE
        val textX = x + (width - textWidth) / 2f

        // the plant behind it is busy, the text needs its own ground to stay readable
        graphics.fill(
            (textX - 1f).toInt(),
            (top - 1f).toInt(),
            (textX + textWidth + 1f).toInt(),
            (top + textHeight).toInt(),
            Common.UI.OVERLAY_BACKGROUND_COLOR
        )

        val pose = graphics.pose()
        pose.pushMatrix()

        try {
            pose.translate(textX, top)
            pose.scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE)
            graphics.modText(font, text, 0, 0, color)
        } finally {
            pose.popMatrix()
        }

        return intArrayOf(textX.toInt(), top.toInt(), (textX + textWidth).toInt(), (top + textHeight).toInt())
    }

    /**
     * The water meter: a positive level fills from the left in blue, a negative one from the right
     * in red, as the game's own bar does.
     */
    private fun renderWaterBar(graphics: GuiGraphicsExtractor, waterLevel: Int) {
        val barWidth = width - WATER_BAR_INSET * 2
        if (barWidth < WATER_BAR_MIN_WIDTH) return

        val left = x + WATER_BAR_INSET
        val right = left + barWidth
        val bottom = y + height - WATER_BAR_INSET
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

        val outlasts = instance.outlastsGrowth(waterEffect)
        val tickMs = GreenhouseData.currentGrowthTickMs()

        val text: String
        val color: Int

        if (ticksLeft == null || outlasts == null || tickMs == null || remainingMs == null) {
            text = "?"
            color = Common.UI.TEXT_COLOR
        } else {
            text = readableDuration(remainingMs + (ticksLeft - 1) * tickMs) +
                    if (instance.waterPredictedInDebt) DEBT_MARK else ""
            color = if (outlasts) Common.UI.SUCCESS_COLOR else Common.UI.DANGER_COLOR
        }

        val font = Minecraft.getInstance().font
        val textHeight = font.lineHeight * INFO_TEXT_SCALE
        val box = drawScaledLabel(graphics, text, barTop - textHeight - 1f, color)

        if (instance.waterPredictedInDebt) {
            val markWidth = (font.width(DEBT_MARK) * INFO_TEXT_SCALE).toInt()
            debtMarkBox = intArrayOf(box[2] - markWidth, box[1], box[2], box[3])
        }
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = inRect(mouseX, mouseY, x, y, width, height)

    /** A plant is hovered, never focused; the listener interface still asks. */
    override fun isFocused(): Boolean = false

    override fun setFocused(focused: Boolean) {}

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
        val cropDefinition = instance.cropDef

        val lines = buildList {
            add(Component.literal(instance.everyCrop.joinToString(" / ") { it.name }).withStyle(ChatFormatting.GREEN))

            instance.slot.slotMark?.let { marking ->
                add(labelled("Role", marking.name))
            }

            if (instance.merged) {
                add(Component.literal("Possible targets:").withStyle(ChatFormatting.GRAY))
                instance.everyCrop.forEach { add(Component.literal(" - ${it.name}").withStyle(ChatFormatting.WHITE)) }
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
                    instance.fullyGrown -> add(labelled("Growth", "Harvestable"))
                    else -> growthText?.let { add(labelled("Growth", it)) }
                }

                // a plant that never drinks has no water level worth a line of its own
                if (instance.needsWater) {
                    add(labelled("Water", instance.waterLevel?.let { "$it%" } ?: "Unknown"))
                }

                instance.decayRemainingMs?.let { add(labelled("Decays in", readableDuration(it))) }
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

        /** Appended to a water time that assumes no skipped ticks. */
        private const val DEBT_MARK: String = "*"

        /** What the mark means, said in full rather than left as a symbol nobody can look up. */
        private val DEBT_EXPLANATION: String = """
            When the plant's water is negative, it has a chance to skip ticks entirely,
            therefore not draining water. This estimate assumes it never skips ticks,
            so your plants don't die.
        """.trimIndent()

        /** Worn in the corner of a plant the worst case has already killed. */
        private val DEAD_MARK: ItemStack = ItemStack(Items.DEAD_BUSH)

        /** Behind the bush, so a slot that might already be dead reads as such at a glance. */
        private const val DEAD_MARK_BACKGROUND: Int = 0xC0201010.toInt()

        private val DEAD_EXPLANATION: String = """
            In the worst case scenario this plant is dead.
            Enter the greenhouse to verify.
        """.trimIndent()

        /** share of the cell each of two merged crops takes */
        private const val SPLIT_SHARE: Float = 0.62f

        private const val DIAGONAL_WIDTH: Int = 2

        /** how long each crop of a slot merged three or more ways is shown */
        private const val CYCLE_MS: Long = 900

        /** The pop on arriving: how small it starts and how long it takes. */
        private const val POP_MS: Long = 150

        /** The flash on a plant just marked: how long it lasts, and the white it starts from. */
        private const val FLASH_MS: Long = 250
        private const val FLASH_COLOR: Int = 0xA0FFFFFF.toInt()
        private const val POP_FROM: Float = 0.5f

        /** Resolved once: only the fire element draws one, and resolving can throw. */
        private val FIRE_SPRITE: TextureAtlasSprite? by lazy {
            runCatching {
                ScreenUtil.getSpriteForState(Blocks.FIRE.defaultBlockState(), Direction.NORTH)
            }.getOrNull()
        }

        private fun labelled(label: String, value: String): Component =
            Component.literal("$label: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE))

        /** Cuts off rather than rounds, and drops the minutes past six hours; Long.toShortDuration keeps them. */
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
