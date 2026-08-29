package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import kotlin.math.absoluteValue
import org.magic.magicaddons.Common
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
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

    /**
     * Where that mark is on screen, so hovering it can explain itself. Null whenever the last
     * drawing of this plant did not need one.
     */
    private var debtMarkBox: IntArray? = null

    /** What the mark means, said in full rather than left as a symbol nobody can look up. */
    private val DEBT_EXPLANATION: String = """
        A plant with negative water has a chance of being skipped when the greenhouse ticks:
        it neither grows a stage nor loses water that tick.
        Because that is a chance rather than a rule, this timer assumes it was never skipped,
        which is the soonest the plant could die rather than the likeliest.
        Water it with a watering can, or read it with the plant analyzer,
        to replace the guess with what the plant actually holds.
    """.trimIndent()
    override var focusedState: Boolean = false

    /**
     * A single fact about a plant, small enough to write over the plant itself. [color] is how the
     * hover controls stand for this fact, the swatches carry no text of their own.
     */
    enum class HoverInfo(val color: Int) {
        GrowthStage(0xFF3FBF3F.toInt()),
        WaterLevel(0xFF3F7FDF.toInt()),
        DecayTime(0xFFCC3333.toInt());

        /** This fact about [instance], or null while the game has not told us the value yet. */
        fun valueFor(instance: GreenhouseElementInstance): String? = when (this) {
            // a plant with one stage never grows, so there is no progress to report on it. Fire,
            // dead plants and the mutations placed by hand are all like this
            GrowthStage -> if (instance.cropDef.maxStage <= 1) null else
                when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> "${stage.stage}/${instance.cropDef.maxStage}"
                // a guessed stage is worth showing, as long as it does not look measured
                is GrowthStageInfo.Estimated -> "~${stage.range.first}-${stage.range.last}"
                null -> null
            }
            WaterLevel -> instance.waterLevel?.let { "$it%" }
            DecayTime -> decayRemainingMs(instance)?.let { readableDuration(it) }
        }
    }

    fun init(){
        markingColor = instance.slot.slotMark?.color
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        markingColor?.let {
            graphics.drawBorder(
                widgetX + 1,
                widgetY + 1,
                widgetX + width - 1,
                widgetY + height - 1,
                1,
                it
            )
        }
        if (instance.elementId == "Fire") {
            renderFire(graphics)
            return
        }
        graphics.renderFakeItem(
            renderedStack,
            widgetX + padding,
            widgetY + padding,
            width - padding * 2,
            height - padding * 2
        )
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

    /**
     * Writes [info] over the plant itself, for the one fact the player pinned to always be visible.
     * Nothing is drawn while that fact is unknown, an empty backdrop says less than the plant does.
     */
    fun renderHoverButtonInfo(graphics: GuiGraphicsExtractor, info: HoverInfo) {
        // water is a level rather than a reading, and a meter says that faster than a number does.
        // A plant that never drinks has neither, so it is left alone rather than drawn with an
        // empty meter that reads as a plant about to die of thirst
        if (info == HoverInfo.WaterLevel) {
            if (!instance.cropDef.needsWater) return

            instance.waterLevel?.let { renderWaterBar(graphics, it) }
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
     * The water meter, drawn along the bottom of the plant.
     *
     * A plant holds between -100 and 100 water. What it has fills from the left in blue, what it
     * owes fills from the right in red, which is the way the game words its own bar: watered plants
     * fill up from the front, dry ones lose ground from the back.
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
     * How many ticks of water the plant has left, written just above its meter.
     *
     * Green when that is enough to see it through to its last stage, red when it runs dry first,
     * and white when something needed to say either way is not known. The stage taken is the lowest
     * it might be at, so an estimate errs towards saying a plant is in trouble rather than towards
     * reassuring the player it is fine.
     */
    private fun renderWaterVerdict(graphics: GuiGraphicsExtractor, waterLevel: Int, barTop: Int) {
        debtMarkBox = null

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
        if (isMouseOver(mouseButtonEvent.x, mouseButtonEvent.y)) {
            ChatUtils.sendWithPrefix("Clicked on ${instance.elementId} ")
            return true
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toInt() in widgetX until widgetX + width &&
                mouseY.toInt() in widgetY until widgetY + height
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

            growthText?.let { add(labelled("Growth", it)) }

            // a plant that never drinks has no water level worth a line of its own
            if (cropDefinition.needsWater) {
                add(labelled("Water", instance.waterLevel?.let { "$it%" } ?: "Unknown"))
            }

            decayRemainingMs(instance)?.let { add(labelled("Decays in", readableDuration(it))) }

            val footprint = cropDefinition.footprint
            if (footprint.width > 1 || footprint.height > 1) {
                add(labelled("Size", "${footprint.width}x${footprint.height}"))
            }
        }

        val components = lines.map { ClientTooltipComponent.create(it.visualOrderText) }

        graphics.tooltip(
            font,
            components,
            mouseX,
            mouseY,
            DefaultTooltipPositioner.INSTANCE,
            null
        )
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

        /**
         * How long this plant has before it decays, or null when either the plant never decays or
         * its age was never read off a plant diagnostic.
         */
        private fun decayRemainingMs(instance: GreenhouseElementInstance): Long? {
            val decayTime = instance.cropDef.decayTimeMs ?: return null
            if (decayTime == NEVER_DECAYS) return null

            val age = instance.age ?: return null

            return (decayTime - age).coerceAtLeast(0L)
        }

        /**
         * [ms] as a duration, cut off rather than rounded, so a figure never reads longer than the
         * time actually left.
         *
         * Past six hours the minutes stop being worth the width they take and are dropped, so six
         * hours and fifty minutes reads as six hours. Below that they are kept, since twenty
         * minutes either way matters when a plant is nearly dry.
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