package org.magic.magicaddons.util

import net.minecraft.client.Minecraft
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnWorldTickEvent
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import java.time.Duration
import java.time.Instant

/** Says once a session, shortly after joining, that a newer build exists. */
object VersionAnnouncer {
    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    /** Long enough after joining that the message is not buried by the server's own greeting. */
    private val DELAY: Duration = Duration.ofSeconds(5)

    private var speakAt: Instant? = null
    private var spoken = false

    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        if (spoken) return

        speakAt = Instant.now().plus(DELAY)
        VersionChecker.check()
    }

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        val due = speakAt ?: return
        if (Instant.now().isBefore(due)) return

        val player = Minecraft.getInstance().player ?: return
        val found = VersionChecker.result ?: return

        speakAt = null
        spoken = true

        if (!found.outdated) return

        player.sendSystemMessage(VersionChecker.message(found))
    }
}
