package org.magic.magicaddons.features.farming.greenhousePresets

import kotlin.math.abs
import java.util.UUID
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.data.greenhouse.WaterModel
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import java.time.Duration
import java.time.Instant

/**
 * Reads how much water a greenhouse's plants hold. Skyblock only shows a bar when the level
 * changes, so using a watering can opens a window and the plot is watched for bars while it lasts.
 */
object GreenhouseWatering {

    /**
     * Every tier of can and what one spray tick of it adds to a plant's level. Only the top tier has
     * been measured; the rest fall back to it until they are.
     */
    private val sprayGain: Map<String, Int?> = linkedMapOf(
        "HYDRO_CAN_1000" to null,
        "HYDRO_CAN_TURBO_2000" to null,
        "HYDRO_CAN_ULTRA_3000" to null,
        "AQUAMASTER_X" to null,
        "AQUAMASTER_HYDROMAX" to 7
    )

    private const val FALLBACK_GAIN: Int = 7

    /** One notch of a bar, the most the bar can be off from the level behind it. */
    private const val NOTCH_PERCENT: Int = 7

    /** How many notches a full water bar has. */
    private const val BAR_NOTCHES: Int = 16

    private val waterCanIds: Set<String> get() = sprayGain.keys

    /** The can last used, which says how much each spray tick adds. */
    private var lastCan: String? = null

    /** How many notches each bar showed when last read, so a change counts as one spray tick. */
    private val lastNotches = mutableMapOf<UUID, Int>()

    /** How long after a watering the bars are worth looking for before they take themselves away. */
    private val WATERING_WINDOW: Duration = Duration.ofSeconds(10)

    /** When the stands spawned by the last watering stop being expected. */
    private var wateringUntil: Instant? = null


    /** Every tier of the watering can, matched past the prefix and casing of a skyblock id. */
    private fun isWaterCan(id: SkyBlockId): Boolean =
        id.id.substringAfter("item:").uppercase() in waterCanIds

    /** Opens the window if the held item is a watering can, and says whether it did. */
    fun startWateringWindow(heldId: SkyBlockId): Boolean {
        if (!isWaterCan(heldId)) return false

        lastCan = heldId.id.substringAfter("item:").uppercase()
        wateringUntil = Instant.now().plus(WATERING_WINDOW)
        return true
    }

    /** Reads any bar standing over a plant of the current grid, for every batch of new entities. */
    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        readWaterStands()
    }


    /**
     * Polled rather than driven by entity events: the game reuses a bar it already has, and a stand
     * whose name changed counts as neither added nor updated.
     */
    private fun readWaterStands() {
        val until = wateringUntil ?: return

        if (Instant.now().isAfter(until)) {
            wateringUntil = null
            lastNotches.clear()
            return
        }

        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val area = grid.plot?.getBuildableArea() ?: return
        val level = Minecraft.getInstance().level ?: return

        val gain = sprayGain[lastCan] ?: FALLBACK_GAIN

        level.getEntitiesOfClass(ArmorStand::class.java, area).forEach { stand ->
            val bar = stand.customName?.let { parseBar(it) } ?: return@forEach
            val slot = grid.getSlotAt(stand.blockPosition(), matchY = false) ?: return@forEach
            val element = grid.elementCovering(slot) ?: return@forEach

            if (!element.instance.needsWater) return@forEach

            // a bar that changed is one spray tick landing; a bar seen for the first time only counts
            // when it shows more than the level already held
            val notches = bar.notches
            val seen = lastNotches[stand.uuid]
            lastNotches[stand.uuid] = notches
            if (seen == notches) return@forEach
            if (seen == null) {
                val implied = ((element.instance.waterLevel ?: 0.0) * BAR_NOTCHES / 100).toInt()
                if (notches <= implied) return@forEach
            }

            // the bar can skip ticks, so the level is the can's gain times at least one tick more than
            // before, capped where the game caps it
            val before = element.instance.waterLevel
            val held = before ?: 0.0
            val ticksHeld = if (held <= 0.0) 0 else (held / gain).toInt()
            val ticksShown = Math.round(bar.percent.toDouble() / gain).toInt()
            val counted = if (bar.percent >= WaterModel.FULL) WaterModel.FULL else (gain * maxOf(ticksHeld + 1, ticksShown)).coerceAtMost(WaterModel.FULL)
            // an exact level keeps the count; one only ever read off bars is overruled by the bar when
            // they disagree by more than a notch
            element.instance.waterLevel = when {
                element.instance.waterExact -> counted
                abs(counted - bar.percent) <= NOTCH_PERCENT -> counted
                else -> bar.percent
            }.toDouble()
            element.instance.waterBestCase = null
            element.instance.waterPredictedInDebt = false
        }
    }

    /** A bar as read: the level it shows, and its filled notches, to tell one bar from the next. */
    private class Bar(val percent: Int, val notches: Int)

    /**
     * A water bar as a level between -100 and 100: blue notches are water held, red notches debt.
     * Any other colour is somebody else's bar, refused.
     */
    private fun parseBar(name: Component): Bar? {
        val counted = CropStandReader.barNotches(name) ?: return null
        if (counted.other > 0) return null

        // a bar cannot show both at once, and a negative level is the one worth reporting
        if (counted.debt > 0) return Bar(-(counted.debt * 100 / counted.total), -counted.debt)

        return Bar(counted.filled * 100 / counted.total, counted.filled)
    }
}
