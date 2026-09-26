package org.magic.magicaddons.features.farming.greenhousePresets.warnings

import java.time.Duration
import java.time.Instant
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.commands.internal.farming.TickReport
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.plot.MiscGreenhouseInfo
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.greenhouse.GrowthTickEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.OtherProfiles
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.BioanalysisAccessory
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.toShortDuration

object PlantWarnings {

    const val HARVEST_KEY: String = "ReadyToHarvestWarning"
    const val DECAY_KEY: String = "DecayWarning"
    const val SNOOZLING_KEY: String = "SnoozlingAsleepWarning"
    const val NOCTILUME_KEY: String = "NoctilumeTimeWarning"
    const val FLESHTRAP_KEY: String = "FleshtrapMeatWarning"
    const val FLESHTRAP_MEAT_KEY: String = "MeatThreshold"
    const val THUNDERLING_KEY: String = "ThunderlingChargeWarning"
    const val THUNDERLING_CHARGE_KEY: String = "ChargeThreshold"
    const val GLASSCORN_KEY: String = "GlasscornResetWarning"
    const val OTHER_PROFILES_KEY: String = "OtherProfileWarnings"
    const val THIRST_KEY: String = "DehydrationWarning"
    const val NEGATIVE_WATER_KEY: String = "AlsoOnNegative"

    private const val HOVER_LINES: Int = 5

    private val DECAY_WITHIN: Duration = Duration.ofDays(1)

    private val HARVEST_TIER_ORDER: List<CropTier> = listOf(
        CropTier.Legendary, CropTier.Epic, CropTier.Rare, CropTier.Uncommon, CropTier.Common,
        CropTier.BaseCrop, CropTier.RareCrop, CropTier.Other
    )

    private val remindersSentByProfile = mutableMapOf<String, MutableSet<Duration>>()

    private val JOIN_LINE_DELAY: Duration = Duration.ofSeconds(5)

    private class PendingTickLine(val ticks: Int, val sendAt: Instant)

    private var joinTickLine: PendingTickLine? = null

    class ProfileGreenhouses(val name: String?, val misc: MiscGreenhouseInfo, val grids: List<GreenhouseGrid>) {
        val key: String get() = name ?: ""
    }

    class Section(val label: String, val count: Int, val hoverHeading: String, val lines: List<Component>)

    fun activeProfile(): ProfileGreenhouses = ProfileGreenhouses(null, GreenhouseData.miscInfo, GreenhouseData.greenhouseGrids)

    fun profileNamed(name: String): ProfileGreenhouses? =
        OtherProfiles.profiles.firstOrNull { it.name == name }?.let { ProfileGreenhouses(it.name, it.misc, it.grids) }

    private fun warnedProfiles(): List<ProfileGreenhouses> {
        val own = listOf(activeProfile())
        if (!warningEnabled(OTHER_PROFILES_KEY)) return own

        return own + OtherProfiles.profiles.map { ProfileGreenhouses(it.name, it.misc, it.grids) }
    }

    private fun warningEnabled(key: String): Boolean = GreenhousePresets.warningTypeEnabled(key)

    @EventHandler
    fun onGrowthTick(event: GrowthTickEvent) {
        val profile = if (event.isActiveProfile) activeProfile() else event.profileName?.let { profileNamed(it) } ?: return
        if (!event.isActiveProfile && !warningEnabled(OTHER_PROFILES_KEY)) return

        remindersSentByProfile.remove(profile.key)

        if (event.isActiveProfile && GreenhouseData.joiningSkyBlock) {
            if (GreenhousePresets.warningsEnabled()) joinTickLine = PendingTickLine(event.ticks, Instant.now().plus(JOIN_LINE_DELAY))
            return
        }

        if (GreenhousePresets.reminderTimeEnabled(GreenhousePresets.AT_TICK_KEY)) sendTickLine(profile, event.ticks, nextTickIn = null)
    }

