package org.magic.magicaddons.config

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnWorldTickEvent

/**
 * Chat lines a config migration wants the player to read. The config loads before there is a
 * player to talk to, so they wait here until one exists.
 */
object ConfigNotices {
    init {
        EventBus.register(this)
    }

    private val pending = mutableListOf<Component>()

    fun queue(message: Component) {
        pending += message
    }

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        if (pending.isEmpty()) return
        val player = Minecraft.getInstance().player ?: return

        pending.forEach { player.sendSystemMessage(it) }
        pending.clear()
    }
}
