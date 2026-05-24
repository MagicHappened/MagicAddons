package org.magic.magicaddons.data.greenhouse

data class MiscGreenhouseInfo(
    var nextTickTime: Long? = null,
    var cropGrowthValue: Int? = null,
    var cropSpeedUpgradeValue: Int? = null,
    var cropYieldUpgradeValue: Int? = null,
    var shouldIgnoreWarning: Boolean = false
)