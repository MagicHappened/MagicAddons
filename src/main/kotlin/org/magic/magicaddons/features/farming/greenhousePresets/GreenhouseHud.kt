package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.WaterModel
import org.magic.magicaddons.ui.hud.ConfigTarget
import org.magic.magicaddons.ui.hud.HudContent
import org.magic.magicaddons.ui.hud.HudElement
import org.magic.magicaddons.ui.hud.HudLine
import org.magic.magicaddons.util.toReadableDuration

/** A small panel on screen while standing in a greenhouse: the next tick and what the plants need. */
object GreenhouseHud : HudElement("greenhouse", "Greenhouse") {

    const val KEY: String = "GreenhouseHud"

    override val defaultX: Int = 8
    override val defaultY: Int = 8

    private fun setting(): BooleanSetting? = GreenhousePresets.baseSetting.getChild<BooleanSetting>(KEY)

    private fun enabled(): Boolean = GreenhousePresets.baseSetting.value && setting()?.value == true

    override val configTarget: ConfigTarget?
        get() = setting()?.let { ConfigTarget(GreenhousePresets, listOf(GreenhousePresets.baseSetting, it)) }

    /** One line of the panel: a label and its value, each in its own colour. */
    private class Line(val label: String, val value: String, val valueColor: Int = Common.UI.TEXT_COLOR)

    override fun content(): HudContent? {
        if (!enabled() || !GreenhouseData.inGreenhouse()) return null
        val grid = GreenhouseData.getCurrentGrid()
        return content(grid?.layout?.displayName() ?: "Greenhouse", lines(grid))
    }

    override fun sample(): HudContent = content(
        "Greenhouse 1",
        listOf(
            Line("Next tick", "12m 30s"),
            Line("Plants", "24"),
            Line("Ready to harvest", "2", Common.UI.SUCCESS_COLOR),
            Line("Dies of thirst in", "3h 10m", Common.UI.WARNING_COLOR),
            Line("Next decay", "1d 4h")
        )
    )

    private fun content(title: String, lines: List<Line>): HudContent = HudContent(buildList {
        add(HudLine.Text(Component.literal(title).withColor(rgb(Common.UI.ACCENT_COLOR))))
        lines.forEach { line ->
            add(HudLine.Pair(
                Component.literal(line.label).withColor(rgb(Common.UI.TEXT_DIM_COLOR)),
                Component.literal(line.value).withColor(rgb(line.valueColor))
            ))
        }
    })

    /** A text colour carries no alpha. */
    private fun rgb(color: Int): Int = color and 0xFFFFFF

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
