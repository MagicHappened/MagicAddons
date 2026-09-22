package org.magic.magicaddons.features.farming.greenhousePresets.lookups

import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidget
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidgetChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent


object StatsWidget {

    const val FARMING_FORTUNE: String = "Farming Fortune"
    const val MINING_FORTUNE: String = "Mining Fortune"

    // the widget draws an icon between the colon and the number
    private val STAT_LINE = Regex("""^[^A-Za-z]*([A-Za-z][A-Za-z ]*?)\s*:\s*\D*?([\d,]+)""")

    var isShown: Boolean? = null
        private set

    private val valueByStat = mutableMapOf<String, Int>()

    fun value(stat: String): Int? = valueByStat[stat]

    @Subscription
    fun onTabWidget(event: TabWidgetChangeEvent) {
        if (event.widget != TabWidget.STATS) return

        isShown = !event.isEmpty
        valueByStat.clear()
        event.new.forEach { line ->
            val match = STAT_LINE.find(line) ?: return@forEach
            valueByStat[match.groupValues[1].trim()] = match.groupValues[2].replace(",", "").toIntOrNull() ?: return@forEach
        }
    }

    @Subscription
    fun onDisconnect(event: ServerDisconnectEvent) {
        isShown = null
        valueByStat.clear()
    }
}
