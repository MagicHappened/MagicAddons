package org.magic.magicaddons.util

import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import kotlin.jvm.optionals.getOrNull


object LocationUtils {

    private var currentPacket: ClientboundLocationPacket? = null

    // TODO implement an enum for island (search for a made one lol)
    private var island: String? = null
    private var serverType: String? = null

    fun register() {
        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket::class.java) { packet ->
            handleLocationPacket(packet)
        }

        HypixelModAPI.getInstance()
            .subscribeToEventPacket(ClientboundLocationPacket::class.java)
    }

    private fun handleLocationPacket(packet: ClientboundLocationPacket) {
        currentPacket = packet

        serverType = packet.serverType.getOrNull()?.name
        island = packet.mode.getOrNull()
    }

    fun getIsland(): String? = island

    fun isIsland(name: String): Boolean =
        island.equals(name, ignoreCase = true)

    fun isInGarden(): Boolean =
        isIsland("garden")

    fun isSkyblock(): Boolean =
        serverType.equals("SKYBLOCK", ignoreCase = true)

    fun hasLocation(): Boolean =
        island != null
}
