package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.data.greenhouse.WaterModel
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.NEVER_DECAYS
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.render.OnHudRenderEvent
import org.magic.magicaddons.ui.hud.HudPosition
import org.magic.magicaddons.util.ScreenUtil.drawPanel
import org.magic.magicaddons.util.compat.McCompat
import org.magic.magicaddons.util.toReadableDuration

/** A small panel on screen while standing in a greenhouse: the next tick and what the plants need. */
object GreenhouseHud {

    const val KEY: String = "GreenhouseHud"

    private val position = HudPosition(offsetX = 8, offsetY = 8, xFraction = 0f, yFraction = 0f)

    private const val PAD: Int = 5

    init {
        EventBus.register(this)
    }

    private fun enabled(): Boolean =
        GreenhousePresets.baseSetting.value &&
                GreenhousePresets.baseSetting.getChild<BooleanSetting>(KEY)?.value == true

    /** One line of the panel: a label and its value, each in its own colour. */
    private data class Line(val label: String, val value: String, val valueColor: Int = Common.UI.TEXT_COLOR)

    @EventHandler
    fun onHudRender(event: OnHudRenderEvent) {
        if (!enabled()) return
        if (McCompat.hudHidden() || McCompat.currentScreen() != null) return
        if (!GreenhouseData.inGreenhouse()) return

        val grid = GreenhouseData.getCurrentGrid()
        val lines = lines(grid)
        val font = Minecraft.getInstance().font
        val title = grid?.layout?.displayName() ?: "Greenhouse"

        val labelWidth = lines.maxOfOrNull { font.width(it.label) } ?: 0
        val valueWidth = lines.maxOfOrNull { font.width(it.value) } ?: 0
        val width = maxOf(font.width(title), labelWidth + Common.UI.SPACING + valueWidth) + PAD * 2
        val height = PAD * 2 + font.lineHeight + Common.UI.SPACING + lines.size * (font.lineHeight + 1)

        val x = position.x()
        val y = position.y()
        val graphics = event.graphics

        graphics.drawPanel(x, y, x + width, y + height)
        graphics.text(font, Component.literal(title), x + PAD, y + PAD, Common.UI.ACCENT_COLOR, false)

        var lineY = y + PAD + font.lineHeight + Common.UI.SPACING
        lines.forEach { line ->
            graphics.text(font, Component.literal(line.label), x + PAD, lineY, Common.UI.TEXT_DIM_COLOR, false)
            graphics.text(font, Component.literal(line.value), x + width - PAD - font.width(line.value), lineY, line.valueColor, false)
            lineY += font.lineHeight + 1
        }
    }

    private fun lines(grid: GreenhouseGrid?): List<Line> = buildList {
        add(Line("Next tick", GreenhouseData.miscInfo.nextTickTime?.toReadableDuration() ?: "unknown"))

        val plants = grid?.layout?.elementInstances ?: return@buildList
        add(Line("Plants", plants.size.toString()))

        val gardenTime = GreenhouseGrid.timeOfDayNow()
        val ready = plants.count { it.cropDef.isMutation && it.grewInPlace && (it.highestStage ?: 0) >= it.cropDef.maxStage }
        // the soonest a plant here dies of thirst, by the same clock the warnings use
        val tickMs = GreenhouseData.currentGrowthTickMs()
        val remainingMs = GreenhouseData.remainingTickMs()
        val thirst = if (tickMs == null || remainingMs == null) null else plants
            .filter { it.needsWater }
            .mapNotNull { plant ->
                val water = plant.waterLevel ?: return@mapNotNull null
                if (water <= WaterModel.DEATH) 0L
                else WaterModel.timeUntilDeath(water, grid.layout.waterEffectAt(plant.slot), remainingMs, tickMs)
            }
            .minOrNull()
        val asleep = plants.count { it.isAsleep }
        val craving = plants.count { instance ->
            val wants = instance.craving ?: return@count false
            val stage = instance.lowestStage
            wants != gardenTime && (stage == null || stage < instance.cropDef.maxStage)
        }
        val decaying = plants.mapNotNull { decayRemainingMs(it) }.minOrNull()

        if (ready > 0) add(Line("Ready to harvest", ready.toString(), Common.UI.SUCCESS_COLOR))
        if (thirst != null) add(Line("Dies of thirst in", if (thirst == 0L) "now" else readableMs(thirst), if (thirst < HOUR_MS) Common.UI.DANGER_COLOR else Common.UI.WARNING_COLOR))
        if (asleep > 0) add(Line("Asleep", asleep.toString(), Common.UI.WARNING_COLOR))
        if (craving > 0) add(Line("Wrong time of day", craving.toString(), Common.UI.WARNING_COLOR))
        if (decaying != null) add(Line("Next decay", readableMs(decaying), if (decaying < HOUR_MS) Common.UI.DANGER_COLOR else Common.UI.TEXT_COLOR))
    }

    private fun decayRemainingMs(instance: GreenhouseElementInstance): Long? {
        val decayTime = instance.cropDef.decayTimeMs
        if (decayTime == NEVER_DECAYS) return null
        val age = instance.age ?: return null
        return (decayTime - age).coerceAtLeast(0L)
    }

    private const val HOUR_MS: Long = 60L * 60 * 1000

    /** "2d 3h", "1h 5m", "20m", "40s". */
    private fun readableMs(ms: Long): String {
        val seconds = (ms / 1000).coerceAtLeast(0)
        val days = seconds / 86400
        val hours = seconds % 86400 / 3600
        val minutes = seconds % 3600 / 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}
