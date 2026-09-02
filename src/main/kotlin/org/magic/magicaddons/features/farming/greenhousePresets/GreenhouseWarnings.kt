package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Duration

/** One plant in trouble: what it is, which greenhouse holds it, and the plot to ride to. */
data class DyingPlant(
    val plant: String,
    val greenhouse: String,
    val plotId: String
)

/**
 * Decides when a greenhouse warning may be sent: ten minutes before its deadline, five, and one,
 * once each. Keyed by the time remaining, so a deadline restated every minute is still one deadline.
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

    /** Updates one warning kind with its current countdown, so a new deadline resets its thresholds. */
    fun tick(kind: String, remainingMs: Long) {
        val cycle = cycles.getOrPut(kind) { Cycle() }

        if (remainingMs > cycle.lastRemainingMs + RESET_SLACK_MS) cycle.fired.clear()
        cycle.lastRemainingMs = remainingMs
    }

    /** Whether this kind should send a warning now. Returning true marks every crossed threshold as used. */
    fun shouldWarn(
        kind: String,
        remainingMs: Long,
        thresholds: List<Duration> = THRESHOLDS
    ): Boolean = warnThreshold(kind, remainingMs, thresholds) != null

    /**
     * The smallest threshold just crossed, or null when none is due. Thresholds skipped over are
     * marked as used as well, so they do not fire afterwards.
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
