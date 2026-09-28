package org.magic.magicaddons.util

import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.plot.GREENHOUSE_SIZE
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot

private const val BUILD_OFFSET = 43

fun BlockPos.center(): Vec3 = Vec3(x + 0.5, y + 0.5, z + 0.5)

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

fun Long.toCoarseDuration(): String {
    val seconds = this / 1000

    val days = seconds / 86400
    val hours = seconds % 86400 / 3600
    val minutes = seconds % 3600 / 60

    return when {
        days > 0 -> "${days}d ${hours}h"
        seconds >= HOURS_ONLY_AFTER_SECONDS -> "${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private const val HOURS_ONLY_AFTER_SECONDS: Long = 6 * 60 * 60

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

fun Plot.getBuildableArea(): AABB {
    val box = this.aabb
    val minX = box.minX + BUILD_OFFSET
    val minZ = box.minZ + BUILD_OFFSET

    return AABB(
        minX, box.minY, minZ,
        minX + GREENHOUSE_SIZE, box.maxY, minZ + GREENHOUSE_SIZE
    )
}
