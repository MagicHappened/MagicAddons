package org.magic.magicaddons.util

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.GREENHOUSE_SIZE
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/** Where a garden plot keeps its greenhouse, offset from the corner of the plot itself. */
private const val BUILD_OFFSET = 43

fun BlockPos.center(): Vec3 = Vec3(x + 0.5, y + 0.5, z + 0.5)

/** Reads a duration the game wrote, such as "1d 4h 30m", as milliseconds. */
fun String.parseDurationToMs(): Long {
    var totalMs = 0L

    Regex("""(\d+)([dhms])""").findAll(this).forEach { match ->
        val value = match.groupValues[1].toLong()

        totalMs += when (match.groupValues[2]) {
            "d" -> value * 24 * 60 * 60 * 1000
            "h" -> value * 60 * 60 * 1000
            "m" -> value * 60 * 1000
            "s" -> value * 1000
            else -> 0L
        }
    }

    return totalMs
}

/** A length of time in two units at most: "2d 3h", "9h 40m", "54m", "30s". */
fun Long.toShortDuration(): String {
    val seconds = (this / 1000).coerceAtLeast(0)
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/** The gap between this instant and [from], worded the way the game words its own timers. */
fun Instant.toReadableDuration(from: Instant = Instant.now()): String {
    var seconds = abs(Duration.between(this, from).seconds)

    val days = seconds / 86400
    seconds %= 86400

    val hours = seconds / 3600
    seconds %= 3600

    val minutes = seconds / 60
    seconds %= 60

    val parts = mutableListOf<String>()

    if (days > 0) parts += "${days}d"
    if (hours > 0) parts += "${hours}h"
    if (minutes > 0) parts += "${minutes}m"
    if (seconds > 0 || parts.isEmpty()) parts += "${seconds}s"

    return parts.joinToString(" ")
}

/** The ten by ten the greenhouse occupies, rather than the whole plot around it. */
fun Plot.getBuildableArea(): AABB {
    val box = this.aabb
    val minX = box.minX + BUILD_OFFSET
    val minZ = box.minZ + BUILD_OFFSET

    return AABB(
        minX, box.minY, minZ,
        minX + GREENHOUSE_SIZE, box.maxY, minZ + GREENHOUSE_SIZE
    )
}
