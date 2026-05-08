package org.magic.magicaddons.features.farming.greenhousePresets

import kotlin.time.Instant

object GreenhouseTickTracker {
    var expectedTickTime: Instant? = null
    var tickWindowActive = false
    var tickConfirmed = false
    var lastConfirmedTick: Instant? = null

    fun receieveTimeString(time: String) {

    }


}