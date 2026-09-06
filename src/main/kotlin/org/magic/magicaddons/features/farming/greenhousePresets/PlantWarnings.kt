package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.util.ChatUtils
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.greenhouse.GrowthTickEvent
import org.magic.magicaddons.util.toShortDuration
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import java.time.Duration
import java.time.Instant

/**
 * Warnings tied to the growth tick: mutations that finished growing, plants about to decay, and
 * plants that stopped growing until the player acts. Each is sent when a tick lands and again at
 * ten, five and one minute before the next. Decay instead warns at 6h, 1h, 20m, 5m and 1m.
 */
object PlantWarnings {

    private const val HARVEST: String = "harvest-ready"
    private const val DECAY: String = "plant-decay"
    private const val ATTENTION: String = "plant-attention"

    /** The setting keys of each warning kind, under the warning types heading. */
    const val HARVEST_KEY: String = "ReadyToHarvestWarning"
    const val DECAY_KEY: String = "DecayWarning"
    const val SNOOZLING_KEY: String = "SnoozlingAsleepWarning"
    const val NOCTILUME_KEY: String = "NoctilumeTimeWarning"
    const val OTHER_PROFILES_KEY: String = "OtherProfileWarnings"

    /** Decay is measured in hours rather than minutes, so it climbs a ladder of its own. */
    private val DECAY_THRESHOLDS: List<Duration> = listOf(
        Duration.ofHours(6),
        Duration.ofHours(1),
        Duration.ofMinutes(20),
        Duration.ofMinutes(5),
        Duration.ofMinutes(1)
    )

    /** The countdown value used when nothing is pending, which resets that kind's thresholds. */
    private val NOTHING_PENDING_MS: Long = Duration.ofDays(365).toMillis()

    /** One greenhouse's part of a warning: the name shown, and the lines hung inside it. */
    private data class HouseNote(val house: String, val lines: List<String>)

    /** One plant about to rot, kept apart from the others so the soonest can lead. */
    private data class DecayingPlant(
        val house: String,
        val plant: String,
        val remainingMs: Long
    )

    private fun enabled(key: String): Boolean = GreenhousePresets.warningType(key)

    // ------------------------------------------------------------ what each warning is about

    /**
     * Mutations the mod watched grow, now at their last stage. A bought one was never grown, so
     * harvesting it gains nothing. Judged by the highest stage it might be at, to tell the player early.
     */
    private fun harvestNotes(): List<HouseNote> = notes { instance ->
        if (!instance.readyToHarvest) return@notes null

        instance.cropDef.name to null
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
            if (!instance.cravesOtherTime(gardenTime)) return@notes null

            val craving = instance.craving ?: return@notes null

            instance.cropDef.name to
                    "garden on ${timeName(gardenTime)}, craves ${timeName(craving)}"
        }
    }

    /** A greenhouse and its name as a warning says it: the plot, and the profile when it is another's. */
    private data class House(val grid: GreenhouseGrid, val name: String)

    /** The greenhouses worth warning about: this profile's, and the other profiles' when asked for. */
    private fun houses(): List<House> {
        val own = GreenhouseData.greenhouseGrids.map { House(it, it.layout.displayName()) }
        if (!enabled(OTHER_PROFILES_KEY)) return own

        return own + OtherProfiles.profiles.flatMap { profile ->
            profile.grids.map { House(it, "${it.layout.displayName()} (${profile.name})") }
        }
    }

    /** Everything with a decay clock running, mutation or not, soonest first. */
    private fun decayingPlants(): List<DecayingPlant> =
        houses().flatMap { (grid, house) ->
            grid.layout.elementInstances.mapNotNull { instance ->
                val remaining = instance.decayRemainingMs ?: return@mapNotNull null

                DecayingPlant(house, instance.cropDef.name, remaining)
            }
        }.sortedBy { it.remainingMs }

    // -------------------------------------------------------------------------- when they run

    /** Sends the harvest and attention warnings straight after a tick. Decay is on its own schedule. */
    @EventHandler
    fun onGrowthTick(event: GrowthTickEvent) {
        if (!GreenhousePresets.reminder(GreenhousePresets.AT_TICK_KEY)) return

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

            if (notes.isNotEmpty() && GreenhouseWarnings.shouldWarn(HARVEST, remainingMs, GreenhousePresets.reminderThresholds())) {
                send(
                    "Some plants are ready to harvest! Next tick in ${remainingMs.toShortDuration()}",
                    notes
                )
            }
        }

        val attention = attentionNotes()

        if (attention.isNotEmpty() && GreenhouseWarnings.shouldWarn(ATTENTION, remainingMs, GreenhousePresets.reminderThresholds())) {
            send("Some plants need attention! Next tick in ${remainingMs.toShortDuration()}", attention)
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
                inHouse.groupBy { it.plant to it.remainingMs.toShortDuration() }
                    .map { (key, plants) -> "${key.first} x${plants.size} - ${key.second}" }
            )
        }

        send("Some plants decay in ${rungText(rung)}!", notes)
    }

    // --------------------------------------------------------------------------- how they read

    /** Groups the plants each greenhouse should report, counting repeats instead of listing them. */
    private fun notes(
        label: (GreenhouseElementInstance) -> Pair<String, String?>?
    ): List<HouseNote> = houses().mapNotNull { (grid, house) ->
        val counted = grid.layout.elementInstances
            .mapNotNull(label)
            .groupingBy { it }
            .eachCount()

        if (counted.isEmpty()) return@mapNotNull null

        HouseNote(
            house,
            counted.map { (plant, count) ->
                val (name, state) = plant

                "$name x$count" + if (state == null) "" else " - $state"
            }
        )
    }

    /** A headline, the greenhouses under it each holding their detail on hover, and a way home. */
    private fun send(headline: String, notes: List<HouseNote>) {
        val message = ChatUtils.buildWithPrefix(Component.literal(headline).withStyle(ChatFormatting.YELLOW))
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
            message.append(gardenWarpLink())
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
}
