package org.magic.magicaddons.features.farming.greenhousePresets

import kotlin.math.abs
import java.util.UUID
import org.magic.magicaddons.Common
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import java.time.Duration
import java.time.Instant
import java.util.Optional
import org.magic.magicaddons.util.compat.McCompat

/**
 * Reads how much water a greenhouse's plants hold. Skyblock only shows a bar when the level
 * changes, so using a watering can opens a window and the plot is watched for bars while it lasts.
 */
object GreenhouseWatering {

    init {
        EventBus.register(this)
    }

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

    private val waterCanIds: Set<String> get() = sprayGain.keys

    /** The can last used, which says how much each spray tick adds. */
    private var lastCan: String? = null

    /** How many notches each bar showed when last read, so a change counts as one spray tick. */
    private val lastNotches = mutableMapOf<UUID, Int>()

    /** The character skyblock builds its bars out of, one per notch of the level. */
    private const val BAR_CHAR: Char = '|'

    /** Water bar notches: blue is a positive water level, red a negative one, white the empty rest. */
    private val BAR_FILLED_COLOR: Int = McCompat.chatColor(ChatFormatting.BLUE)
    private val BAR_DEBT_COLOR: Int = McCompat.chatColor(ChatFormatting.RED)
    private val BAR_EMPTY_COLOR: Int = McCompat.chatColor(ChatFormatting.WHITE)

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

    /** The spray does not reach the plants on the tick the can is used, so this only opens the window. */


    /** Reads any bar standing over a plant of the current grid, for every batch of new entities. */
    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
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

            if (!element.instance.cropDef.needsWater) return@forEach

            // a bar that changed is one spray tick landing, worth the can's gain over the level held;
            // the bar itself is coarser than that, so it only overrules a count it disagrees with
            // a bar comes up showing the level the plant already holds, so seeing one for the first
            // time is a tick only when it shows more than the level held accounts for
            val notches = bar.notches
            val seen = lastNotches[stand.uuid]
            lastNotches[stand.uuid] = notches
            if (seen == notches) return@forEach
            if (seen == null) {
                val implied = ((element.instance.waterLevel ?: 0) * 16 / 100)
                if (notches <= implied) return@forEach
            }

            // a level known to the point stays exact: the bar is too coarse to correct it, and a bar
            // that failed to move must not drag the count back onto it. A level only ever read off
            // bars stays a bar reading, corrected by the bar when the count drifts from it
            // a bar does not move on every spray tick: sprayed without pause, one plant's bar showed
            // two and three ticks at a time. So the bar is read as a count of ticks, at least one
            // more than before, and the level is that many gains, capped where the game caps it
            val before = element.instance.waterLevel
            val held = before ?: 0
            val ticksHeld = if (held <= 0) 0 else held / gain
            val ticksShown = Math.round(bar.percent.toDouble() / gain).toInt()
            // the game caps at a hundred, and a full bar is that cap rather than fourteen sevens
            val counted = if (bar.percent >= 100) 100 else (gain * maxOf(ticksHeld + 1, ticksShown)).coerceAtMost(100)
            element.instance.waterLevel = when {
                element.instance.waterExact -> counted
                abs(counted - bar.percent) <= NOTCH_PERCENT -> counted
                else -> bar.percent
            }
            element.instance.waterPredictedInDebt = false

            Common.LOGGER.info(
                "[water] spray: ${element.instance.cropDef.name} at (${slot.x}, ${slot.y}) " +
                        "$before -> ${element.instance.waterLevel} (bar ${bar.percent}%, ${bar.notches} notches, " +
                        "exact=${element.instance.waterExact}, can=$lastCan)"
            )
        }
    }

    /**
     * A water bar as a level between -100 and 100: blue notches are water held, red notches debt,
     * counted rather than taken as a leading run. Any other colour is somebody else's bar, refused.
     */
    /** A bar as read: the level it shows, and its filled notches, to tell one bar from the next. */
    private class Bar(val percent: Int, val notches: Int)

    private fun parseBar(name: Component): Bar? {
        var filled = 0
        var debt = 0
        var total = 0
        var foreign = false

        name.visit({ style, text ->
            val notches = text.count { it == BAR_CHAR }

            if (notches > 0) {
                when (style.color?.value) {
                    BAR_FILLED_COLOR -> filled += notches
                    BAR_DEBT_COLOR -> debt += notches
                    BAR_EMPTY_COLOR -> Unit
                    else -> foreign = true
                }

                total += notches
            }

            Optional.empty<Unit>()
        }, Style.EMPTY)

        if (foreign || total == 0) return null


        // a bar cannot show both at once, and a negative level is the one worth reporting
        if (debt > 0) return Bar(-(debt * 100 / total), -debt)

        return Bar(filled * 100 / total, filled)
    }
}
