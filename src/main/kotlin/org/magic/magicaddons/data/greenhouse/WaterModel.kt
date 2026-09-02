package org.magic.magicaddons.data.greenhouse

import kotlin.math.ceil

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

    /**
     * Loss per tick for a total signed effect: 50 for one retaining neighbour, -30 for a draining one.
     * Clamped, so no amount of retain lets a plant gain water by standing still.
     */
    fun lossPerTick(waterEffectPercent: Int): Int {
        val loss = BASE_LOSS * (1.0 - waterEffectPercent / 200.0)

        return loss.coerceAtLeast(0.0).toInt()
    }

    /** Water level after that many ticks. Allowed below the death level: the gap says how many
     * ticks the plant has been dead for in the estimate. */
    fun after(water: Int, ticks: Int, waterEffectPercent: Int): Int =
        water - lossPerTick(waterEffectPercent) * ticks

    /**
     * The worst level a plant predicted dead can be at while still standing: one tick from dying.
     */
    fun aliveFloor(predicted: Int, waterEffectPercent: Int): Int {
        val loss = lossPerTick(waterEffectPercent)
        if (loss <= 0 || predicted > DEATH) return predicted

        val skips = (DEATH - predicted) / loss + 1

        return predicted + skips * loss
    }

    /**
     * Ticks before the plant dies, null when it loses nothing. The last tick counts even when it
     * only takes the plant part of the way, as the game's own figure does.
     */
    fun ticksUntilDeath(water: Int, waterEffectPercent: Int): Int? {
        val loss = lossPerTick(waterEffectPercent)
        if (loss <= 0) return null

        return ceil((water - DEATH).toDouble() / loss).toInt()
    }

    /**
     * Time left, stated as the game states it: what remains of the current tick plus whole ticks
     * after it. The killing tick is not waited out.
     */
    fun timeUntilDeath(water: Int, waterEffectPercent: Int, remainingMs: Long, tickMs: Long): Long? {
        val ticks = ticksUntilDeath(water, waterEffectPercent) ?: return null

        return remainingMs + (ticks - 1) * tickMs
    }
}
