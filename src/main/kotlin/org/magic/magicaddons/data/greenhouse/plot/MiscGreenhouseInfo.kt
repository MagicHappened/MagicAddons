package org.magic.magicaddons.data.greenhouse.plot

import java.time.Instant

data class MiscGreenhouseInfo(
    var nextTickTime: Instant? = null,
    var cropGrowthValue: Int? = null,
    var cropSpeedUpgradeValue: Int? = null,
    var cropYieldUpgradeValue: Int? = null,
    var greenhouseSpeedAttribute: Int? = null,
    val cropsWithoutInfo: MutableSet<String> = mutableSetOf()
)
