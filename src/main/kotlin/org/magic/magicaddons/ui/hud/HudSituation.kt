package org.magic.magicaddons.ui.hud

import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

/** A place the player can be, so the editor can show what the hud holds there. */
enum class HudSituation(val label: String) {
    EVERYTHING("Everything"),
    GARDEN("Garden"),
    GREENHOUSE("Greenhouse"),
    SAFARI("Safari");

    companion object {
        /** Where the player is now, or everything when nowhere named. */
        fun current(): HudSituation = when {
            GreenhouseData.inGreenhouse() -> GREENHOUSE
            LocationAPI.island == SkyBlockIsland.GARDEN -> GARDEN
            LocationAPI.island == SkyBlockIsland.SAFARI -> SAFARI
            else -> EVERYTHING
        }

        /** Everything, then every situation some element belongs to. */
        fun offered(): List<HudSituation> =
            listOf(EVERYTHING) + entries.filter { situation -> situation != EVERYTHING && HudElements.all.any { situation in it.situations } }
    }
}
