package org.magic.magicaddons.util

import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnSetTimePacket
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.checkForUpdate
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.greenhouseGrids
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object ServerUtils {
    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    var lastGameTime: Long? = null
        private set

    var totalServerTicks: Long = 0
        private set

    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        totalServerTicks = 0
        lastGameTime = null

        if (event.new != SkyBlockIsland.GARDEN) {
            DataHandler.saveGardenData()
            greenhouseGrids.forEach {
                it.state.hasRuntimeReferences = false
            }
        }
        checkForUpdate()

    }

    @EventHandler
    fun onTick(event: OnSetTimePacket) {

        val currentGameTime = event.packet.gameTime
        val previousGameTime = lastGameTime

        if (previousGameTime != null) {
            totalServerTicks += currentGameTime - previousGameTime
        }

        lastGameTime = currentGameTime

    }
}