    fun onTick() {
        joinTickLine?.let { pending ->
            if (Instant.now().isBefore(pending.sendAt)) return@let
            joinTickLine = null
            sendTickLine(activeProfile(), pending.ticks, nextTickIn = null)
        }
        warnedProfiles().forEach { profile -> remindBeforeTick(profile) }
    }

    private fun remindBeforeTick(profile: ProfileGreenhouses) {
        val nextTick = profile.misc.nextTickTime ?: return
        val remainingMs = Duration.between(Instant.now(), nextTick).toMillis()
        val sent = remindersSentByProfile.getOrPut(profile.key) { mutableSetOf() }

        val crossed = GreenhousePresets.reminderThresholds().filter { remainingMs <= it.toMillis() }
        if (crossed.isEmpty() || crossed.all { it in sent }) return

        sent.addAll(crossed)

        val ticksPassed = profile.grids.maxOfOrNull { it.state.ticksSinceLastScan } ?: 0
        sendTickLine(profile, ticksPassed, nextTickIn = crossed.min())
    }

    private fun sendTickLine(profile: ProfileGreenhouses, ticksPassed: Int, nextTickIn: Duration?) {
        if (profile.name == null && GreenhouseData.inOwnGreenhouse()) return

        val sections = sections(profile)
        val inProfile = profile.name?.let { " in profile $it" } ?: ""

        val lines = mutableListOf<String>()
        when {
            ticksPassed == 1 -> lines += "Greenhouse tick has passed$inProfile!"
            ticksPassed > 1 -> lines += "$ticksPassed greenhouse ticks have passed$inProfile!"
        }
        nextTickIn?.let { lines += "Next tick in ${rungText(it)}${if (lines.isEmpty()) inProfile else ""}!" }
        if (lines.isEmpty()) return

        val message = ChatUtils.buildWithPrefix(Component.literal(lines.joinToString("\n")).withStyle(ChatFormatting.YELLOW))

        if (sections.isNotEmpty()) {
            message.append(Component.literal(" "))
            message.append(infoButton(profile))
        }

        Minecraft.getInstance().player?.sendSystemMessage(message)
    }

    private fun rungText(rung: Duration): String {
        val minutes = rung.toMinutes()

        return if (minutes == 1L) "1 minute" else "$minutes minutes"
    }

    private fun infoButton(profile: ProfileGreenhouses): Component {
        val command = buildString {
            append("/${MainInternal.PATH} ${TickReport.NAME}")
            profile.name?.let { append(" $it") }
        }

        return ChatUtils.buildStyled(
            "[INFO]",
            ChatFormatting.GREEN,
            Component.literal("Click for details"),
            ClickEvent.RunCommand(command),
        )
    }

    fun sendReport(profile: ProfileGreenhouses) {
        val sections = sections(profile)

        val message = ChatUtils.buildWithPrefix(Component.literal("Greenhouse report:").withStyle(ChatFormatting.YELLOW))

        if (sections.isEmpty()) {
            message.append(Component.literal(" nothing to report.").withStyle(ChatFormatting.GRAY))
            Minecraft.getInstance().player?.sendSystemMessage(message)
            return
        }

        sections.forEach { section ->
            message.append(Component.literal("\n  "))
            message.append(
                ChatUtils.buildStyled("${section.label}: ${section.count}", ChatFormatting.AQUA, hoverText(section))
            )
        }

        Minecraft.getInstance().player?.sendSystemMessage(message)
    }

    private fun hoverText(section: Section): Component {
        val text = Component.literal(section.hoverHeading).withStyle(ChatFormatting.GRAY)

        section.lines.take(HOVER_LINES).forEach { line ->
            text.append(Component.literal("\n"))
            text.append(line)
        }
        val more = section.lines.size - HOVER_LINES
        if (more > 0) text.append(Component.literal("\nAnd $more more...").withStyle(ChatFormatting.GRAY))

        return text
    }

    private fun sections(profile: ProfileGreenhouses): List<Section> = listOfNotNull(
        decaySection(profile),
        harvestSection(profile),
        attentionSection(profile),
        waterSection(profile)
    )

    // ------------------------------------------------------------------------- the sections

    private const val WATER_LABEL: String = "Water"

