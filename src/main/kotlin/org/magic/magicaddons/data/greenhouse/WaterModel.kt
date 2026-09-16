package org.magic.magicaddons.data.greenhouse

import kotlin.math.ceil
import kotlin.math.floor

/** how a greenhouse plant loses water, measured in game rather than taken from the wiki */
object WaterModel {

    const val LOSS_PER_TICK: Int = 20

    const val DEATH_LEVEL: Int = -100

    const val FULL_LEVEL: Int = 100

    /** taken from each neighbour holding any water, corners included */
    const val DRAIN_PER_DONOR: Double = 2.5

    /**
     * The share of what it took that a draining plant keeps. The only ratio that reproduces a bud's
     * whole run of shown totals, 10.4 to 54.9 in steps of 20 taken, with the display rounding
     * ties down; 0.555 and 0.56 each miss one of them.
     */
    const val DRAIN_KEPT: Double = 0.556

    /**
     * What a draining plant has to have kept to leave a stage, per stage: ten taken a stage, which
     * is 5.5 as the plant shows it. Judged after the tick's drain has landed.
     */
    const val DRAIN_PER_STAGE: Double = 5.5

    /** clamped, so no amount of retain lets a plant gain water by standing still */
    fun lossPerTick(waterEffectPercent: Int): Double =
        (LOSS_PER_TICK * (1.0 - waterEffectPercent / 200.0)).coerceAtLeast(0.0)

    /** below the death level the gap says how many ticks the plant has been dead for in the estimate */
    fun waterLevelAfter(water: Double, ticks: Int, waterEffectPercent: Int): Double =
        water - lossPerTick(waterEffectPercent) * ticks

    /** where a plant predicted dead but still standing has to be: one tick from dying */
    fun lowestWaterLevelStillAlive(predicted: Double, waterEffectPercent: Int): Double {
        val loss = lossPerTick(waterEffectPercent)
        if (loss <= 0.0 || predicted > DEATH_LEVEL) return predicted

        val ticksSkipped = floor((DEATH_LEVEL - predicted) / loss) + 1

        return predicted + ticksSkipped * loss
    }

    /** the last tick counts even when it only takes the plant part of the way, as the game's own figure does */
    fun ticksUntilDeath(water: Double, waterEffectPercent: Int): Int? {
        val loss = lossPerTick(waterEffectPercent)
        if (loss <= 0.0) return null

        return ceil((water - DEATH_LEVEL) / loss).toInt()
    }

    /** whole when it is whole, otherwise to one place, the way the game writes it */
    fun formatWaterLevel(water: Double): String =
        if (water == floor(water)) water.toInt().toString() else "%.1f".format(water)

    /** what remains of the current tick plus whole ticks after it; the killing tick is not waited out */
    fun timeUntilDeath(water: Double, waterEffectPercent: Int, remainingMs: Long, tickMs: Long): Long? {
        val ticks = ticksUntilDeath(water, waterEffectPercent) ?: return null

        return remainingMs + (ticks - 1) * tickMs
    }
}
