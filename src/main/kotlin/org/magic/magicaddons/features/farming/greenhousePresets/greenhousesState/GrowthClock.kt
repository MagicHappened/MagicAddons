package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import java.time.Instant
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.data.greenhouse.plot.*
import org.magic.magicaddons.events.interact.*
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI

/** How long a growth tick takes for this greenhouse, and how much of the running one is left. */
object GrowthClock {

    fun speedAttribute(): Int? =
        GreenhouseData.miscInfo.greenhouseSpeedAttribute
            ?: AttributeAPI.attributeMap.entries
                .firstOrNull { it.key.id == GreenhouseData.GREENHOUSE_SPEED_ATTRIBUTE_ID }
                ?.value
                ?.level
                ?.takeIf { it > 0 }

    fun tickLengthMs(): Long? {
        val cropGrowth = GreenhouseData.miscInfo.cropGrowthValue ?: return null
        val upgrade = GreenhouseData.miscInfo.cropSpeedUpgradeValue ?: return null

        return stageTimeMs(
            GreenhouseData.getCurrentUniques().size,
            cropGrowth,
            upgrade,
            speedAttribute() ?: 0
        )
    }

    fun remainingTickMs(): Long? {
        val next = GreenhouseData.miscInfo.nextTickTime ?: return null

        return (next.toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0L)
    }

    fun stageTimeMs(
        uniqueCrops: Int,
        cropGrowthStat: Int,
        greenhouseUpgrade: Int,
        speedAttribute: Int = 0
    ): Long {

        val uniqueCropBonus = 0.025 * uniqueCrops
        val cropGrowthBonus = 0.0025 * cropGrowthStat
        // a tenth of a percent a level: the attribute caps at one percent over its ten levels
        val attributeBonus = 0.001 * speedAttribute

        val upgradeBonus = when (greenhouseUpgrade) {
            in 0..8 -> 0.05 * greenhouseUpgrade
            9 -> 0.50
            else -> throw IllegalArgumentException("Invalid greenhouse upgrade level: $greenhouseUpgrade")
        }

        val denominator =
            1.0 +
                    uniqueCropBonus +
                    cropGrowthBonus +
                    attributeBonus +
                    upgradeBonus

        val seconds = 14400.0 / denominator

        return (seconds * 1000.0).toLong()
    }
}
