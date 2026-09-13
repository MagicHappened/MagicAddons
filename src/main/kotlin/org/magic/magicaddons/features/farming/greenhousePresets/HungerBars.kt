package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Duration
import java.time.Instant
import java.util.Optional
import java.util.UUID
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.util.getBuildableArea

/**
 * Keeps the hunger of every plant that hangs a bar over itself current while the player stands in
 * the greenhouse, and logs each change with the time left to the tick, so how much a tick takes
 * off the bar can be read from the log.
 */
object HungerBars {

    private const val LOOK_EVERY_TICKS: Int = 20

    private var ticksUntilNextLook: Int = 0

    /** What each bar stand showed when last logged, so the log carries only changes. */
    private val lastShown = mutableMapOf<UUID, String>()

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        if (--ticksUntilNextLook > 0) return
        ticksUntilNextLook = LOOK_EVERY_TICKS

        readHungerBars()
    }

    private fun readHungerBars() {
        if (!GreenhouseData.inOwnGarden()) return
        val grid = GreenhouseData.getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val area = grid.plot?.getBuildableArea() ?: return
        val level = Minecraft.getInstance().level ?: return

        level.getEntitiesOfClass(ArmorStand::class.java, area).forEach { stand ->
            val name = stand.customName ?: return@forEach
            if (CropStandReader.barNotches(name) == null) return@forEach

            val slot = grid.getSlotAt(stand.blockPosition(), matchY = false) ?: return@forEach
            val element = grid.elementCovering(slot) ?: return@forEach
            val instance = element.instance
            if (!instance.cropDef.readsHunger) return@forEach

            val shown = describe(name)
            if (lastShown[stand.uuid] == shown) return@forEach
            lastShown[stand.uuid] = shown

            val percent = CropStandReader.ownBarPercent(name)
            val before = instance.hunger
            if (percent != null) instance.readings[CropStandReader.HUNGER] = percent

            Common.LOGGER.info(
                "[hunger] ${instance.cropDef.name} on ${grid.layout.displayName()} (${slot.x}, ${slot.y}): " +
                        "$shown -> ${percent?.let { "$it%" } ?: "not the plant's own bar"} (was ${before?.let { "$it%" } ?: "unread"}), " +
                        "next tick in ${timeToTick()}"
            )
        }
    }

    /** The bar's text with a count of notches per colour, as "Hunger |||||| [4x#ffaa00 2x#ffffff]". */
    private fun describe(name: Component): String {
        val notchesByColour = linkedMapOf<String, Int>()

        name.visit({ style, text ->
            val notches = text.count { it == '|' }
            if (notches > 0) {
                val colour = style.color?.value?.let { "#%06x".format(it) } ?: "none"
                notchesByColour.merge(colour, notches, Int::plus)
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)

        return "\"${name.string}\" [${notchesByColour.entries.joinToString(" ") { "${it.value}x${it.key}" }}]"
    }

    private fun timeToTick(): String {
        val nextTick = GreenhouseData.miscInfo.nextTickTime ?: return "unknown"
        val left = Duration.between(Instant.now(), nextTick)

        return "${left.toHours()}h ${left.toMinutesPart()}m ${left.toSecondsPart()}s"
    }
}
