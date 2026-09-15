package org.magic.magicaddons.util

import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.SetTimePacketEvent

object ServerClock {

    init {
        EventBus.register(this)
    }

    private const val MS_PER_TICK: Long = 50

    private const val INTERPOLATION_CAP_MS: Long = 1_000

    private const val ABSENCE_MS: Long = 20_000

    private var lastGameTime: Long? = null
    private var lastPacketRealMs: Long = System.currentTimeMillis()
    private var msAtLastPacket: Long = 0
    private var lastReadMs: Long = 0

    @EventHandler
    fun onTimePacket(event: SetTimePacketEvent) {
        val realNow = System.currentTimeMillis()
        val realGap = realNow - lastPacketRealMs
        val ticks = lastGameTime?.let { event.packet.gameTime - it }

        val serverGap = ticks?.let { it * MS_PER_TICK }?.takeIf { it > 0 && it <= realGap + INTERPOLATION_CAP_MS }
        msAtLastPacket += when {
            serverGap == null -> realGap
            realGap - serverGap > ABSENCE_MS -> realGap
            else -> serverGap
        }

        lastGameTime = event.packet.gameTime
        lastPacketRealMs = realNow
    }

    fun nowMs(): Long {
        val sinceLastPacket = (System.currentTimeMillis() - lastPacketRealMs).coerceIn(0, INTERPOLATION_CAP_MS)
        lastReadMs = maxOf(lastReadMs, msAtLastPacket + sinceLastPacket)
        return lastReadMs
    }
}
