package org.magic.magicaddons.ui.hud

import org.magic.magicaddons.util.SBLocation

enum class HudSituation(val label: String) {
    EVERYTHING("Everything"),
    GARDEN("Garden"),
    GREENHOUSE("Greenhouse"),
    SAFARI("Safari");

    companion object {
        fun currentSituation(): HudSituation = when {
            SBLocation.OwnGreenhouse.inside() -> GREENHOUSE
            SBLocation.Garden.inside() -> GARDEN
            SBLocation.Safari.inside() -> SAFARI
            else -> EVERYTHING
        }

        fun situationsToOffer(): List<HudSituation> =
            listOf(EVERYTHING) + entries.filter { situation -> situation != EVERYTHING && HudElements.all.any { situation in it.situations } }
    }
}
