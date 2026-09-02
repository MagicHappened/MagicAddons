package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.NEVER_DECAYS
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.greenhouse.GrowthTickEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import java.time.Duration
import java.time.Instant

/**
 * The warnings that hang off the growth tick: what has finished growing, what is about to rot, and
 * what has stopped growing until somebody sees to it. Each speaks when the tick lands and again on
 * the way to the next one; decay climbs its own ladder instead, six hours down to one minute.
 */
object PlantWarnings {

    init {
        EventBus.register(this)
    }

    private const val HARVEST: String = "harvest-ready"
    private const val DECAY: String = "plant-decay"
    private const val ATTENTION: String = "plant-attention"

    private const val HARVEST_KEY: String = "ReadyToHarvestWarning"
    private const val DECAY_KEY: String = "DecayWarning"
    private const val SNOOZLING_KEY: String = "SnoozlingAsleepWarning"
    private const val NOCTILUME_KEY: String = "NoctilumeTimeWarning"

    /** Decay is measured in hours rather than minutes, so it climbs a ladder of its own. */
    private val DECAY_THRESHOLDS: List<Duration> = listOf(
        Duration.ofHours(6),
        Duration.ofHours(1),
        Duration.ofMinutes(20),
        Duration.ofMinutes(5),
        Duration.ofMinutes(1)
    )

    /** What a countdown reads with nothing to count down to, so the cadence clears its cycle. */
    private val NOTHING_PENDING_MS: Long = Duration.ofDays(365).toMillis()

    /** One greenhouse's part of a warning: the name shown, and the lines hung inside it. */
    private data class HouseNote(val house: String, val lines: List<String>)

    /** One plant about to rot, kept apart from the others so the soonest can lead. */
    private data class DecayingPlant(
        val house: String,
        val plant: String,
        val remainingMs: Long
    )

    private fun enabled(key: String): Boolean =
        baseSetting.getChild<BooleanSetting>(key)?.value == true

    // ------------------------------------------------------------ what each warning is about

    /**
     * Mutations the mod watched grow, now at their last stage. A bought one was never grown, so
     * harvesting it gains nothing. Judged by the highest stage it might be at, to tell the player early.
     */
    private fun harvestNotes(): List<HouseNote> = notes { instance ->
        val definition = instance.cropDef

        if (!definition.isMutation) return@notes null
        if (!instance.grewInPlace) return@notes null

        val stage = instance.highestStage ?: return@notes null
        if (stage < definition.maxStage) return@notes null

        definition.name to null
    }

    /**
     * Plants that have stopped growing: a snoozling asleep, a noctilume craving the other time of
     * day. Each half answers to its own setting.
     */
    private fun attentionNotes(): List<HouseNote> {
        val snoozling = enabled(SNOOZLING_KEY)
        val noctilume = enabled(NOCTILUME_KEY)

        if (!snoozling && !noctilume) return emptyList()

        val gardenTime = GreenhouseGrid.timeOfDayNow()

        return notes { instance ->
            if (snoozling && instance.isAsleep) return@notes instance.cropDef.name to "asleep"

            if (!noctilume) return@notes null

            val craving = instance.craving ?: return@notes null
            if (craving == gardenTime) return@notes null

            // a plant with nothing left to grow craves nothing in practice, whatever its skull says
            val stage = instance.lowestStage
            if (stage != null && stage >= instance.cropDef.maxStage) return@notes null

            instance.cropDef.name to
                    "garden on ${timeName(gardenTime)}, craves ${timeName(craving)}"
        }
    }

    /** Everything with a decay clock running, mutation or not, soonest first. */
    private fun decayingPlants(): List<DecayingPlant> =
        GreenhouseData.greenhouseGrids.flatMap { grid ->
            val house = grid.layout.displayName()

            grid.layout.elementInstances.mapNotNull { instance ->
                val remaining = decayRemainingMs(instance) ?: return@mapNotNull null

                DecayingPlant(house, instance.cropDef.name, remaining)
            }
        }.sortedBy { it.remainingMs }

    /** Time left before this plant rots. Null when it never rots, or its age was never measured. */
    private fun decayRemainingMs(instance: GreenhouseElementInstance): Long? {
        val decayTime = instance.cropDef.decayTimeMs
        if (decayTime == NEVER_DECAYS) return null

        val age = instance.age ?: return null

        return (decayTime - age).coerceAtLeast(0L)
    }

    // -------------------------------------------------------------------------- when they run

    /** The tick has landed, so everything it made true is said now. Decay keeps its own ladder. */
    @EventHandler
    fun onGrowthTick(event: GrowthTickEvent) {
        if (enabled(HARVEST_KEY)) {
            harvestNotes().takeIf { it.isNotEmpty() }?.let {
                send("Some plants are ready to harvest!", it)
            }
        }

        attentionNotes().takeIf { it.isNotEmpty() }?.let {
            send("A tick has progressed in garden! Some plants need attention!", it)
        }
    }

