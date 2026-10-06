package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import java.time.Instant
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI

object GreenhouseTickTime {

    const val MAX_UNIQUE_CROPS: Int = 10

    private const val BONUS_PER_UNIQUE_CROP: Double = 0.025

    private val FLORA_ATTRIBUTE_ID: String? = "attribute:l52"

    val isFloraAttributeIdKnown: Boolean get() = FLORA_ATTRIBUTE_ID != null

    fun shardLevelOf(attributeId: String): Int? =
        AttributeAPI.attributeMap.entries.firstOrNull { it.key.id == attributeId }?.value?.level

    fun speedAttribute(): Int? =
        GreenhouseData.miscInfo.greenhouseSpeedAttribute
            ?: shardLevelOf(GreenhouseData.GREENHOUSE_SPEED_ATTRIBUTE_ID)?.takeIf { it > 0 }

    fun floraShardLevel(): Int? = FLORA_ATTRIBUTE_ID?.let { shardLevelOf(it) }

    fun floraAttribute(): Int? = FLORA_ATTRIBUTE_ID?.let { floraShardLevel() ?: 0 }

    fun uniqueCropsNeeded(): Int = (MAX_UNIQUE_CROPS - (floraAttribute() ?: 0)).coerceAtLeast(0)

    fun uniqueCropBonus(uniqueCrops: Int, floraAttribute: Int): Double =
        BONUS_PER_UNIQUE_CROP * (uniqueCrops + floraAttribute).coerceAtMost(MAX_UNIQUE_CROPS)

    val tickMs: Long?
        get() {
            val cropGrowth = GreenhouseData.miscInfo.cropGrowthValue ?: return null
            val upgrade = GreenhouseData.miscInfo.cropSpeedUpgradeValue ?: return null

            return stageTimeMs(
                GreenhouseData.getCurrentUniques().size,
                cropGrowth,
                upgrade,
                speedAttribute() ?: 0,
                floraAttribute() ?: 0
            )
        }

    fun remainingTickMs(): Long? {
        val next = GreenhouseData.miscInfo.nextTickTime ?: return null

        return (next.toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0L)
    }

    fun hasGrowthTickPassedSince(epochMs: Long): Boolean? {
        val nextTickMs = GreenhouseData.miscInfo.nextTickTime?.toEpochMilli() ?: return null
        val growthTickMs = tickMs ?: return null

        return Math.floorDiv(System.currentTimeMillis() - nextTickMs, growthTickMs) >
                Math.floorDiv(epochMs - nextTickMs, growthTickMs)
    }

    fun stageTimeMs(
        uniqueCrops: Int,
        cropGrowthStat: Int,
        greenhouseUpgrade: Int,
        speedAttribute: Int = 0,
        floraAttribute: Int = 0
    ): Long {

        val uniqueCropBonus = uniqueCropBonus(uniqueCrops, floraAttribute)
        val cropGrowthBonus = 0.0025 * cropGrowthStat
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
