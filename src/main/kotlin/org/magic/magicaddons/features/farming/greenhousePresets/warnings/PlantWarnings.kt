package org.magic.magicaddons.features.farming.greenhousePresets.warnings

import java.time.Duration
import java.time.Instant
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import org.magic.magicaddons.data.greenhouse.plot.DyingPlant
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.greenhouse.GrowthTickEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.OtherProfiles
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.BioanalysisAccessory
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.toShortDuration
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object PlantWarnings {

    private const val HARVEST: String = "harvest-ready"
    private const val DECAY: String = "plant-decay"
    private const val ATTENTION: String = "plant-attention"

    const val HARVEST_KEY: String = "ReadyToHarvestWarning"
    const val DECAY_KEY: String = "DecayWarning"
    const val SNOOZLING_KEY: String = "SnoozlingAsleepWarning"
    const val NOCTILUME_KEY: String = "NoctilumeTimeWarning"
    const val OTHER_PROFILES_KEY: String = "OtherProfileWarnings"

    /** The setting key of the thirst warning, under the warning types heading. */
    const val THIRST_KEY: String = "DehydrationWarning"

    private const val DEHYDRATION: String = "dehydration"
    private const val CHORUS_COLLISION: String = "chorus-collision"

    val THRESHOLDS: List<Duration> = listOf(
        Duration.ofMinutes(10),
        Duration.ofMinutes(5),
        Duration.ofMinutes(1)
    )

    /** The countdown jumping up by more than this is a new deadline rather than clock jitter. */
    private const val RESET_SLACK_MS: Long = 30_000

    private class Cycle {
        val fired = mutableSetOf<Duration>()
        var lastRemainingMs = Long.MAX_VALUE
    }

    private val cycles = mutableMapOf<String, Cycle>()

    private fun tick(kind: String, remainingMs: Long) {
        val cycle = cycles.getOrPut(kind) { Cycle() }

        if (remainingMs > cycle.lastRemainingMs + RESET_SLACK_MS) cycle.fired.clear()
        cycle.lastRemainingMs = remainingMs
    }

    /** Whether this kind should send a warning now. Returning true marks every crossed threshold as used. */
    private fun shouldWarn(
        kind: String,
        remainingMs: Long,
        thresholds: List<Duration> = THRESHOLDS
    ): Boolean = warnThreshold(kind, remainingMs, thresholds) != null

    /**
     * The smallest threshold just crossed, or null when none is due. Thresholds skipped over are
     * marked as used as well, so they do not fire afterwards.
     */
    private fun warnThreshold(
        kind: String,
        remainingMs: Long,
        thresholds: List<Duration> = THRESHOLDS
    ): Duration? {
        val cycle = cycles.getOrPut(kind) { Cycle() }

        val crossed = thresholds.filter { remainingMs <= it.toMillis() }
        if (crossed.isEmpty() || crossed.all { it in cycle.fired }) return null

        cycle.fired.addAll(crossed)

        return crossed.minOrNull()
    }

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

    /** The last dehydration warning sent while away from the garden, with the time it was sent. */
    private var awayWarning: Pair<Instant, DyingPlant>? = null

    /** A teleport to a dying plant's plot, offered once the garden has finished loading. */
    private var teleportOffer: DyingPlant? = null
    private var teleportOfferAt: Instant? = null

    /** How soon after a warning returning to the garden still counts as a response to it. */
    private val TELEPORT_OFFER_WINDOW: Duration = Duration.ofSeconds(15)

    /** Which greenhouses run out of room for their chorus before the player is next back. */
    fun warnOfChorusCollision() {
        if (!GreenhousePresets.warningTypeEnabled(GreenhousePresets.CHORUS_KEY)) return

        val nextTick = GreenhouseData.miscInfo.nextTickTime ?: return
        val remainingMs = Duration.between(Instant.now(), nextTick).toMillis()

        tick(CHORUS_COLLISION, remainingMs)

        val ticks = GreenhousePresets.chorusAbsenceTicks() ?: return

        val crowded = GreenhouseData.greenhouseGrids.mapNotNull { grid ->
            ChorusCollision.reportFor(grid, ticks, BioanalysisAccessory.mutationWeightMultiplier())
                ?.takeIf { it.needsWarning }
                ?.let { grid.layout to it }
        }

        if (crowded.isEmpty()) return
        if (!shouldWarn(CHORUS_COLLISION, remainingMs)) return

        sendChorusWarning(crowded)
    }

    /** What to break, where, and why. A growing jellybean is named when there is one to lose. */
    private fun sendChorusWarning(crowded: List<Pair<PlotLayout, ChorusCollision.Report>>) {
        val message = ChatUtils.buildWithPrefix(
                Component.literal("Chorus collision likely: ").withStyle(ChatFormatting.RED)
            )

        crowded.forEachIndexed { index, (layout, report) ->
            if (index > 0) {
                message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY))
            }

            message.append(
                Component.literal("break ${report.chorusToBreak} youngest chorus")
                    .withStyle(ChatFormatting.YELLOW)
            )
            message.append(
                Component.literal(" (or harvest ${report.chorusToBreak * 2} ripe)")
                    .withStyle(ChatFormatting.GRAY)
            )
            message.append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))

            // the numbers behind the verdict hang off the greenhouse's own name, so several
            // greenhouses in one warning each keep their own working
            val detail = Component.literal(
                "${report.movingChorus} moving chorus, ${report.freeTiles} free tiles, " +
                        "${report.openSpawners} open spawners over ${report.ticksAway} ticks away.\n" +
                        "Margin ${report.tilesSpare} plus ${report.ripeningChorus} ripening is under the " +
                        "${report.tilesNeeded} the window asks for." +
                        if (report.growingJellybeansAtRisk > 0) {
                            "\n${report.growingJellybeansAtRisk} growing jellybeans stand in the blast radius."
                        } else {
                            ""
                        }
            )

            message.append(
                Component.literal(layout.displayName()).withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withHoverEvent(HoverEvent.ShowText(detail))
                )
            )
        }

        Minecraft.getInstance().player?.sendSystemMessage(message)
    }

    /**
     * Which plants the coming tick will kill, while there is still time to water them. One already
     * past death is left alone, since the dead bush says it better.
     */
    fun warnOfDyingPlants() {
        if (!GreenhousePresets.warningTypeEnabled(THIRST_KEY)) return

        val nextTick = GreenhouseData.miscInfo.nextTickTime ?: return
        val remainingMs = Duration.between(Instant.now(), nextTick).toMillis()

        tick(DEHYDRATION, remainingMs)

        val dying = mutableListOf<DyingPlant>()

        GreenhouseData.greenhouseGrids.forEach { grid ->
            grid.layout.plants.forEach { instance ->
                if (!instance.consumesWater) return@forEach

                val water = instance.waterLevel ?: return@forEach
                if (water <= PlotPrediction.WATER_DEATH_LEVEL) return@forEach

                // a plant that has finished growing stopped drinking, so nothing kills it
                val lowestStage = instance.lowestStage
                if (lowestStage != null && lowestStage >= instance.cropDef.maxStage) return@forEach

                val effect = GreenhouseGrid.waterEffectAt(grid.layout, instance.slot)
                val ticksLeft = PlotPrediction.ticksUntilDeath(water, effect) ?: return@forEach

                if (ticksLeft <= 1) {
                    dying += DyingPlant(
                        instance.cropDef.name,
                        grid.layout.displayName(),
                        grid.layout.id
                    )
                }
            }
        }

        if (dying.isEmpty()) return
        if (!shouldWarn(DEHYDRATION, remainingMs)) return

        sendDehydrationWarning(dying, remainingMs)
    }

    /** Returning to the garden soon after a warning is treated as an answer to it. */
    fun onGardenArrival() {
        awayWarning?.let { (at, plant) ->
            if (Duration.between(at, Instant.now()) <= TELEPORT_OFFER_WINDOW) {
                teleportOffer = plant
                teleportOfferAt = Instant.now().plusSeconds(2)
            }
        }
        awayWarning = null
    }

    fun warnSurvivor(plant: DyingPlant) {
        if (!GreenhousePresets.warningTypeEnabled(THIRST_KEY)) return

        val remaining = GreenhouseData.miscInfo.nextTickTime
            ?.let { Duration.between(Instant.now(), it).toMillis().coerceAtLeast(0) }
            ?: 0

        sendDehydrationWarning(listOf(plant), remaining)
    }

    /** The warning itself, plants grouped by greenhouse, with a way home when away. */
    private fun sendDehydrationWarning(dying: List<DyingPlant>, remainingMs: Long) {
        val byHouse = dying.groupBy({ it.greenhouse }, { it.plant })

        val message = ChatUtils.buildWithPrefix(
                Component.literal("Dying of thirst in ${ChatUtils.shortDuration(remainingMs)}: ")
                    .withStyle(ChatFormatting.RED)
            )

        byHouse.entries.forEachIndexed { index, (house, plants) ->
            if (index > 0) {
                message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY))
            }

            message.append(
                Component.literal(plants.joinToString(", ")).withStyle(ChatFormatting.YELLOW)
            )
            message.append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))
            message.append(Component.literal(house).withStyle(ChatFormatting.AQUA))
        }

        if (LocationAPI.island != SkyBlockIsland.GARDEN || LocationAPI.isGuest) {
            message.append(Component.literal(" "))
            message.append(ChatUtils.gardenWarpLink())

            // remembered so returning to the garden can be answered with a teleport to the plot
            awayWarning = Instant.now() to dying.first()
        }

        Minecraft.getInstance().player?.sendSystemMessage(message)
    }


    fun offerTeleportIfArrived() {
        val offer = teleportOffer ?: return
        val at = teleportOfferAt ?: return

        if (Instant.now().isBefore(at)) return
        if (Minecraft.getInstance().level == null) return

        teleportOffer = null

        if (LocationAPI.island != SkyBlockIsland.GARDEN || LocationAPI.isGuest) return

        val plotNumber = offer.plotId.removePrefix(PlotLayout.PRESET_PREFIX)

        val message = ChatUtils.buildWithPrefix(
                Component.literal(
                    "Click here to teleport to ${offer.greenhouse} to water ${offer.plant}"
                ).withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(ClickEvent.RunCommand("/tptoplot $plotNumber"))
                        .withHoverEvent(
                            HoverEvent.ShowText(Component.literal("Running: /tptoplot $plotNumber"))
                        )
                )
            )

        Minecraft.getInstance().player?.sendSystemMessage(message)
    }

    private fun enabled(key: String): Boolean = GreenhousePresets.warningTypeEnabled(key)


    private fun harvestNotes(): List<HouseNote> = notes { grid, instance ->
        if (!instance.readyToHarvest) return@notes null
        if (GreenhousePresets.harvestHighlightOnlyTargets() && !onWantedTarget(grid, instance)) return@notes null

        instance.cropDef.name to null
    }

    /** Whether [instance] covers a target slot of the plan running on [grid] that accepts its crop. */
    private fun onWantedTarget(grid: GreenhouseGrid, instance: Plant): Boolean {
        val plan = grid.state.assignedLayout?.turned(grid.state.planTurns) ?: return false
        val footprint = instance.cropDef.footprint

        return plan.plants.any { target ->
            target.slot.mark == LayoutSlot.Marking.Target &&
                    target.acceptsCrop(instance.cropDef) &&
                    target.slot.x in instance.slot.x until instance.slot.x + footprint.width &&
                    target.slot.y in instance.slot.y until instance.slot.y + footprint.height
        }
    }

    /**
     * Plants that have stopped growing: a snoozling asleep, a noctilume craving the other time of
     * day. Each half answers to its own setting.
     */
    private fun attentionNotes(): List<HouseNote> {
        val snoozling = enabled(SNOOZLING_KEY)
        val noctilume = enabled(NOCTILUME_KEY)

        if (!snoozling && !noctilume) return emptyList()

        val gardenTime = GreenhouseGrid.dayOrNightNow()

        return notes { _, instance ->
            if (snoozling && instance.isAsleep) return@notes instance.cropDef.name to "asleep"

            if (!noctilume) return@notes null
            if (!instance.needsOtherTimeOfDay(gardenTime)) return@notes null

            val craving = instance.timeOfDayNeeded ?: return@notes null

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
            grid.layout.plants.mapNotNull { instance ->
                val remaining = instance.decayRemainingMs ?: return@mapNotNull null

                DecayingPlant(house, instance.cropDef.name, remaining)
            }
        }.sortedBy { it.remainingMs }

    // -------------------------------------------------------------------------- when they run

    /** Sends the harvest and attention warnings straight after a tick. Decay is on its own schedule. */
    @EventHandler
    fun onGrowthTick(event: GrowthTickEvent) {
        if (!GreenhousePresets.reminderTimeEnabled(GreenhousePresets.AT_TICK_KEY)) return

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

        tick(HARVEST, remainingMs)
        tick(ATTENTION, remainingMs)

        if (enabled(HARVEST_KEY)) {
            val notes = harvestNotes()

            if (notes.isNotEmpty() && shouldWarn(HARVEST, remainingMs, GreenhousePresets.reminderThresholds())) {
                send(
                    "Some plants are ready to harvest! Next tick in ${remainingMs.toShortDuration()}",
                    notes
                )
            }
        }

        val attention = attentionNotes()

        if (attention.isNotEmpty() && shouldWarn(ATTENTION, remainingMs, GreenhousePresets.reminderThresholds())) {
            send("Some plants need attention! Next tick in ${remainingMs.toShortDuration()}", attention)
        }
    }

    /** The one that counts down to a plant rotting, on its own ladder and its own soonest plant. */
    private fun decayWarnings() {
        if (!enabled(DECAY_KEY)) {
            tick(DECAY, NOTHING_PENDING_MS)
            return
        }

        val plants = decayingPlants()
        val soonest = plants.firstOrNull()?.remainingMs ?: NOTHING_PENDING_MS

        tick(DECAY, soonest)

        val rung = warnThreshold(DECAY, soonest, DECAY_THRESHOLDS) ?: return

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
        label: (GreenhouseGrid, Plant) -> Pair<String, String?>?
    ): List<HouseNote> = houses().mapNotNull { (grid, house) ->
        val counted = grid.layout.plants
            .mapNotNull { label(grid, it) }
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
            message.append(ChatUtils.gardenWarpLink())
        }

        Minecraft.getInstance().player?.sendSystemMessage(message)
    }

    private fun timeName(craving: Int): String =
        if (craving == StandReader.NEEDS_NIGHT) "Night" else "Day"

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