    /**
     * Asked every client tick, so a rung fires the moment it is crossed and a new deadline is noticed.
     */
    fun onTick() {
        tickBoundWarnings()
        decayWarnings()
    }

    /** The two that count down to the next growth tick. */
    private fun tickBoundWarnings() {
        val nextTick = GreenhouseData.miscInfo.nextTickTime ?: return
        val remainingMs = Duration.between(Instant.now(), nextTick).toMillis()

        GreenhouseWarnings.tick(HARVEST, remainingMs)
        GreenhouseWarnings.tick(ATTENTION, remainingMs)

        if (enabled(HARVEST_KEY)) {
            val notes = harvestNotes()

            if (notes.isNotEmpty() && GreenhouseWarnings.shouldWarn(HARVEST, remainingMs)) {
                send(
                    "Some plants are ready to harvest! Next tick in ${shortDuration(remainingMs)}",
                    notes
                )
            }
        }

        val attention = attentionNotes()

        if (attention.isNotEmpty() && GreenhouseWarnings.shouldWarn(ATTENTION, remainingMs)) {
            send("Some plants need attention! Next tick in ${shortDuration(remainingMs)}", attention)
        }
    }

    /** The one that counts down to a plant rotting, on its own ladder and its own soonest plant. */
    private fun decayWarnings() {
        if (!enabled(DECAY_KEY)) {
            GreenhouseWarnings.tick(DECAY, NOTHING_PENDING_MS)
            return
        }

        val plants = decayingPlants()
        val soonest = plants.firstOrNull()?.remainingMs ?: NOTHING_PENDING_MS

        GreenhouseWarnings.tick(DECAY, soonest)

        val rung = GreenhouseWarnings.warnThreshold(DECAY, soonest, DECAY_THRESHOLDS) ?: return

        // only the plants the rung is actually about, so a six hour warning does not also list the
        // plant that has five days left
        val due = plants.filter { it.remainingMs <= rung.toMillis() }
        if (due.isEmpty()) return

        val notes = due.groupBy { it.house }.map { (house, inHouse) ->
            HouseNote(
                house,
                inHouse.groupBy { it.plant to shortDuration(it.remainingMs) }
                    .map { (key, plants) -> "${key.first} x${plants.size} - ${key.second}" }
            )
        }

        send("Some plants decay in ${rungText(rung)}!", notes)
    }

    // --------------------------------------------------------------------------- how they read

    /** Files what each plant is worth saying, counting identical answers instead of repeating them. */
    private fun notes(
        label: (GreenhouseElementInstance) -> Pair<String, String?>?
    ): List<HouseNote> = GreenhouseData.greenhouseGrids.mapNotNull { grid ->
        val counted = grid.layout.elementInstances
            .mapNotNull(label)
            .groupingBy { it }
            .eachCount()

        if (counted.isEmpty()) return@mapNotNull null

        HouseNote(
            grid.layout.displayName(),
            counted.map { (plant, count) ->
                val (name, state) = plant

                "$name x$count" + if (state == null) "" else " - $state"
            }
        )
    }

    /** A headline, the greenhouses under it each holding their detail on hover, and a way home. */
    private fun send(headline: String, notes: List<HouseNote>) {
        val message = Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(headline).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\n"))

        notes.forEachIndexed { index, note ->
            if (index > 0) {
                message.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY))
            }

            message.append(
                Component.literal(note.house).withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withHoverEvent(
                            HoverEvent.ShowText(
                                Component.literal(note.lines.joinToString("\n"))
                            )
                        )
                )
            )
        }

        if (LocationAPI.island != SkyBlockIsland.GARDEN || LocationAPI.isGuest) {
            message.append(Component.literal(" "))
            message.append(
                Component.literal("[GARDEN]").withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(ClickEvent.RunCommand("/warp garden"))
                        .withHoverEvent(
                            HoverEvent.ShowText(
                                Component.literal("Click here to warp to garden!")
                            )
                        )
                )
            )
        }

        Minecraft.getInstance().player?.sendSystemMessage(message)
    }

    private fun timeName(craving: Int): String =
        if (craving == CropStandReader.CRAVES_NIGHT) "Night" else "Day"

    /** A rung as the headline says it: "6 hours", "20 minutes", "1 minute". */
    private fun rungText(rung: Duration): String {
        val minutes = rung.toMinutes()

        return when {
            minutes >= 120 -> "${minutes / 60} hours"
            minutes == 60L -> "1 hour"
            minutes == 1L -> "1 minute"
            else -> "$minutes minutes"
        }
    }

    /** A countdown as the hover and the headline read one: "2d 3h", "1h 5m", "20m", "40s". */
    private fun shortDuration(ms: Long): String {
        val seconds = (ms / 1000).coerceAtLeast(0)
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}
