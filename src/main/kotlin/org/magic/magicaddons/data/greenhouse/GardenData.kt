package org.magic.magicaddons.data.greenhouse

import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object GardenData {
    //top left = index 0 bottom right index 24
    val GreenhousePlotIndexes = mutableListOf<Int>(2,4,5) //todo change later to not be predetermined

    @Subscription
    private fun islandChange(islandChangeEvent: IslandChangeEvent) {
        if (islandChangeEvent.new != SkyBlockIsland.GARDEN) return


    }

}