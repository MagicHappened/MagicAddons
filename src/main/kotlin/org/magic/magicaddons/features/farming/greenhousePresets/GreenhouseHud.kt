package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.network.chat.Component
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.WaterModel
import org.magic.magicaddons.ui.hud.ConfigTarget
import org.magic.magicaddons.ui.hud.HudContent
import org.magic.magicaddons.ui.hud.HudElement
import org.magic.magicaddons.ui.hud.HudLine
import org.magic.magicaddons.ui.hud.HudSituation
import org.magic.magicaddons.util.toReadableDuration
import org.magic.magicaddons.util.toShortDuration
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import java.time.Duration

/** A small panel on screen in the player's own garden: the next tick, and in a greenhouse what the plants need. */
object GreenhouseHud : HudElement("greenhouse", "Greenhouse") {

    const val KEY: String = "GreenhouseHud"

    override val defaultX: Int = 8
    override val defaultY: Int = 8

    override val situations: Set<HudSituation> = setOf(HudSituation.GARDEN, HudSituation.GREENHOUSE)

    /** Under this much time left, a countdown is shown in the danger colour. */
    private val URGENT_MS: Long = Duration.ofHours(1).toMillis()

    private fun setting(): BooleanSetting? = GreenhousePresets.baseSetting.getChild<BooleanSetting>(KEY)

    private fun enabled(): Boolean = GreenhousePresets.baseSetting.value && setting()?.value == true

    override val configTarget: ConfigTarget?
        get() = setting()?.let { ConfigTarget(GreenhousePresets, listOf(GreenhousePresets.baseSetting, it)) }

    /** One line of the panel: a label and its value, each in its own colour. */
    private class Line(val label: String, val value: String, val valueColor: Int = Common.UI.TEXT_COLOR)

    override fun content(): HudContent? {
        if (!enabled()) return null

        // own garden only, unless the anywhere switch is on
        val ownGarden = GreenhouseData.inOwnGarden()
        if (!ownGarden && !(GreenhousePresets.hudAnywhere() && LocationAPI.isOnSkyBlock)) return null

        val grid = if (ownGarden && GreenhouseData.inGreenhouse()) GreenhouseData.getCurrentGrid() else null
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
        val ready = plants.count { it.readyToHarvest }
        // the soonest a plant here dies of thirst, by the same clock the warnings use
        val tickMs = GreenhouseData.currentGrowthTickMs()
        val remainingMs = GreenhouseData.remainingTickMs()
        val thirst = if (tickMs == null || remainingMs == null) null else plants
            .filter { it.needsWater }
            .mapNotNull { plant ->
                val water = plant.waterLevel ?: return@mapNotNull null
                val effect = GreenhouseGrid.waterEffectAt(grid.layout, plant.slot)

                // a plant that reaches its last stage on the water it holds is not dying of thirst
                if (plant.outlastsGrowth(effect) == true) return@mapNotNull null
                if (water <= WaterModel.DEATH) 0L
                else WaterModel.timeUntilDeath(water, effect, remainingMs, tickMs)
            }
            .minOrNull()
        val asleep = plants.count { it.isAsleep }
        val craving = plants.count { it.cravesOtherTime(gardenTime) }
        val decaying = plants.mapNotNull { it.decayRemainingMs }.minOrNull()

        if (ready > 0) add(Line("Ready to harvest", ready.toString(), Common.UI.SUCCESS_COLOR))
        if (thirst != null) add(Line("Dies of thirst in", if (thirst == 0L) "now" else thirst.toShortDuration(), if (thirst < URGENT_MS) Common.UI.DANGER_COLOR else Common.UI.WARNING_COLOR))
        if (asleep > 0) add(Line("Asleep", asleep.toString(), Common.UI.WARNING_COLOR))
        if (craving > 0) add(Line("Wrong time of day", craving.toString(), Common.UI.WARNING_COLOR))
        if (decaying != null) add(Line("Next decay", decaying.toShortDuration(), if (decaying < URGENT_MS) Common.UI.DANGER_COLOR else Common.UI.TEXT_COLOR))
    }
}
