package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Duration

/** One plant in trouble: what it is, which greenhouse holds it, and the plot to ride to. */
data class DyingPlant(
    val plant: String,
    val greenhouse: String,
    val plotId: String
)

/**
 * When a greenhouse warning may speak: the shared cadence for all of them.
 *
 * A warning fires at ten minutes, five, and one before its deadline, once each. Crossing several
 * thresholds at once - logging in four minutes from a death - says its piece once rather than once
 * per threshold missed. The countdown climbing back up is a new deadline, and everything may speak
 * again. Keyed by the remaining time alone, so the deadline being re-stated as a fresh instant
 * every minute (which is what had one warning sent five times) changes nothing.
 *
 * Dehydration is the first customer; the snoozling and noctilume warnings land in the same frame,
 * each under its own kind.
 */
object GreenhouseWarnings {

    /** The ladder almost everything here climbs: ten minutes out, five, and one. */
    val THRESHOLDS: List<Duration> = listOf(
        Duration.ofMinutes(10),
        Duration.ofMinutes(5),
        Duration.ofMinutes(1)
    )

    /** The countdown jumping up by more than this is a new deadline rather than clock jitter. */
    private const val RESET_SLACK_MS: Long = 30_000

    private class Cycle {
        val fired = mutableSetOf<Duration>()
        var lastRemainingMs = Long.MAX_VALUE
    }

    private val cycles = mutableMapOf<String, Cycle>()

    /**
     * Keeps [kind]'s cycle abreast of the countdown. Called every tick whether or not there is
     * anything to warn about, since the new deadline has to be noticed even while all is well.
     */
    fun tick(kind: String, remainingMs: Long) {
        val cycle = cycles.getOrPut(kind) { Cycle() }

        if (remainingMs > cycle.lastRemainingMs + RESET_SLACK_MS) cycle.fired.clear()
        cycle.lastRemainingMs = remainingMs
    }

    /**
     * Whether [kind] should speak now, [remainingMs] from its deadline. Saying yes burns every
     * threshold already crossed, so a deadline is spoken of at most once per rung of [thresholds].
     */
    fun shouldWarn(
        kind: String,
        remainingMs: Long,
        thresholds: List<Duration> = THRESHOLDS
    ): Boolean = warnThreshold(kind, remainingMs, thresholds) != null

    /**
     * The rung that has just been reached, or null when [kind] has nothing to say yet.
     *
     * The tightest of the crossed rungs, since that is the one the player is actually near: a
     * warning that arrives with four minutes left is a five minute warning however long it has
     * been since ten minutes went by. Every crossed rung is burned either way, so the ones that
     * were skipped do not fire afterwards.
     */
    fun warnThreshold(
        kind: String,
        remainingMs: Long,
        thresholds: List<Duration> = THRESHOLDS
    ): Duration? {
        val cycle = cycles.getOrPut(kind) { Cycle() }

        val crossed = thresholds.filter { remainingMs <= it.toMillis() }
        if (crossed.isEmpty() || crossed.all { it in cycle.fired }) return null

        cycle.fired.addAll(crossed)

        return crossed.minOrNull()
    }
}
