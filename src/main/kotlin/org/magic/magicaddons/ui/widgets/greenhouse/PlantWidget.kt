package org.magic.magicaddons.ui.widgets.greenhouse

import kotlin.math.absoluteValue
import kotlin.math.ceil
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.ChargeRule
import org.magic.magicaddons.data.greenhouse.crops.DecayOutlook
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.PlantStage
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.FireElement
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime
import org.magic.magicaddons.ui.ScreenRect
import org.magic.magicaddons.util.ScreenUtil
import org.magic.magicaddons.util.ScreenUtil.drawBorder
import org.magic.magicaddons.util.ScreenUtil.drawCountedCrop
import org.magic.magicaddons.util.ScreenUtil.drawTooltipLines
import org.magic.magicaddons.util.ScreenUtil.easedProgress
import org.magic.magicaddons.util.ScreenUtil.fillCornerTriangle
import org.magic.magicaddons.util.ScreenUtil.fillRounded
import org.magic.magicaddons.util.ScreenUtil.inRect
import org.magic.magicaddons.util.ScreenUtil.modText
import org.magic.magicaddons.util.ScreenUtil.drawItem
import org.magic.magicaddons.util.ScreenUtil.splitMod
import org.magic.magicaddons.util.ScreenUtil.withAlpha
import org.magic.magicaddons.util.toCoarseDuration

class PlantWidget(val plant: Plant) : Renderable, GuiEventListener {
    var x: Int = 0
    var y: Int = 0
    var padding: Int = 0

    var appearedAt: Long = 0L

    var markedAt: Long = 0L

    var isInPreset: Boolean = false

    var missingSpawnConditions: List<String> = emptyList()

    class MergedTargetRegion(
        val corners: List<Pair<Int, Int>>,
        val cells: List<ScreenRect>,
        val outline: List<ScreenRect>,
        val iconCell: ScreenRect
    )

    var targetRegion: MergedTargetRegion? = null
    var width = 0
    var height = 0

    var cropStack: ItemStack = ItemStack.EMPTY

    var isPlanMuted: Boolean = false

    private val markColor: Int? get() = plant.slot.mark?.takeUnless { isPlanMuted }?.color

    var waterEffect: Int = 0

    var drainingSoggybuds: Int = 0

    var soggybudTicksToGrow: Int? = null

    var soggybudTicksIfWatered: Int? = null

    var isSoggybudSimulated: Boolean = false

    private var hintMarkBox: ScreenRect? = null

    private var chargeMarkBox: ScreenRect? = null

    private var hintTooltip: String? = null

    private var cornerMarkBox: ScreenRect? = null

    private var cornerMarkTooltip: String? = null

    private var mutationCountBox: ScreenRect? = null

    var decayOutlook: DecayOutlook? = null

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        hintMarkBox = null
        hintTooltip = null
        chargeMarkBox = null
        cornerMarkBox = null
        cornerMarkTooltip = null
        mutationCountBox = null

        if (isPlanMuted && plant.slot.mark == LayoutSlot.Marking.Target && plant.growthStage == null) return

        markColor?.let { color ->
            val region = targetRegion

            if (region != null) {
                region.outline.forEach { graphics.fill(it.x, it.y, it.right, it.bottom, color) }
                return@let
            }

            graphics.drawBorder(x, y, x + width, y + height, Common.UI.BORDER_SIZE, color)

            val cornerTagSize = (width / 3).coerceAtLeast(5)
            graphics.fillCornerTriangle(x + width, y, cornerTagSize, color)
        }
        if (plant.cropDef === FireElement.definition) {
            renderFire(graphics)
            return
        }
        if (markedAt != 0L) {
            val flashProgress = easedProgress(markedAt, FLASH_MS)
            if (flashProgress < 1f) graphics.fill(x, y, x + width, y + height, withAlpha(FLASH_COLOR, 1f - flashProgress))
        }

        val elapsed = System.currentTimeMillis() - appearedAt
        val popScale = if (appearedAt == 0L || elapsed >= POP_MS) 1f else POP_FROM + (1f - POP_FROM) * elapsed / POP_MS

        graphics.pose().pushMatrix()
        graphics.pose().translate(x + width / 2f, y + height / 2f)
        graphics.pose().scale(popScale, popScale)
        graphics.pose().translate(-(x + width / 2f), -(y + height / 2f))
        renderCropIcons(graphics)
        graphics.pose().popMatrix()

