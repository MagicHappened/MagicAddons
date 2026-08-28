package org.magic.magicaddons.data.greenhouse

import java.time.Instant

data class MiscGreenhouseInfo(
    var nextTickTime: Instant? = null,
    var cropGrowthValue: Int? = null,
    var cropSpeedUpgradeValue: Int? = null,
    var cropYieldUpgradeValue: Int? = null,
    /**
     * The greenhouse speed attribute, worth half a percent a level. Nothing reports it, so it is
     * asked for the way the upgrades are.
     */
    var greenhouseSpeedAttribute: Int? = null,
    var shouldIgnoreWarning: Boolean = false
)