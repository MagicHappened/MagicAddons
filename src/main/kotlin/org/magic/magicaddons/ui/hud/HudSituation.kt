package org.magic.magicaddons.ui.hud

import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

enum class HudSituation(val label: String) {
    EVERYTHING("Everything"),
    GARDEN("Garden"),
    GREENHOUSE("Greenhouse"),
    SAFARI("Safari");

    companion object {
        fun currentSituation(): HudSituation = when {
            GreenhouseData.inOwnGreenhouse() -> GREENHOUSE
            GreenhouseData.inGarden() -> GARDEN
            LocationAPI.island == SkyBlockIsland.SAFARI -> SAFARI
            else -> EVERYTHING
        }

        fun situationsToOffer(): List<HudSituation> =
            listOf(EVERYTHING) + entries.filter { situation -> situation != EVERYTHING && HudElements.all.any { situation in it.situations } }
    }
}