        val decayDoubt = plant.predictedDecayDoubt
        when {
            decayDoubt != null -> renderCornerMark(graphics, MAY_HAVE_DECAYED_MARK, decayDoubtExplanation(decayDoubt))
            plant.isHaltedByWater -> renderCornerMark(graphics, HALTED_MARK, HALTED_EXPLANATION)
        }

        if (missingSpawnConditions.isNotEmpty()) {
            val textHeight = Minecraft.getInstance().font.lineHeight * LABEL_TEXT_SCALE
            drawScaledLabel(graphics, BLOCKED_LABEL, y + height - textHeight - 1f, Common.UI.DANGER_COLOR)
        }
    }

    private fun renderCropIcons(graphics: GuiGraphicsExtractor) {
        targetRegion?.let { region ->
            graphics.drawCountedCrop(
                Minecraft.getInstance().font, cropStack, region.iconCell, region.corners.size, Common.UI.TEXT_COLOR
            )
            return
        }

        val alternatives = alternativeStacks
        val innerWidth = width - padding * 2

        when (alternatives.size) {
            0 -> graphics.drawItem(cropStack, x + padding, y + padding, innerWidth, height - padding * 2)
            1 -> {
                val splitIconSize = (innerWidth * SPLIT_ICON_SHARE).toInt().coerceAtLeast(4)
                graphics.drawItem(cropStack, x + padding, y + padding, splitIconSize, splitIconSize)
                graphics.drawItem(alternatives[0], x + width - padding - splitIconSize, y + height - padding - splitIconSize, splitIconSize, splitIconSize)
                renderSplitDiagonal(graphics)
            }
            else -> {
                val shownIndex = ((System.currentTimeMillis() / ALTERNATIVE_CYCLE_MS) % (alternatives.size + 1)).toInt()
                val stack = if (shownIndex == 0) cropStack else alternatives[shownIndex - 1]
                graphics.drawItem(stack, x + padding, y + padding, innerWidth, height - padding * 2)
            }
        }
    }

    private fun renderSplitDiagonal(graphics: GuiGraphicsExtractor) {
        val inset = padding
        val span = width - inset * 2
        for (step in 0 until span) {
            val lineX = x + inset + step
            val lineY = y + height - inset - 1 - step * (height - inset * 2) / span
            graphics.fill(lineX, lineY, lineX + 1, lineY + DIAGONAL_WIDTH, Common.UI.TEXT_COLOR)
        }
    }

    private val alternativeStacks: List<ItemStack> by lazy { plant.presetAlternatives.map { ScreenUtil.itemStackFor(it) } }

    private fun renderFire(graphics: GuiGraphicsExtractor) {
        val sprite = FIRE_SPRITE ?: return
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height)
    }

    fun renderLabel(graphics: GuiGraphicsExtractor, label: PlantLabel) {
        if (label == PlantLabel.WaterLevel) {
            if (plant.isAsleep) {
                renderStalledLabel(graphics)
                return
            }

            if (!plant.isPlacedMutation) plant.cropDef.chargeRule?.let {
                renderChargeMeter(graphics, it)
                return
            }

            if (plant.cropDef.drainsNeighbours) {
                if (plant.consumesWater) plant.waterLevel?.let { renderWaterTime(graphics, it, y + height) }
                return
            }

            plant.chanceToReachStage?.let {
                renderChanceToReachStage(graphics, it)
                return
            }

            if (!plant.cropDef.needsWater || (!plant.consumesWater && drainingSoggybuds == 0)) return

            plant.waterLevel?.let {
                renderWaterMeter(graphics, it.coerceAtLeast(PlotPrediction.WATER_HALT_LEVEL.toDouble()))
            }
            return
        }

        if (label == PlantLabel.DecayTime) renderMutationCount(graphics, label.colorFor(plant))

        val text = label.valueFor(plant, decayOutlook) ?: return
        val font = Minecraft.getInstance().font
        val textHeight = font.lineHeight * LABEL_TEXT_SCALE

        drawScaledLabel(graphics, text, y + height - textHeight - 1f, label.colorFor(plant))
    }

    private fun renderMutationCount(graphics: GuiGraphicsExtractor, color: Int) {
        if (plant.cropDef === FireElement.definition || plant.cropDef.minMutationsBeforeDecay == null || plant.isGrowingTeleporter) return
        if (decayOutlook?.canDecay == false) return

        val text = (if (plant.mutationsSpawnedIsMinimum) "≥" else "") + plant.mutationsSpawned
        mutationCountBox = drawScaledLabel(graphics, text, y + 1f, color, left = x + 1f)
    }

    fun mutationCountTooltipAt(mouseX: Int, mouseY: Int): String? {
        val box = mutationCountBox ?: return null
        if (!inRect(mouseX, mouseY, box.x, box.y, box.width, box.height)) return null

        return if (plant.mutationsSpawnedIsMinimum) MUTATION_COUNT_MINIMUM else MUTATION_COUNT_EXACT
    }

    private fun renderStalledLabel(graphics: GuiGraphicsExtractor) {
        val font = Minecraft.getInstance().font
        val textHeight = font.lineHeight * LABEL_TEXT_SCALE

        hintTooltip = plant.cropDef.stallExplanation
        hintMarkBox = drawScaledLabel(graphics, STALLED_LABEL, y + height - textHeight - 1f, Common.UI.DANGER_COLOR)
    }

    private fun renderChanceToReachStage(graphics: GuiGraphicsExtractor, chance: Double) {
        val font = Minecraft.getInstance().font
        val textHeight = font.lineHeight * LABEL_TEXT_SCALE
        val color = if (chance >= LIKELY_CHANCE) Common.UI.SUCCESS_COLOR else Common.UI.DANGER_COLOR

        hintTooltip = CHANCE_TO_REACH_STAGE
        hintMarkBox = drawScaledLabel(graphics, formatChance(chance) + HINT_MARK, y + height - textHeight - 1f, color)
    }

    private fun formatChance(chance: Double): String {
        val percent = chance * 100

        return when {
            percent >= 10 -> "%.0f%%".format(percent)
            percent >= 1 -> "%.1f%%".format(percent)
            else -> "%.2f%%".format(percent)
        }
    }

    private fun drawScaledLabel(graphics: GuiGraphicsExtractor, text: String, top: Float, color: Int, left: Float? = null): ScreenRect {
        val font = Minecraft.getInstance().font

        val textWidth = font.width(text) * LABEL_TEXT_SCALE
        val textHeight = font.lineHeight * LABEL_TEXT_SCALE
        val textX = left ?: (x + (width - textWidth) / 2f)

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
            pose.scale(LABEL_TEXT_SCALE, LABEL_TEXT_SCALE)
            graphics.modText(font, text, 0, 0, color)
        } finally {
            pose.popMatrix()
        }

        return ScreenRect.fromEdges(textX.toInt(), top.toInt(), (textX + textWidth).toInt(), (top + textHeight).toInt())
    }

    private fun meterRect(): ScreenRect? {
        val meterWidth = width - METER_INSET * 2
        if (meterWidth < METER_MIN_WIDTH) return null

        val bottom = y + height - METER_INSET
        return ScreenRect(x + METER_INSET, bottom - METER_HEIGHT, meterWidth, METER_HEIGHT)
    }

    private fun renderWaterMeter(graphics: GuiGraphicsExtractor, waterLevel: Double) {
        val meter = meterRect() ?: return

        val hunger = plant.hunger
        if (hunger != null) {
            val meatTop = y + METER_INSET
            val meatBottom = meatTop + METER_HEIGHT

            graphics.fillRounded(meter.x, meatTop, meter.right, meatBottom, METER_RADIUS, Common.UI.MEAT_TRACK_COLOR)
            val fed = meter.width * hunger.coerceIn(0, 100) / 100
            if (fed > 0) graphics.fillRounded(meter.x, meatTop, meter.x + fed, meatBottom, METER_RADIUS, Common.UI.MEAT_FULL_COLOR)
        }

        if (plant.consumesWater) renderWaterTime(graphics, waterLevel, meter.y)

        graphics.fillRounded(meter.x, meter.y, meter.right, meter.bottom, METER_RADIUS, Common.UI.WATER_TRACK_COLOR)

        val filled = (meter.width * waterLevel.absoluteValue.coerceAtMost(100.0) / 100).toInt()
        if (filled <= 0) return

        if (waterLevel >= 0) {
            graphics.fillRounded(meter.x, meter.y, meter.x + filled, meter.bottom, METER_RADIUS, Common.UI.WATER_FULL_COLOR)
        } else {
            graphics.fillRounded(meter.right - filled, meter.y, meter.right, meter.bottom, METER_RADIUS, Common.UI.WATER_NEGATIVE_COLOR)
        }
    }

    private fun renderChargeMeter(graphics: GuiGraphicsExtractor, chargeRule: ChargeRule) {
        val meter = meterRect() ?: return

        renderFullChargeTime(graphics, chargeRule, meter.y)

        graphics.fillRounded(meter.x, meter.y, meter.right, meter.bottom, METER_RADIUS, Common.UI.WATER_TRACK_COLOR)

        val filled = meter.width * plant.charge.coerceIn(0, chargeRule.limit) / chargeRule.limit
        if (filled > 0) graphics.fillRounded(meter.x, meter.y, meter.x + filled, meter.bottom, METER_RADIUS, Common.UI.CHARGE_FULL_COLOR)
    }

    private fun renderFullChargeTime(graphics: GuiGraphicsExtractor, chargeRule: ChargeRule, meterTop: Int) {
        if (plant.isFullyGrown) return

        val remainingMs = GreenhouseTickTime.remainingTickMs()
        val tickMs = GreenhouseTickTime.tickMs
        val stagesToGrow = (plant.cropDef.maxStage - (plant.lowestStage ?: 1)).coerceAtLeast(0)
        val willReachFullCharge = plant.charge + chargeRule.perStage * stagesToGrow >= chargeRule.limit

        val stagesCounted = if (willReachFullCharge) chargeRule.stagesUntilFullCharge(plant.charge) else stagesToGrow
        val color = if (willReachFullCharge) Common.UI.DANGER_COLOR else Common.UI.SUCCESS_COLOR

        val text = if (remainingMs == null || tickMs == null || stagesCounted <= 0) {
            "?"
        } else {
            (remainingMs + (stagesCounted - 1) * tickMs).toCoarseDuration()
        }

        val font = Minecraft.getInstance().font
        val textHeight = font.lineHeight * LABEL_TEXT_SCALE
        val box = drawScaledLabel(graphics, text + if (plant.chargeKnown) "" else HINT_MARK, meterTop - textHeight - 1f, color)
        chargeMarkBox = if (plant.chargeKnown) null else box
    }

    fun chargeTooltipAt(mouseX: Int, mouseY: Int): String? {
        val box = chargeMarkBox ?: return null

        return CHARGE_ESTIMATED.takeIf { inRect(mouseX, mouseY, box.x, box.y, box.width, box.height) }
    }

    private fun renderWaterTime(graphics: GuiGraphicsExtractor, waterLevel: Double, meterTop: Int) {
        if (waterLevel <= PlotPrediction.WATER_HALT_LEVEL) return

        val remainingMs = GreenhouseTickTime.remainingTickMs()
        val tickMs = GreenhouseTickTime.tickMs
        val isWaterNegative = plant.waterPredictedNegative

        val stage = if (isWaterNegative) plant.highestStage else plant.lowestStage

        val text: String
        val color: Int

        if (stage == null || tickMs == null || remainingMs == null) {
            text = "?"
            color = Common.UI.TEXT_COLOR
        } else if (plant.cropDef.drainsNeighbours) {
            val ticksToGrow = soggybudTicksToGrow

            if (!isSoggybudSimulated) {
                text = "?"
                color = Common.UI.TEXT_COLOR
            } else if (ticksToGrow == null && decayOutlook?.canDecay != false) {
                text = "stalls" + HINT_MARK
                color = Common.UI.DANGER_COLOR
                hintTooltip = SOGGYBUD_STALL
            } else if (ticksToGrow == null) {
                val ticksIfWatered = soggybudTicksIfWatered
                if (ticksIfWatered == null) {
                    text = "no water" + HINT_MARK
                    color = Common.UI.DANGER_COLOR
                    hintTooltip = SOGGYBUD_NO_DONORS
                } else {
                    text = (remainingMs + (ticksIfWatered - 1) * tickMs).toCoarseDuration() + HINT_MARK
                    color = Common.UI.WATER_FULL_COLOR
                    hintTooltip = SOGGYBUD_IF_WATERED
                }
            } else {
                text = (remainingMs + (ticksToGrow - 1) * tickMs).toCoarseDuration()
                color = Common.UI.SUCCESS_COLOR
            }
        } else {
            val stagesLeft = (plant.cropDef.maxStage - stage).coerceAtLeast(1)

            val waterLossPerTick = PlotPrediction.waterLossPerTick(waterEffect) + PlotPrediction.DRAIN_PER_DONOR * drainingSoggybuds
            val ticksToHalt = if (waterLossPerTick <= 0.0) null else ceil((waterLevel - PlotPrediction.WATER_HALT_LEVEL) / waterLossPerTick).toInt()

            fun timeOf(ticks: Int): String = (remainingMs + (ticks - 1) * tickMs).toCoarseDuration()

            if (ticksToHalt != null && ticksToHalt <= stagesLeft) {
                text = (if (isWaterNegative) "≤" else "") + timeOf(ticksToHalt) + (if (isWaterNegative) HINT_MARK else "")
                color = Common.UI.DANGER_COLOR
                if (isWaterNegative) hintTooltip = NEGATIVE_WATER_MAY_HALT
            } else {
                val mayStall = isWaterNegative || waterLevel - waterLossPerTick * stagesLeft < 0
                text = (if (mayStall) "≥" else "") + timeOf(stagesLeft) + (if (mayStall) HINT_MARK else "")
                color = Common.UI.SUCCESS_COLOR
                if (mayStall) hintTooltip = NEGATIVE_WATER_MAY_STALL
            }
        }

        val font = Minecraft.getInstance().font
        val textHeight = font.lineHeight * LABEL_TEXT_SCALE
        val box = drawScaledLabel(graphics, text, meterTop - textHeight - 1f, color)

        if (hintTooltip != null) hintMarkBox = box
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        targetRegion?.cells?.any { inRect(mouseX, mouseY, it.x, it.y, it.width, it.height) }
            ?: inRect(mouseX, mouseY, x, y, width, height)

    override fun isFocused(): Boolean = false

    override fun setFocused(focused: Boolean) {}

    private fun renderCornerMark(graphics: GuiGraphicsExtractor, mark: ItemStack, explanation: String) {
        val footprint = plant.cropDef.footprint
        val markSize = (if (footprint.width > 1) width / footprint.width / 2 else width / 3).coerceAtLeast(8)
        val markX = x + width - markSize

        graphics.fill(markX, y, markX + markSize, y + markSize, CORNER_MARK_BACKGROUND)
        graphics.drawItem(mark, markX, y, markSize, markSize)
        cornerMarkBox = ScreenRect(markX, y, markSize, markSize)
        cornerMarkTooltip = explanation
    }

    private fun decayDoubtExplanation(outlook: DecayOutlook): String {
        if (outlook.kind == DecayOutlook.Kind.AfterUncountedSpawns) return MAY_HAVE_DECAYED_UNCOUNTED
        if (outlook.isPooled) {
            return "Decay timer ran out. It decays once the combined mutates remaining of its ${poolText(outlook)} reaches 0 (now ${outlook.mutationsLeft})."
        }

        val spots = if (outlook.spotsThatCanSpawn == 1) "1 spot next to it can" else "${outlook.spotsThatCanSpawn} spots next to it can"
        return "Decay timer ran out. It decays once it has contributed to ${outlook.mutationsLeft} more ${spawnedMutations(outlook.mutationsLeft)}, " +
                "and $spots still spawn one. Whether it is a Dead Plant by now depends on those spawns."
    }

    private fun spawnedMutations(count: Int): String = if (count == 1) "spawned mutation" else "spawned mutations"

    private fun poolText(outlook: DecayOutlook): String = "${outlook.poolSize} ${pluralOf(plant.cropDef.name)}"

    private fun pooledDecayTooltipLine(time: String, outlook: DecayOutlook): Component {
        val remaining = (if (outlook.isMutationsLeftMinimum) "≤" else "") + outlook.mutationsLeft
        return when (outlook.kind) {
            DecayOutlook.Kind.OnTime -> labelled("Decays in", time)
            DecayOutlook.Kind.Never ->
                labelled("Won't decay", "combined mutates remaining: $remaining, none of its ${poolText(outlook)} has a spot next to it that can spawn")
            else -> labelled("Decays in", "≥$time, combined mutates remaining: $remaining (${poolText(outlook)})")
        }
    }

    private fun decayTooltipLine(remainingMs: Long, outlook: DecayOutlook): Component {
        val time = remainingMs.toCoarseDuration()
        if (outlook.isPooled) return pooledDecayTooltipLine(time, outlook)
        val mutationsLeft = "${outlook.mutationsLeft} ${spawnedMutations(outlook.mutationsLeft)}"

        return when (outlook.kind) {
            DecayOutlook.Kind.OnTime -> labelled("Decays in", time)
            DecayOutlook.Kind.AfterSpawns -> labelled("Decays in", "≥$time, needs to contribute to $mutationsLeft")
            DecayOutlook.Kind.AfterUncountedSpawns ->
                if (plant.mutationsSpawnedIsMinimum) {
                    labelled("Decays in", "≥$time, contributed to at least ${plant.mutationsSpawned} of ${plant.cropDef.minMutationsBeforeDecay} spawned mutations")
                } else {
                    labelled("Decays in", "≥$time, needs to contribute to $mutationsLeft")
                }
            DecayOutlook.Kind.Never -> labelled("Won't decay", "needs to contribute to $mutationsLeft, none can spawn next to it")
        }
    }

    fun cornerMarkTooltipAt(mouseX: Int, mouseY: Int): String? {
        val box = cornerMarkBox ?: return null

        return cornerMarkTooltip?.takeIf { inRect(mouseX, mouseY, box.x, box.y, box.width, box.height) }
    }

    fun hintTooltipAt(mouseX: Int, mouseY: Int): String? {
        val box = hintMarkBox ?: return null

        return hintTooltip?.takeIf { inRect(mouseX, mouseY, box.x, box.y, box.width, box.height) }
    }

    fun renderTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val cropDefinition = plant.cropDef

        val lines = buildList {
            add(Component.literal(plant.acceptedCrops.joinToString(" / ") { it.name }).withStyle(ChatFormatting.GREEN))

            plant.slot.mark?.let { marking ->
                add(labelled("Role", marking.name))
            }

            targetRegion?.let { region ->
                add(Component.literal("${region.corners.size} corners it could grow from:").withStyle(ChatFormatting.GRAY))
                region.corners.forEach { (cornerX, cornerY) ->
                    add(Component.literal(" - $cornerX, $cornerY").withStyle(ChatFormatting.WHITE))
                }
            }

            if (missingSpawnConditions.isNotEmpty()) {
                add(Component.literal("$BLOCKED_LABEL, cannot appear here:").withStyle(ChatFormatting.RED))
                missingSpawnConditions.forEach { add(Component.literal(" - $it").withStyle(ChatFormatting.RED)) }
            }

            if (plant.hasAlternatives) {
                add(Component.literal("Possible targets:").withStyle(ChatFormatting.GRAY))
                plant.acceptedCrops.forEach { add(Component.literal(" - ${it.name}").withStyle(ChatFormatting.WHITE)) }
            }

            val growthText = when (val stage = plant.growthStage) {
                is PlantStage.Known ->
                    "${stage.stage}/${cropDefinition.maxStage}"

                is PlantStage.Estimated ->
                    "${stage.range.first}-${stage.range.last}/${cropDefinition.maxStage} (estimated)"

                null -> null
            }

            if (!isInPreset) {
                when {
                    plant.isPlacedMutation -> add(labelled("Growth", if (!plant.isCollectable) "Placed, uncollectable" else "Placed"))
                    plant.readyToHarvest -> add(labelled("Growth", "Harvestable"))
                    else -> growthText?.let { add(labelled("Growth", it)) }
                }

                if (plant.cropDef.needsWater && !plant.isPlacedMutation) {
                    add(labelled("Water", waterText(plant) ?: "Unknown"))
                }

                if (!plant.isPlacedMutation) plant.cropDef.chargeRule?.let { rule ->
                    add(labelled("Charge", "${plant.charge}/${rule.limit}" + if (plant.chargeKnown) "" else HINT_MARK))
                    if (!plant.chargeKnown) add(Component.literal(CHARGE_ESTIMATED).withStyle(ChatFormatting.GRAY))
                }

                plant.decayRemainingMs?.let { remainingMs ->
                    decayOutlook?.let { outlook ->
                        add(decayTooltipLine(remainingMs, outlook))
                        if (outlook.kind == DecayOutlook.Kind.Never) add(labelled("Decay attempt in", remainingMs.toCoarseDuration()))
                    }
                }
            }

            val footprint = cropDefinition.footprint
            if (footprint.width > 1 || footprint.height > 1) {
                add(labelled("Size", "${footprint.width}x${footprint.height}"))
            }

            if (isInPreset) add(Component.literal("Right click to mark").withStyle(ChatFormatting.GRAY))
        }

        val font = Minecraft.getInstance().font
        graphics.drawTooltipLines(lines.flatMap { font.splitMod(it, TOOLTIP_WRAP_WIDTH) }, mouseX, mouseY)
    }

    private fun pluralOf(name: String): String = when {
        name.endsWith("s") -> name
        name.endsWith("o") -> name + "es"
        else -> name + "s"
    }

    companion object {
        private const val LABEL_TEXT_SCALE: Float = 0.75f

        private const val LIKELY_CHANCE: Double = 0.5

        private const val CHANCE_TO_REACH_STAGE: String =
            "Chance this plant reaches this stage without resetting"

        private const val BLOCKED_LABEL: String = "Blocked"

        private const val METER_HEIGHT: Int = 4
        private const val METER_INSET: Int = 3
        private const val METER_RADIUS: Int = 1
        private const val METER_MIN_WIDTH: Int = 8

        private const val HINT_MARK: String = "*"

        private const val STALLED_LABEL: String = "stalled" + HINT_MARK

        private const val TOOLTIP_WRAP_WIDTH: Int = 170

        private const val SOGGYBUD_STALL: String =
            "This soggybud will not have enough neighbours with water in the current situation to reach full growth without decaying first."

        private const val SOGGYBUD_IF_WATERED: String = "Assuming the plants around it are always watered."

        private const val SOGGYBUD_NO_DONORS: String = "None of its neighbours can give this soggybud water, so it can't grow."

        private val NEGATIVE_WATER_MAY_HALT: String = """
            With negative water the plant may skip ticks, and a skipped tick costs no water.
            This is the soonest this plant could halt, only if no tick was skipped.
            It stops needing water once fully grown, so it cannot halt after that.
        """.trimIndent()

        private val NEGATIVE_WATER_MAY_STALL: String = """
            With negative water the plant may skip ticks, and a skipped tick costs no water.
            This plant has enough water regardless, so it will not halt.
            The time is with no tick skipped; it grows on the ticks it doesn't skip, which may be the next one or many away.
        """.trimIndent()

        private val MAY_HAVE_DECAYED_MARK: ItemStack = ItemStack(Items.DEAD_BUSH)

        private val HALTED_MARK: ItemStack = ItemStack(Items.BUCKET)

        private const val CORNER_MARK_BACKGROUND: Int = 0xC0201010.toInt()

        private val HALTED_EXPLANATION: String = """
            In the worst case this plant is halted at -100% water.
            Water it to continue growing.
        """.trimIndent()

        private const val MUTATION_COUNT_EXACT: String = "How many times this plant has mutated"

        private const val MUTATION_COUNT_MINIMUM: String = "The minimum amount of times this plant has mutated"

        private const val MAY_HAVE_DECAYED_UNCOUNTED: String =
            "Decay timer ran out. It helps spawn Chorus Fruit, which teleport away and free the spot for another, " +
                    "so the mod can't count how many it contributed to. It may be a Dead Plant by now."

        private const val SPLIT_ICON_SHARE: Float = 0.62f

        private const val CHARGE_ESTIMATED: String =
            "This thunderling charge is estimated based on the stage of the crop, go near it to update"

        private const val DIAGONAL_WIDTH: Int = 2

        private const val ALTERNATIVE_CYCLE_MS: Long = 900

        private const val POP_MS: Long = 150

        private const val FLASH_MS: Long = 250
        private const val FLASH_COLOR: Int = 0xA0FFFFFF.toInt()
        private const val POP_FROM: Float = 0.5f

        private val FIRE_SPRITE: TextureAtlasSprite? by lazy {
            runCatching {
                ScreenUtil.spriteFor(Blocks.FIRE.defaultBlockState(), Direction.NORTH)
            }.getOrNull()
        }

        private fun labelled(label: String, value: String): Component =
            Component.literal("$label: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE))
    }
}
