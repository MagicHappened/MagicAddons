package org.magic.magicaddons.events.greenhouse

/**
 * The garden's growth tick has come round.
 *
 * Posted once per look at the clock rather than once per tick: an absence is noticed all at once,
 * so [ticks] is how many of them passed since the last look and is often more than one. [tickMs]
 * is how long a single one of them takes at the player's current stats, which is what anything
 * counting down to the next one needs.
 *
 * Everything the plants themselves owe to those ticks has already been applied by the time this
 * is posted, so a listener reads the world as it now stands rather than as it was.
 */
class GrowthTickEvent(val ticks: Int, val tickMs: Long)
