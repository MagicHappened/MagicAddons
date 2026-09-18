package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.time.Duration

fun gardenWarpLink(): Component = Component.literal("[GARDEN]").withStyle(
    Style.EMPTY
        .withColor(ChatFormatting.GREEN)
        .withClickEvent(ClickEvent.RunCommand("/warp garden"))
        .withHoverEvent(HoverEvent.ShowText(Component.literal("Click here to warp to garden!")))
)

object GreenhouseWarnings {

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