    private class DecayingPlant(val greenhouse: String, val plant: String, val remainingMs: Long)

    private fun decaySection(profile: ProfileGreenhouses): Section? {
        if (!warningEnabled(DECAY_KEY)) return null

        val decaying = profile.grids.flatMap { grid ->
            grid.layout.plants.mapNotNull { plant ->
                plant.decayRemainingMs?.let { DecayingPlant(grid.layout.displayName(), plant.cropDef.name, it) }
            }
        }.sortedBy { it.remainingMs }

        val soonest = decaying.firstOrNull() ?: return null
        if (soonest.remainingMs > DECAY_WITHIN.toMillis()) return null

        val lines = decaying.map { decayingPlant ->
            Component.literal(decayingPlant.plant).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(decayingPlant.greenhouse).withStyle(ChatFormatting.AQUA))
                .append(
                    if (decayingPlant.remainingMs <= 0) Component.literal(" - decayed").withStyle(ChatFormatting.DARK_RED)
                    else Component.literal(" - ${decayingPlant.remainingMs.toShortDuration()}").withStyle(ChatFormatting.WHITE)
                )
        }

        return Section("Decaying plants", decaying.size, "Decaying soonest:", lines)
    }

    private fun harvestSection(profile: ProfileGreenhouses): Section? {
        if (!warningEnabled(HARVEST_KEY)) return null

        val onlyTargets = GreenhousePresets.harvestHighlightOnlyTargets()
        val harvestable = profile.grids.flatMap { grid ->
            grid.layout.plants.filter { plant ->
                GreenhousePresets.isHarvestable(plant) && (!onlyTargets || onWantedTarget(grid, plant))
            }
        }
        if (harvestable.isEmpty()) return null

        val counted = harvestable.groupingBy { it.cropDef }.eachCount()
        val lines = counted.entries
            .sortedWith(compareBy<Map.Entry<CropDefinition, Int>> { HARVEST_TIER_ORDER.indexOf(it.key.tier) }.thenByDescending { it.value })
            .map { (crop, count) -> countedLine(count, crop.name) }

        return Section("Harvestable", harvestable.size, "Ready to harvest:", lines)
    }

    private fun onWantedTarget(grid: GreenhouseGrid, plant: Plant): Boolean {
        val plan = grid.state.assignedLayout?.turnedBy(grid.state.planTurns) ?: return false
        val footprint = plant.cropDef.footprint

        return plan.plants.any { target ->
            target.slot.mark == LayoutSlot.Marking.Target &&
                    target.acceptsCrop(plant.cropDef) &&
                    target.slot.x in plant.slot.x until plant.slot.x + footprint.width &&
                    target.slot.y in plant.slot.y until plant.slot.y + footprint.height
        }
    }

    private fun attentionSection(profile: ProfileGreenhouses): Section? {
        val gardenTime = GreenhouseGrid.dayOrNightNow()
        val meatThreshold = GreenhousePresets.fleshtrapMeatThreshold()
        val chargeThreshold = GreenhousePresets.thunderlingChargeThreshold()
        val plants = profile.grids.flatMap { it.layout.plants }

        val counts = linkedMapOf<String, Int>()
        fun count(label: String, enabledKey: String, matches: (Plant) -> Boolean) {
            if (!warningEnabled(enabledKey)) return
            val found = plants.count(matches)
            if (found > 0) counts[label] = found
        }

        count("Fleshtrap starving", FLESHTRAP_KEY) { it.cropDef.hasHungerBar && it.hunger == 0 }
        count("Fleshtrap needs meat", FLESHTRAP_KEY) { it.cropDef.hasHungerBar && (it.hunger ?: 100) in 1..meatThreshold }
        count("Noctilume time change", NOCTILUME_KEY) { it.needsOtherTimeOfDay(gardenTime) }
        count("Snoozling asleep", SNOOZLING_KEY) { it.isAsleep && it.cropDef.name == "Snoozling" }
        count("Jerryflower asleep", SNOOZLING_KEY) { it.isAsleep && it.cropDef.name == "Jerryflower" }
        count("Thunderling near overload", THUNDERLING_KEY) { it.cropDef.chargeRule != null && !it.isPlacedMutation && it.charge >= chargeThreshold }
        count("Glasscorn about to reset", GLASSCORN_KEY) { it.cropDef.resetsToFirstStage && it.highestStage == it.cropDef.maxStage }

        if (warningEnabled(THUNDERLING_KEY)) {
            val destroyed = profile.grids.sumOf { it.state.thunderlingsDestroyed }
            if (destroyed > 0) counts["Thunderling destroyed"] = destroyed
        }
        if (warningEnabled(GLASSCORN_KEY)) {
            val reset = profile.grids.sumOf { it.state.glasscornsReset }
            if (reset > 0) counts["Glasscorn reset"] = reset
        }

        val chorusCollisions = chorusCollisions(profile)
        if (chorusCollisions > 0) counts["Chorus collision"] = chorusCollisions

        if (counts.isEmpty()) return null

        val lines = counts.map { (label, count) ->
            Component.literal(label).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" x$count").withStyle(ChatFormatting.WHITE))
        }

        return Section("Attention plants", counts.values.sum(), "Needs attention:", lines)
    }

    private fun chorusCollisions(profile: ProfileGreenhouses): Int {
        if (!warningEnabled(GreenhousePresets.CHORUS_KEY)) return 0

        val ticks = GreenhousePresets.chorusAbsenceTicks() ?: return 0

        return profile.grids.count { grid ->
            ChorusCollision.reportFor(grid, ticks, BioanalysisAccessory.mutationWeightMultiplier())?.needsWarning == true
        }
    }

    private enum class Thirst(val label: String, val color: ChatFormatting) {
        PresumedDead("presumed dead", ChatFormatting.DARK_RED),
        DyingNextTick("dying next tick", ChatFormatting.RED),
        OutOfWater("out of water", ChatFormatting.GOLD)
    }

    private class ThirstyPlant(val plant: Plant, val greenhouse: String, val thirst: Thirst)

    private fun waterSection(profile: ProfileGreenhouses): Section? {
        if (!warningEnabled(THIRST_KEY)) return null

        val warnNegative = GreenhousePresets.negativeWaterWarningEnabled()

        val thirsty = profile.grids.flatMap { grid ->
            grid.layout.plants.mapNotNull { plant ->
                if (!plant.consumesWater) return@mapNotNull null

                val water = plant.waterLevel ?: return@mapNotNull null
                if (water <= PlotPrediction.WATER_DEATH_LEVEL) return@mapNotNull ThirstyPlant(plant, grid.layout.displayName(), Thirst.PresumedDead)

                val effect = GreenhouseGrid.waterEffectAt(grid.layout, plant.slot)
                val ticksLeft = PlotPrediction.ticksUntilDeath(water, effect) ?: return@mapNotNull null

                when {
                    ticksLeft <= 1 -> ThirstyPlant(plant, grid.layout.displayName(), Thirst.DyingNextTick)
                    water < 0 && warnNegative -> ThirstyPlant(plant, grid.layout.displayName(), Thirst.OutOfWater)
                    else -> null
                }
            }
        }
        if (thirsty.isEmpty()) return null

        val lines = thirsty
            .groupingBy { Triple(it.thirst, it.plant.cropDef.name, it.greenhouse) }
            .eachCount()
            .entries
            .sortedWith(compareBy<Map.Entry<Triple<Thirst, String, String>, Int>> { it.key.first }.thenByDescending { it.value })
            .map { (key, count) ->
                val (thirst, crop, greenhouse) = key
                countedLine(count, crop)
                    .append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(greenhouse).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" - ${thirst.label}").withStyle(thirst.color))
            }

        return Section(WATER_LABEL, thirsty.size, "$WATER_LABEL:", lines)
    }

    private fun countedLine(count: Int, crop: String): MutableComponent =
        Component.literal("x$count ").withStyle(ChatFormatting.WHITE)
            .append(Component.literal(crop).withStyle(ChatFormatting.YELLOW))

}
