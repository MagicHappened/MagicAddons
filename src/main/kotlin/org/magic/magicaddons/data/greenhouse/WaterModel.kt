package org.magic.magicaddons.data.greenhouse

import kotlin.math.ceil

/**
 * How a greenhouse plant loses water.
 *
 * Measured in game over several growth ticks rather than taken from the wiki, which states a loss
 * of two to three a stage and is wrong by an order of magnitude. The readings and the working are
 * in notes/water-formula.md; anything changed here should be checked against them.
 *
 * A plant loses [BASE_LOSS] every growth tick. A neighbour carrying water retain takes away half of
 * that per full hundred percent, and a neighbour draining adds to it on the same scale, so the
 * effect is linear in the loss rather than in how long the water lasts:
 *
 *     none        20        +50% retain    15
 *     +100%       10        -30% drain     23
 *
 * A plant dies when its water reaches [DEATH].
 */
object WaterModel {

    /** What a plant loses each growth tick with nothing beside it. */
    const val BASE_LOSS: Int = 20

    /** The level a plant dies at. */
    const val DEATH: Int = -100

    /** The level a freshly watered plant holds. */
    const val FULL: Int = 100

    /**
     * What a plant loses each tick given the water effects of everything beside it, as a total
     * signed percentage: 50 for one retaining neighbour, -30 for a draining one.
     *
     * Whether two neighbours of the same kind really add up this way is not measured yet, so a
     * total is taken at face value and clamped so that no amount of retain can make a plant gain
     * water by standing still.
     */
    fun lossPerTick(waterEffectPercent: Int): Int {
        val loss = BASE_LOSS * (1.0 - waterEffectPercent / 200.0)

        return loss.coerceAtLeast(0.0).toInt()
    }

    /** Where [water] sits after [ticks] more growth ticks, never past death. */
    fun after(water: Int, ticks: Int, waterEffectPercent: Int): Int =
        (water - lossPerTick(waterEffectPercent) * ticks).coerceAtLeast(DEATH)

    /**
     * How many ticks a plant at [water] has before it dies, or null when it loses nothing at all.
     *
     * The last tick counts even when it only takes the plant part of the way, which is what the
     * game's own "lasts for" figure does, and is why an effect can change the loss without changing
     * that figure.
     */
    fun ticksUntilDeath(water: Int, waterEffectPercent: Int): Int? {
        val loss = lossPerTick(waterEffectPercent)
        if (loss <= 0) return null

        return ceil((water - DEATH).toDouble() / loss).toInt()
    }

    /**
     * How long a plant at [water] has left, in the same terms the game states it.
     *
     * The tick already running only counts for what is left of it, [remainingMs], and every tick
     * after that counts in full. The last tick is the one that kills the plant, so it is not
     * waited out: a plant one tick from death has exactly the current tick left, which is why the
     * rose at -93 read the same twenty minutes as its countdown.
     */
    fun timeUntilDeath(water: Int, waterEffectPercent: Int, remainingMs: Long, tickMs: Long): Long? {
        val ticks = ticksUntilDeath(water, waterEffectPercent) ?: return null

        return remainingMs + (ticks - 1) * tickMs
    }
}
