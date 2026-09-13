package org.magic.magicaddons.data.greenhouse

import kotlin.math.ceil
import kotlin.math.floor

/**
 * How a greenhouse plant loses water: BASE_LOSS a tick, halved per full retain effect beside it and
 * raised by draining ones. Measured in game, not off the wiki. Working: notes/water-formula.md.
 */
object WaterModel {

    /** What a plant loses each growth tick with nothing beside it. */
    const val BASE_LOSS: Int = 20

    /** The level a plant dies at. */
    const val DEATH: Int = -100

    /** The level a freshly watered plant holds. */
    const val FULL: Int = 100

    /** What a draining plant takes from each neighbour holding any water, corners included. */
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

    /**
     * Loss per tick for a total signed effect: 50 for one retaining neighbour, -30 for a draining one.
     * Clamped, so no amount of retain lets a plant gain water by standing still.
     */
    fun lossPerTick(waterEffectPercent: Int): Double =
        (BASE_LOSS * (1.0 - waterEffectPercent / 200.0)).coerceAtLeast(0.0)

    /** Water level after that many ticks. Allowed below the death level: the gap says how many
     * ticks the plant has been dead for in the estimate. */
    fun after(water: Double, ticks: Int, waterEffectPercent: Int): Double =
        water - lossPerTick(waterEffectPercent) * ticks

    /**
     * The worst level a plant predicted dead can be at while still standing: one tick from dying.
     */
    fun aliveFloor(predicted: Double, waterEffectPercent: Int): Double {
        val loss = lossPerTick(waterEffectPercent)
        if (loss <= 0.0 || predicted > DEATH) return predicted

        val skips = floor((DEATH - predicted) / loss) + 1

        return predicted + skips * loss
    }

    /**
     * Ticks before the plant dies, null when it loses nothing. The last tick counts even when it
     * only takes the plant part of the way, as the game's own figure does.
     */
    fun ticksUntilDeath(water: Double, waterEffectPercent: Int): Int? {
        val loss = lossPerTick(waterEffectPercent)
        if (loss <= 0.0) return null

        return ceil((water - DEATH) / loss).toInt()
    }

    /** The level as the game writes it: whole when it is whole, otherwise to one place. */
    fun shown(water: Double): String =
        if (water == floor(water)) water.toInt().toString() else "%.1f".format(water)

    /**
     * Time left, stated as the game states it: what remains of the current tick plus whole ticks
     * after it. The killing tick is not waited out.
     */
    fun timeUntilDeath(water: Double, waterEffectPercent: Int, remainingMs: Long, tickMs: Long): Long? {
        val ticks = ticksUntilDeath(water, waterEffectPercent) ?: return null

        return remainingMs + (ticks - 1) * tickMs
    }
}
