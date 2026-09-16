package org.magic.magicaddons.data.greenhouse

import java.time.Instant

data class MiscGreenhouseInfo(
    var nextTickTime: Instant? = null,
    var cropGrowthValue: Int? = null,
    var cropSpeedUpgradeValue: Int? = null,
    var cropYieldUpgradeValue: Int? = null,
    /** worth a tenth of a percent a level */
    var greenhouseSpeedAttribute: Int? = null,
    val cropsWithoutInfo: MutableSet<String> = mutableSetOf()
)
