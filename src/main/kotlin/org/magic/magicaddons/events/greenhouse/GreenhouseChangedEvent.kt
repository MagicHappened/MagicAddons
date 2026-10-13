package org.magic.magicaddons.events.greenhouse

import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid

class GreenhouseChangedEvent(val grid: GreenhouseGrid, val changes: Set<Change>, val source: Source) {

    enum class Change {
        Contents,
        Growth,
        Parts
    }

    enum class Source {
        Scan,
        Pull,
        Diagnosis,
        Watering,
        Hand
    }
}
