package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.data.greenhouse.LayoutSlot
import tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileAPI
import org.magic.magicaddons.data.greenhouse.MasterLayout
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.commands.debug.CropCollector
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.util.getBuildableArea
import org.magic.magicaddons.util.parseDurationToMs
import org.magic.magicaddons.util.center
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.ClickEvent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.debug.FarmingDebug
import org.magic.magicaddons.data.greenhouse.GREENHOUSE_SOIL_Y
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.toFunctionString
import org.magic.magicaddons.data.greenhouse.elements.FireElement
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.OnSystemChatEvent
import org.magic.magicaddons.events.greenhouse.GrowthTickEvent
import org.magic.magicaddons.events.greenhouse.PlotChangedEvent
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.ui.widgets.config.SettingDetail
import org.magic.magicaddons.util.ServerUtils
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.extentions.isSkyblockFiller
import net.minecraft.network.chat.Style
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import java.util.Optional
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.abs

object GreenhouseData {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    private const val BUILD_OFFSET = 43
    private const val GRID_SIZE = 10

    var lastPlot: Plot? = null

    /** When the garden last loaded around the player, the earliest a spawned mutation can be from. */
    private var gardenArrivedAt: Instant? = null

    var checkGreenhouses = false
    var greenhousesInitialized = false
    var greenhouseGrids = mutableListOf<GreenhouseGrid>()
    var presetGrids = mutableListOf<MasterLayout>()

    /** Every plot of every preset, the things a greenhouse can be assigned. */
    fun allPlots(): List<GreenhouseLayout> = presetGrids.flatMap { it.plots }

    fun masterOf(plot: GreenhouseLayout): MasterLayout? = presetGrids.find { plot in it.plots }

    /** A plot as the player knows it: the preset's name, and which plot when the preset has several. */
    fun describe(plot: GreenhouseLayout): String {
        val master = masterOf(plot) ?: return plot.displayName()
        return if (master.plots.size > 1) "${master.plotTitle(plot)} of ${master.displayName()}" else master.displayName()
    }
    var miscInfo = MiscGreenhouseInfo()
    var lastGridLayouts = mutableListOf<GreenhouseLayout>()

    var currentPreset: MasterLayout? = null
    var currentGridIndex: Int = 0

    var lastCheckTime: Instant? = null
    var lastServerTick: Long? = null

    var removedElementByAttack: ElementRuntimeState? = null
    /** One crop the player put down, until the plot confirms it or the window runs out. */
    private class Placement(val def: CropDefinition, val pos: BlockPos, val at: Instant)

    /** Every placement still waiting on the plot: several go down in a row faster than a scan. */
    private val placements = mutableListOf<Placement>()

    /**
     * Every crop put down this session, by the soil block under it. A placed mutation with no
     * placed look recorded matches nothing, so this is how the scan still knows what stands there.
     */
    private val placedHere = mutableMapOf<BlockPos, CropDefinition>()

    /** The crop the player put down on the soil at [soil] this session, if any. */
    fun placedHereAt(soil: BlockPos): CropDefinition? = placedHere[BlockPos(soil.x, GREENHOUSE_SOIL_Y, soil.z)]

    /** How long a placement the server never confirmed is still worth waiting for. */
    private val PLACE_WINDOW: Duration = Duration.ofSeconds(5)


    val elementsBySoil: Map<Block, List<CropDefinition>> =
        CropRegistry.all
            .flatMap { definition ->
                definition.requiredSoil.map { soil ->
                    soil to definition
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )



    private var plantDiagnosticHitBaseBlock: BlockPos? = null
    private var plantDiagnosticListeningElement: ElementRuntimeState? = null
    // ^^ temp


    private fun initKnownIds() {
        if (checkGreenhouses) return
        if (DataHandler.activeProfile == null) return
        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach { plot ->
            if (plot.data?.isGreenhouse != true) return@forEach
            val existingGrid = greenhouseGrids.find { "plot_${plot.id}" == it.layout.id }
            existingGrid ?: run {
                val gridLayout = GreenhouseLayout(
                    id = "plot_${plot.id}"
                )
                val gridState = GreenhouseGrid.GridState(
                    null,
                    true,
                    null,
                    false
                )

                val grid = GreenhouseGrid(gridState, gridLayout)
                grid.plot = plot
                greenhouseGrids.add(grid)
                return@forEach
            }
            existingGrid.plot = plot
        }
        greenhousesInitialized = true
        checkGreenhouses = true
    }

    private fun scanGridData() {
        if (!greenhousesInitialized) return
        val plot = PlotAPI.getCurrentPlot() ?: return

        val grid = getCurrentGrid() ?: return
        if (grid.state.hasRuntimeReferences && !grid.state.needsUpdate) return

        grid.plot = plot

        grid.createSlotDataForGrid()

        // a merge, so whatever the plot cannot say for a plant that is still there is carried over,
        // and any stage predicted while away is corrected by what is actually standing
        grid.setPlantData()

        claimPlantedCrop(grid)

        // the plan on screen is read off the plot, so it is only right until the plot changes
        LayoutRenderState.refresh()

        // after grid update
        grid.state.hasRuntimeReferences = true
        grid.state.needsUpdate = false
        grid.state.lastUpdateTimestamp = Instant.now()
        grid.state.pendingGrowthTicks = 0
    }


    /** The most one look at the clock may move the next tick by, so a wrong guess cannot walk it away. */
    private const val MAX_TICK_ADJUSTMENT_MS: Long = 5_000

    /** The hunting shard carrying greenhouse speed, a legendary one, twenty four syphons to max. */
    const val GREENHOUSE_SPEED_ATTRIBUTE_ID: String = "attribute:l57"

    /** Whether the plot changed since the last look. Scans are cheap enough to run on the change itself. */
    private var reconcileWanted: Boolean = false

    /** Something in the plot changed, so what is stored for it can no longer be trusted. */
    fun requestReconcile() {
        reconcileWanted = true
    }

    /** Runs a reconcile if anything has asked for one since the last tick. */
    private fun runDueReconcile() {
        if (!reconcileWanted) return

        reconcileWanted = false

        getCurrentGrid()?.state?.needsUpdate = true
        scanGridData()
    }

    /** The last dehydration warning sent while away from the garden, with the time it was sent. */
    private var awayWarning: Pair<Instant, DyingPlant>? = null

    /** A teleport to a dying plant's plot, offered once the garden has finished loading. */
    private var teleportOffer: DyingPlant? = null
    private var teleportOfferAt: Instant? = null

    /** How soon after a warning returning to the garden still counts as a response to it. */
    private val TELEPORT_OFFER_WINDOW: Duration = Duration.ofSeconds(15)

    private const val DEHYDRATION: String = "dehydration"
    private const val CHORUS_COLLISION: String = "chorus-collision"

    /** How many growth ticks the player says they will be away for. */
    private fun absenceTicks(): Int? = GreenhousePresets.chorusAbsenceTicks()

    /**
     * The chosen ticks as time off, for the line under the setting. A span one tick wide, since the
     * tick already running is part spent, and it slides as the countdown runs.
     */
    fun absenceDetail(): SettingDetail? {
        val ticks = absenceTicks() ?: return null

        val tickMs = currentGrowthTickMs()
        val remaining = remainingTickMs()

        if (tickMs == null || remaining == null) {
            return SettingDetail.Text(
                "(Missing variables, Cannot resolve tick time.)",
                MISSING_COLOR
            )
        }

        val shortest = remaining + (ticks - 1) * tickMs
        val longest = remaining + ticks * tickMs

        return SettingDetail.Text(
            "(absent for between ${spanText(shortest)} - ${spanText(longest)})"
        )
    }

    /** The colour of the detail line when a value it needs is missing. */
    private const val MISSING_COLOR: Int = 0xFFFF8855.toInt()

    /** A length of time as the player reads one: "2d 3h", "9h 40m", "54m", "30s". */
    fun spanText(ms: Long): String {
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

    /** Which greenhouses run out of room for their chorus before the player is next back. */
    private fun warnOfChorusCollision() {
        if (!GreenhousePresets.warningType(GreenhousePresets.CHORUS_KEY)) return

        val nextTick = miscInfo.nextTickTime ?: return
        val remainingMs = Duration.between(Instant.now(), nextTick).toMillis()

        GreenhouseWarnings.tick(CHORUS_COLLISION, remainingMs)

        val ticks = absenceTicks() ?: return

        val crowded = greenhouseGrids.mapNotNull { grid ->
            ChorusCollision.analyse(grid, ticks)
                ?.takeIf { it.warns }
                ?.let { grid.layout to it }
        }

        if (crowded.isEmpty()) return
        if (!GreenhouseWarnings.shouldWarn(CHORUS_COLLISION, remainingMs)) return

        sendChorusWarning(crowded)
    }

    /** What to break, where, and why. A growing jellybean is named when there is one to lose. */
    private fun sendChorusWarning(crowded: List<Pair<GreenhouseLayout, ChorusCollision.Report>>) {
        val message = Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)
            .append(
                Component.literal("Chorus collision likely: ").withStyle(ChatFormatting.RED)
            )

        crowded.forEachIndexed { index, (layout, report) ->
            if (index > 0) {
                message.append(Component.literal("; ").withStyle(ChatFormatting.DARK_GRAY))
            }

            message.append(
                Component.literal("break ${report.cull} youngest chorus")
                    .withStyle(ChatFormatting.YELLOW)
            )
            message.append(
                Component.literal(" (or harvest ${report.cull * 2} ripe)")
                    .withStyle(ChatFormatting.GRAY)
            )
            message.append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))

            // the numbers behind the verdict hang off the greenhouse's own name, so several
            // greenhouses in one warning each keep their own working
            val detail = Component.literal(
                "${report.movers} moving chorus, ${report.free} free tiles, " +
                        "${report.spawnOpen} open spawners over ${report.ticks} ticks away.\n" +
                        "Margin ${report.margin} plus ${report.ripening} ripening is under the " +
                        "${report.need} the window asks for." +
                        if (report.jelliesAtRisk > 0) {
                            "\n${report.jelliesAtRisk} growing jellybeans stand in the blast radius."
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
    private fun warnOfDyingPlants() {
        val nextTick = miscInfo.nextTickTime ?: return
        val remainingMs = Duration.between(Instant.now(), nextTick).toMillis()

        GreenhouseWarnings.tick(DEHYDRATION, remainingMs)

        val dying = mutableListOf<DyingPlant>()

        greenhouseGrids.forEach { grid ->
            grid.layout.elementInstances.forEach { instance ->
                if (!instance.needsWater) return@forEach

                val water = instance.waterLevel ?: return@forEach
                if (water <= WaterModel.DEATH) return@forEach

                // a plant that has finished growing stopped drinking, so nothing kills it
                val lowestStage = when (val stage = instance.growthStage) {
                    is GrowthStageInfo.Known -> stage.stage
                    is GrowthStageInfo.Estimated -> stage.range.first
                    null -> null
                }
                if (lowestStage != null && lowestStage >= instance.cropDef.maxStage) return@forEach

                val effect = grid.layout.waterEffectAt(instance.slot)
                val ticksLeft = WaterModel.ticksUntilDeath(water, effect) ?: return@forEach

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
        if (!GreenhouseWarnings.shouldWarn(DEHYDRATION, remainingMs)) return

        sendDehydrationWarning(dying, remainingMs)
    }

    /** A plant found alive past its predicted death: said now, at whatever the countdown reads. */
    fun warnSurvivor(plant: DyingPlant) {
        val remaining = miscInfo.nextTickTime
            ?.let { Duration.between(Instant.now(), it).toMillis().coerceAtLeast(0) }
            ?: 0

        sendDehydrationWarning(listOf(plant), remaining)
    }

    private fun shortDuration(ms: Long): String {
        val seconds = (ms / 1000).coerceAtLeast(0)

        return if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
    }

    /** The warning itself, plants grouped by greenhouse, with a way home when away. */
    private fun sendDehydrationWarning(dying: List<DyingPlant>, remainingMs: Long) {
        val byHouse = dying.groupBy({ it.greenhouse }, { it.plant })

        val message = Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)
            .append(
                Component.literal("Dying of thirst in ${shortDuration(remainingMs)}: ")
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
            message.append(
                Component.literal("[GARDEN]").withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(ClickEvent.RunCommand("/warp garden"))
                        .withHoverEvent(
                            HoverEvent.ShowText(Component.literal("Click here to warp to garden!"))
                        )
                )
            )

            // remembered so returning to the garden can be answered with a teleport to the plot
            awayWarning = Instant.now() to dying.first()
        }

        Minecraft.getInstance().player?.sendSystemMessage(message)
    }

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        runDueReconcile()
    }

    /** Whether the player stands in a greenhouse, their own or one they are visiting. */
    fun inGreenhouse(): Boolean = PlotAPI.getCurrentPlot()?.takeUnless { it.isBarn } != null

    fun getCurrentGrid(): GreenhouseGrid? {
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return null
        return greenhouseGrids.find { it.layout.id == "plot_$plotId" }
    }
    fun computeNextAvailableId(): Int {
        val usedIds = presetGrids
            .mapNotNull {
                it.id.removePrefix("preset_").toIntOrNull()
            }
            .toSet()

        var nextId = 1

        while (nextId in usedIds) {
            nextId++
        }

        return nextId
    }

    /**
     * How far the server may fall behind before the gap reads as an absence. A stalled server still
     * sends ticks; one the client is disconnected from sends none.
     */
    private val AWAY_THRESHOLD: Duration = Duration.ofSeconds(20)

    fun checkForUpdate() {
        if (!greenhousesInitialized) return

        // only the values the clock actually needs stop it. Warning about them is the screen's job,
        // not something to do from inside a check that runs every tick
        if (miscInfo.cropGrowthValue == null || miscInfo.cropSpeedUpgradeValue == null) return

        val nextTick = miscInfo.nextTickTime ?: return

        val growthTickMs = computeGrowthStageTimeMs(
            getCurrentUniques().size,
            miscInfo.cropGrowthValue!!,
            miscInfo.cropSpeedUpgradeValue!!,
            greenhouseSpeedAttribute() ?: 0
        )

        val now = Instant.now()

        val onlineTickTracking =
            LocationAPI.island == SkyBlockIsland.GARDEN &&
                    !LocationAPI.isGuest &&
                    lastCheckTime != null

        val overdueMs = if (onlineTickTracking) {
            val currentTick = ServerUtils.totalServerTicks
            val previousTick = lastServerTick

            lastServerTick = currentTick

            // leaving without updating lastCheckTime makes the next call measure real time across
            // the whole gap against server time from the last moment only
            if (previousTick == null) {
                lastCheckTime = now
                return
            }

            val passedServerTicks = currentTick - previousTick
            if (passedServerTicks <= 0) {
                lastCheckTime = now
                return
            }

            val lastCheck = lastCheckTime ?: run {
                lastCheckTime = now
                return
            }

            val serverMs = passedServerTicks * 50L
            val realMs = now.toEpochMilli() - lastCheck.toEpochMilli()

            // a stalled server still sends ticks so its time nearly keeps up; an absence arrives as
            // unaccounted time and must not be handed to the countdown as lag
            val unaccountedMs = realMs - serverMs

            if (Duration.ofMillis(unaccountedMs) > AWAY_THRESHOLD) {
                lastCheckTime = now
                return
            }
            // bounded: this nudges a drifted countdown, so one long pause cannot move it further
            // than the gap it is measuring
            val adjustmentDelta = unaccountedMs
                .coerceIn(-MAX_TICK_ADJUSTMENT_MS, MAX_TICK_ADJUSTMENT_MS)

            // added, not taken off: time the server spent behind is time the tick has not served
            // yet. Taking it off ran the screen ahead of the game
            miscInfo.nextTickTime =
                miscInfo.nextTickTime!!.plusMillis(adjustmentDelta)
            lastCheckTime = now

            now.toEpochMilli() -
                    miscInfo.nextTickTime!!.toEpochMilli()


        } else {
            lastCheckTime = now
            now.toEpochMilli() - nextTick.toEpochMilli()
        }

        if (overdueMs <= 0) return
        val passedGrowthTicks = (overdueMs / growthTickMs)

        if (passedGrowthTicks <= 0 && !nextTick.isBefore(now)) return

        // the countdown running out is itself a tick, and passedGrowthTicks only counts whole
        // periods since then. The clock knew this and the plants did not, so one tick went untold
        val elapsedTicks = passedGrowthTicks.toInt() + 1
//        Common.LOGGER.info("Overdue ms $overdueMs")
//        Common.LOGGER.info("Overdue growth ticks $passedGrowthTicks")
        val nextTickAdvance = (passedGrowthTicks + 1) * growthTickMs
//        Common.LOGGER.info("Next tick advance ms $nextTickAdvance")
//        Common.LOGGER.info("Previous tick ${miscInfo.nextTickTime}")
//        Common.LOGGER.info("Next tick ${miscInfo.nextTickTime!!.plusMillis((nextTickAdvance))}")
//        Common.LOGGER.info("Now $now") //hopefully dont need anymore
        miscInfo.nextTickTime =
            miscInfo.nextTickTime!!.plusMillis(
                nextTickAdvance
            )

        greenhouseGrids.forEach { grid ->
            if (onlineTickTracking && !grid.hasRuntime()) return@forEach

            // a greenhouse not read this session has no pending count yet, which is no reason to
            // skip it: these are exactly the ticks nobody was there to see
            val pendingTicks = grid.state.pendingGrowthTicks ?: 0

            grid.state.pendingGrowthTicks = pendingTicks + elapsedTicks
            grid.state.needsUpdate = true

            // nobody is looking at this greenhouse, so the clock is all we have to go on
            grid.predictGrowth(elapsedTicks, growthTickMs)
        }

        // posted after every plant has been moved on, so a listener reads the garden as it now
        // stands. An absence arrives as one event carrying all of its ticks rather than as a burst
        EventBus.post(GrowthTickEvent(elapsedTicks, growthTickMs))
    }

    /** Loads the profile the game says is being played, the first time and on every switch. */
    private fun ensureProfile() {
        if (!ProfileAPI.isLoaded) return
        val id = runCatching { ProfileAPI.profileId }.getOrNull() ?: return
        DataHandler.switchProfile(id, ProfileAPI.profileName ?: return)
    }

    /** Forgets what was learned about the last profile's garden, so the new one is read afresh. */
    fun resetForProfile() {
        checkGreenhouses = false
        currentPreset = null
        lastCheckTime = null
        lastServerTick = null
        gardenArrivedAt = null
        regenRender()
    }

    @Subscription
    fun onTick(event: TickEvent) {
        ensureProfile()
        if (DataHandler.activeProfile == null) return
        OtherProfiles.advance()

        val now = Instant.now()
        val last = lastCheckTime

        // the game may already be on the garden when the mod starts, which no island change reports
        if (gardenArrivedAt == null && LocationAPI.island == SkyBlockIsland.GARDEN) gardenArrivedAt = now
        if (
            last == null ||
            last.plusSeconds(60).isBefore(now) ||
            miscInfo.nextTickTime?.isBefore(now) ?: false
        ) {
            checkForUpdate()
        }

        // asked every tick rather than once a minute, so each threshold fires the moment it
        // is crossed rather than up to a minute late
        warnOfDyingPlants()
        warnOfChorusCollision()
        PlantWarnings.onTick()
        offerTeleportIfArrived()
    }

    /** The promised ride to the dying plant, sent once the garden is loaded and still current. */
    private fun offerTeleportIfArrived() {
        val offer = teleportOffer ?: return
        val at = teleportOfferAt ?: return

        if (Instant.now().isBefore(at)) return
        if (Minecraft.getInstance().level == null) return

        teleportOffer = null

        if (LocationAPI.island != SkyBlockIsland.GARDEN || LocationAPI.isGuest) return

        val plotNumber = offer.plotId.removePrefix("plot_")

        val message = Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)
            .append(
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


    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        // returning to the garden just after a dehydration warning is treated as a response to it,
        // so a teleport to the plot is offered two seconds later, once the world has loaded
        if (event.new == SkyBlockIsland.GARDEN) {
            gardenArrivedAt = Instant.now()

            awayWarning?.let { (at, plant) ->
                if (Duration.between(at, Instant.now()) <= TELEPORT_OFFER_WINDOW) {
                    teleportOffer = plant
                    teleportOfferAt = Instant.now().plusSeconds(2)
                }
            }
            awayWarning = null
        }

        if (event.new != SkyBlockIsland.GARDEN) {
            DataHandler.saveGardenData()
            greenhouseGrids.forEach {
                it.state.hasRuntimeReferences = false
            }
            EventBus.post(PlotChangedEvent(lastPlot,null))
            lastPlot = null
        }
        checkForUpdate()
    }

    @Subscription
    fun onGameShutdown(event: ServerDisconnectEvent) {
        DataHandler.saveGardenData()
    }


    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (lastPlot != PlotAPI.getCurrentPlot()) {
            EventBus.post(PlotChangedEvent(lastPlot,PlotAPI.getCurrentPlot()))
            lastPlot = PlotAPI.getCurrentPlot()
        }
    }

    @EventHandler
    fun onPlotChanged(event: PlotChangedEvent) {
        if (!baseSetting.value) return
        initKnownIds()
        scanGridData()
        regenRender()
    }

    /** Takes the plan off the greenhouse being stood in, for both the button and the chat word. */
    fun unplanCurrentGreenhouse(): Boolean {
        val grid = getCurrentGrid() ?: run {
            ChatUtils.sendWithPrefix("Not standing in a greenhouse.")
            return false
        }

        return unplanGreenhouse(grid)
    }

    /**
     * Takes the plan off one particular greenhouse: the screen means whichever was picked from its
     * selector, not the one being stood in.
     */
    fun unplanGreenhouse(grid: GreenhouseGrid): Boolean {
        if (grid.state.assignedLayout == null) {
            ChatUtils.sendWithPrefix("No planner running on ${grid.layout.displayName()}.")
            return false
        }

        grid.state.assignedLayout = null
        grid.state.completionMuted = false

        regenRender()

        ChatUtils.sendWithPrefix("Planner stopped on ${grid.layout.displayName()}")

        return true
    }

    fun regenRender(){
        LayoutRenderState.show()
    }


    @Subscription
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onInventory(event: ContainerInitializedEvent) {
        val realItems = event.containerItems.filter { !it.isSkyblockFiller() }
        if (event.title == "Crop Diagnostics") {
            getDiagnosesData(realItems)
            plantDiagnosticListeningElement = null
            plantDiagnosticHitBaseBlock = null
            return
        }
        plantDiagnosticListeningElement = null
        plantDiagnosticHitBaseBlock = null
        if (event.title == "Desk") {
            updateCropGrowth(realItems)
            return
        }

        if (event.title == "Greenhouse Upgrades") {
            updateUpgrades(realItems)
        }

    }


    @EventHandler
    fun onBlockBreak(event: OnBlockDestroyedEvent) {
        regenRender()

        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return


        val pos = event.pos
        val blockCenter = Vec3.atCenterOf(pos)

        if (grid.plot?.aabb?.contains(blockCenter) != true) return

        val slot = grid.getSlotAt(pos, false) ?: return
        if (event.pos.y == GREENHOUSE_SOIL_Y) {
            slot.placedBlock = Blocks.AIR.defaultBlockState()
        } else {
            grid.removeMatchingBlock(pos)
        }

        requestReconcile()
    }

    @EventHandler
    fun onBlockPlaced(event: OnBlockPlacedEvent) {
        regenRender()

        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val blockVec3 = Vec3.atCenterOf(event.pos)
        if (grid.plot?.aabb?.contains(blockVec3) != true) return

        requestReconcile()
    }

    @EventHandler
    fun onEntityAdded(event: OnEntityAdded) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val gridArea = grid.plot?.getBuildableArea() ?: return

        // entities are reported for the whole world at once, so what matters is whether any of
        // them turned up in this plot, not whether all of them did
        if (event.addedEntityList.none { gridArea.contains(it.entity.position()) }) return

        requestReconcile()
    }

    @EventHandler
    fun onBlockUpdated(event: OnBlockChangedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val gridArea = grid.plot?.getBuildableArea() ?: return
        if (!gridArea.contains(event.packet.pos.center())) return
        val slot = grid.getSlotAt(event.packet.pos, false) ?: return

        if (gridArea.contains(event.packet.pos.center())) {
            if (event.packet.pos.y == 74) {

                if (event.packet.blockState.block == Blocks.FIRE) {
                    val alreadyHasFire = grid.elements.any {
                        it.instance.slot == slot && it.instance.cropDef.name == "Fire"
                    }
                    if (!alreadyHasFire) {
                        val fireRuntime = FireElement.getFireAtSlot(
                            slot,
                            mapOf(event.packet.pos to event.packet.blockState)
                        )
                        grid.elements.add(fireRuntime)
                    }
                    return
                }
            }
        }

        if (event.packet.pos.y != GREENHOUSE_SOIL_Y) return
        slot.placedBlock = event.packet.blockState

        requestReconcile()
    }


    /**
     * A plant is broken by hitting stands, not blocks, so the block listener never fires for it.
     * Waited for rather than read on the swing, which looked at a plot the crop still stood in.
     */
    @EventHandler
    fun onEntityRemoved(event: OnEntityRemoved) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val area = grid.plot?.getBuildableArea() ?: return
        if (event.removedEntityList.none { area.contains(it.entity.position()) }) return

        requestReconcile()
    }

    @EventHandler
    fun onAttackEntity(event: OnAttackEntityEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return

        val area = grid.plot?.getBuildableArea() ?: return
        if (!area.contains(event.target.position())) return

        requestReconcile()
    }

    @EventHandler
    fun onInteractEntity(event: OnInteractEntityEvent) {
        val entityBlockPos = BlockPos.containing(event.target.position())
        plantDiagnosticHitBaseBlock = BlockPos(entityBlockPos.x, 73, entityBlockPos.z)
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return
        val standTarget = event.target as? ArmorStand ?: return
        if (mainHandId.id == "item:plant_diagnostics_tool") {
            setDiagnosesListeningElement(null, standTarget, grid)
            return
        }
    }


    @EventHandler
    fun onItemUse(event: OnUseEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        if (GreenhouseWatering.startWateringWindow(mainHandId)) return
    }

    @EventHandler
    fun onBlockUse(event: OnBlockUseEvent) {
        plantDiagnosticHitBaseBlock = BlockPos(event.hit.blockPos.x, 73, event.hit.blockPos.z)
        val grid = getCurrentGrid() ?: return
        if (!grid.hasRuntime()) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        val foundCrop = CropRegistry.get(mainHandId.id)

        if (mainHandId.id == "item:plant_diagnostics_tool") {
            setDiagnosesListeningElement(event.hit.blockPos, null, grid)

            return
        }
        if (GreenhouseWatering.startWateringWindow(mainHandId)) return

        if (foundCrop != null) {
            val aimed = event.hit.blockPos.relative(event.hit.direction)

            // the game puts a three by three down centred on the block aimed at and a two by two
            // with that block as its north-west corner, so the plant's own corner is a step back
            // for every two of width beyond one
            val footprint = foundCrop.footprint
            val pos = aimed.offset(-((footprint.width - 1) / 2), 0, -((footprint.height - 1) / 2))

            placements.add(Placement(foundCrop, pos, Instant.now()))
            placedHere[BlockPos(pos.x, GREENHOUSE_SOIL_Y, pos.z)] = foundCrop
            requestReconcile()

            return
        }


    }


    fun warnUnknownValues(sendWarning: Boolean = true): Boolean {
        // the numbers only matter once there is a greenhouse for them to time
        if (greenhouseGrids.isEmpty() && PlotAPI.plots.none { it.data?.isGreenhouse == true }) return false

        val warnings = mutableListOf<Component>()
        if (miscInfo.cropGrowthValue == null) {
            warnings.add(
                ChatUtils.buildWithCommand(
                    "Unknown Crop Growth value. Click here to open desk",
                    "/desk"
                )
            )
        }
        if (miscInfo.cropSpeedUpgradeValue == null || miscInfo.cropYieldUpgradeValue == null) {
            warnings.add(
                ChatUtils.buildWithCommand(
                    "Unknown Crop Speed or Yield upgrade. Click here to open desk",
                    "/greenhouseupgrades"
                )
            )
        }
        if (greenhouseSpeedAttribute() == null) {
            warnings.add(
                ChatUtils.buildWithCommand(
                    "Unknown Timestalk attribute, ticks are timed as if it were zero. Click to set it",
                    "MagicAddons internal setTimestalkAttributeL57"
                )
            )
        }
        if (miscInfo.nextTickTime == null) {
            warnings.add(
                ChatUtils.buildWithPrefix("Unknown tick time, please right click a non fully grown plant")
            )
        }
        if (sendWarning) {
            ChatUtils.sendWarningsComponents(warnings)
        }
        return warnings.isNotEmpty()
    }

    /**
     * Marks a plant just put down as new. The server decides whether it went in at all; all this
     * adds is what a read cannot know, that it is at no age and holds no water.
     */
    private fun claimPlantedCrop(grid: GreenhouseGrid) {
        val now = Instant.now()
        placements.removeAll { now.isAfter(it.at.plus(PLACE_WINDOW)) }

        placements.removeAll { placement ->
            val slot = grid.getSlotAt(placement.pos, false) ?: return@removeAll false
            val element = grid.elementCovering(slot) ?: return@removeAll false
            if (element.instance.cropDef != placement.def) return@removeAll false

            claimPlacedPlant(element.instance)
            true
        }
    }

    /** Whether a plant of [def] was just put down on [slot], taking the placement off the list. */
    fun takePlacement(def: CropDefinition, slot: LayoutSlot, grid: GreenhouseGrid): Boolean {
        val now = Instant.now()
        val index = placements.indexOfFirst { placement ->
            placement.def == def &&
                    !now.isAfter(placement.at.plus(PLACE_WINDOW)) &&
                    grid.getSlotAt(placement.pos, false)?.let { it.x == slot.x && it.y == slot.y } == true
        }
        if (index < 0) return false
        placements.removeAt(index)
        return true
    }

    /** A plant the player put down: new, dry, and never to count as grown here. */
    fun claimPlacedPlant(instance: GreenhouseElementInstance) {
        instance.placed = true
        instance.age = 0L
        // placed, it is finished and drinks nothing, so it carries no level at all
        instance.waterLevel = null
        instance.waterExact = false
        instance.firstSeenStage = instance.lowestStage
    }

    /**
     * A mutation found where nothing stood at the last look. It spawned dry at stage one, and a
     * plant with no water debt takes every tick, so each stage it has climbed cost exactly one
     * tick of water. Its age is counted from the garden loading, whatever stage it is at: the
     * game starts a mutation that spawned while the player was away at zero.
     */
    fun claimSpawnedMutation(instance: GreenhouseElementInstance, layout: GreenhouseLayout) {
        val grown = ((instance.lowestStage ?: 1) - 1).coerceAtLeast(0)
        val now = Instant.now()

        if (instance.cropDef.needsWater) {
            instance.waterLevel = WaterModel.after(0, grown, layout.waterEffectAt(instance.slot))
            instance.waterExact = true
        }

        instance.age = Duration.between(gardenArrivedAt ?: now, now).toMillis().coerceAtLeast(0L)
        instance.firstSeenStage = 1
    }

    /** What the server says when a placement did not happen, so the claim is dropped rather than reused. */
    private val PLACE_REFUSALS: List<Regex> = listOf(
        Regex("can only grow on ", RegexOption.IGNORE_CASE),
        Regex("There is already a crop planted here", RegexOption.IGNORE_CASE),
        Regex("You cannot build here", RegexOption.IGNORE_CASE)
    )

    @EventHandler
    fun onPlacementRefused(event: OnSystemChatEvent) {
        if (placements.isEmpty()) return
        if (PLACE_REFUSALS.none { it.containsMatchIn(event.text) }) return

        // the refusal is about the last thing put down
        placements.removeAt(placements.lastIndex)
    }

    fun setDiagnosesListeningElement(hitBlock: BlockPos? = null, hitEntity: ArmorStand? = null, grid: GreenhouseGrid) {
        var hitElement: ElementRuntimeState? = null
        if (hitBlock != null) {
            hitElement = grid.elements.find {
                it.blocksMap?.keys?.contains(hitBlock) ?: return@find false
            }
        }
        if (hitEntity != null && hitElement == null) {
            hitElement = grid.elements.find {
                it.standEntities?.contains(hitEntity) ?: return@find false
            }
        }
        // anything inside a plant's footprint counts as part of it, whether or not the stage names
        // it, so pointing the tool at a hunger bar or an unnamed block still finds the plant
        if (hitElement == null) {
            val pos = hitBlock ?: hitEntity?.blockPosition()

            hitElement = pos
                ?.let { grid.getSlotAt(BlockPos(it.x, GREENHOUSE_SOIL_Y, it.z), false) }
                ?.let { grid.elementCovering(it) }
        }

        if (hitElement == null) {
            plantDiagnosticListeningElement = null
            return
        }

        plantDiagnosticListeningElement = hitElement
    }

    fun getDiagnosesData(realItems: List<ItemStack>) {
        if (!baseSetting.value) return
        val identifyStack = realItems.firstOrNull() ?: return

        val stackId = identifyStack.getSkyBlockId()
        val useNameFallback = stackId == null

        var def: CropDefinition? = null

        if (useNameFallback) {
            if (identifyStack.getLore().any { it.string.contains("Base Crop") }) {
                def = CropRegistry.get(identifyStack.customName?.string ?: identifyStack.itemName.string)
            }
        } else {
            def = CropRegistry.get(stackId.id)
        }

        val beaconLore = realItems.firstOrNull { it.item == Items.BEACON }?.getLore()
        val saplingLore = realItems.firstOrNull { it.item == Items.JUNGLE_SAPLING }?.getLore()
        val bucketLore = realItems.firstOrNull { it.item == Items.WATER_BUCKET }?.getLore()

        if (beaconLore == null || saplingLore == null || bucketLore == null) {
            if (CropCollector.isActive()) ChatUtils.sendWithPrefix(
                "The diagnosis is missing a page: " +
                        listOfNotNull(
                            "status".takeIf { beaconLore == null },
                            "growth".takeIf { saplingLore == null },
                            "water".takeIf { bucketLore == null }
                        ).joinToString(", ")
            )
            return
        }

        val waterLevel = runCatching {
            bucketLore[0].siblings[1].string.toInt()
        }.getOrNull()

        val status = runCatching {
            beaconLore[0].siblings[1].string
        }.getOrNull()

        // these two are read out of a fixed position in the lore rather than by label, so an empty
        // result prints the page it came from
        if (waterLevel == null && CropCollector.isActive()) {
            ChatUtils.sendWithPrefix("Could not read the water level, the water page reads:")
            dumpLore(bucketLore)
        }

        if (status == null && CropCollector.isActive()) {
            ChatUtils.sendWithPrefix("Could not read the status, the status page reads:")
            dumpLore(beaconLore)
        }

        val age = saplingLore.valueFor("Age")

        // "Stage: 1/15", of which only the part before the slash is the stage
        val stageRaw = saplingLore.valueFor("Stage")?.substringBefore('/')?.trim()
            ?.let { raw ->
                when {
                    raw.equals("FULLY GROWN", ignoreCase = true) -> def?.maxStage
                    raw.equals("DEAD", ignoreCase = true) -> {
                        if (def?.skyblockId == SkyBlockItemId.item("DEAD_PLANT"))
                            return@let 1
                        return@let null
                    }

                    else -> raw.toIntOrNull()
                }
            }

        val nextStage = saplingLore.valueFor("Next Stage")

        if (nextStage?.contains(Regex("\\d")) ?: false) {
            if (!LocationAPI.isGuest) {
                val was = miscInfo.nextTickTime

                miscInfo.nextTickTime = Instant.now().plusMillis(nextStage.parseDurationToMs())
                lastCheckTime = Instant.now()

                // the one line of the old tick logging worth keeping: how far the countdown had
                // drifted by the moment the game stated it, for reading a session back later
                was?.let {
                    val movedS = Duration.between(it, miscInfo.nextTickTime).toSeconds()

                    Common.LOGGER.info("[tick] resynced from the game: countdown moved ${movedS}s")
                }

                realignWithGame()
            }
        }

        // what the pages say about the plant pointed at, and the one thing the game knows better
        // than any guess: stage, water and age exactly
        plantDiagnosticListeningElement?.let { element ->
            age?.parseDurationToMs()?.let { element.instance.age = it }
            stageRaw?.let { element.instance.growthStage = GrowthStageInfo.Known(it) }

            // read rather than predicted, so whatever was assumed about the ticks it may have been
            // passed over for no longer applies
            waterLevel?.let {
                element.instance.waterLevel = it
                element.instance.waterPredictedInDebt = false
                element.instance.waterExact = true
            }
        }

        if (!CropCollector.isActive()) return

        // every failure is reported in chat rather than returning silently, since the player pointed
        // the tool at a plant to find out what the mod makes of it
        if (def == null) {
            ChatUtils.sendWithPrefix(
                "No crop described for ${stackId?.id ?: "an unrecognised plant"}, nothing to match against."
            )
            return
        }

        if (stageRaw == null) {
            ChatUtils.sendWithPrefix(
                "Could not read what stage ${def.name} is at, the growth page reads:"
            )

            dumpLore(saplingLore)
            return
        }

        // the page says how many stages the crop really has, so a definition that disagrees is
        // wrong about something the game just told us
        saplingLore.valueFor("Stage")
            ?.substringAfter('/', "")
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it != def.maxStage }
            ?.let {
                ChatUtils.sendWithPrefix(
                    "${def.name} is described with ${def.maxStage} stages but the game says $it"
                )
            }

        val hit = plantDiagnosticHitBaseBlock
        if (hit == null) {
            ChatUtils.sendWithPrefix("Nothing was pointed at, so there is no plant to correct.")
            return
        }

        // the only caller left, and it rescans the whole footprint itself, so what the tool
        // hands over is the crop, the stage the page just named and where the tool was pointed
        CropCollector.correct(def, stageRaw, hit)
    }

    /**
     * The value beside a label on a diagnosis page, read off the whole line: the server splits a
     * line wherever its colouring changes, so "Stage: 1/15" arrives as three pieces.
     */
    private fun List<Component>.valueFor(label: String): String? =
        firstOrNull { it.string.trimStart().startsWith("$label:", ignoreCase = true) }
            ?.string
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** Every lore line as it reads and as the pieces behind it, for when the parsing stops working. */
    private fun dumpLore(lore: List<Component>) {
        lore.forEachIndexed { index, line ->
            val pieces = line.siblings
                .mapIndexed { pieceIndex, piece -> "[$pieceIndex]${piece.string}" }
                .joinToString(" ")

            val whole = "[$index] ${line.string}    pieces: $pieces"

            ChatUtils.send(
                Component.literal("  $whole").withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withClickEvent(ClickEvent.CopyToClipboard(whole))
                        .withHoverEvent(HoverEvent.ShowText(Component.literal("Click to copy")))
                )
            )
        }
    }

    fun updateCropGrowth(realItems: List<ItemStack>) {
        val sunflower = realItems.firstOrNull {
            it.item == Items.SUNFLOWER
        } ?: return

        val lore = sunflower.getLore()

        val cropGrowthLine = lore.firstOrNull { component ->
            component.siblings.any { it.string.contains("Crop Growth") }
        }

        val cropGrowth = cropGrowthLine
            ?.siblings
            ?.firstOrNull { it.string.any { c -> c.isDigit() } }
            ?.string
            ?.filter { it.isDigit() }
            ?.toIntOrNull()

        cropGrowth ?: return
        if (miscInfo.cropGrowthValue != cropGrowth) {
            ChatUtils.sendWithPrefix("Updated crop growth value to $cropGrowth")
            ChatUtils.sendWithPrefix("Make sure to open /desk again if you get more crop growth.")
            miscInfo.cropGrowthValue = cropGrowth
        }

    }

    fun updateUpgrades(realItems: List<ItemStack>) {
        val seedsStack = realItems.firstOrNull { it.item == Items.WHEAT_SEEDS }
        val plantPotStack = realItems.firstOrNull { it.item == Items.FLOWER_POT }
        val seedsLore = seedsStack?.getLore()
        val potLore = plantPotStack?.getLore()

        val speedTier = seedsLore?.firstOrNull { line ->
            line.siblings.any { it.string.contains("Current Tier") }
        }?.siblings?.firstOrNull { it.string.toIntOrNull() != null }
            ?.string
            ?.toIntOrNull()

        val yieldTier = potLore?.firstOrNull { line ->
            line.siblings.any { it.string.contains("Current Tier") }
        }?.siblings?.firstOrNull { it.string.toIntOrNull() != null }
            ?.string
            ?.toIntOrNull()

        speedTier ?: return
        yieldTier ?: return

        if (miscInfo.cropSpeedUpgradeValue != speedTier) {
            miscInfo.cropSpeedUpgradeValue = speedTier
            ChatUtils.sendWithPrefix("Updated Growth Speed Tier to $speedTier")
        }
        if (miscInfo.cropYieldUpgradeValue != yieldTier) {
            miscInfo.cropYieldUpgradeValue = yieldTier
            ChatUtils.sendWithPrefix("Updated Plant Yield Tier to $speedTier")
        }
    }

    fun getCurrentUniques(): Set<UniqueCropKey> {
        val foundUniques = mutableSetOf<UniqueCropKey>()

        greenhouseGrids.forEach { grid ->
            grid.layout.elementInstances.forEach { instance ->
                if (!instance.cropDef.isBaseCrop) return@forEach
                foundUniques.add(UniqueCropKey.from(instance.cropDef))
            }
        }

        return foundUniques
    }

    fun getMissingUniques(): Set<UniqueCropKey> {
        val found = getCurrentUniques()
        return CropRegistry.all
            .filter { it.isBaseCrop }
            .map { UniqueCropKey.from(it) }
            .toSet()
            .minus(found)
    }


    /**
     * The greenhouse speed attribute, half a percent a level. Taken from what the player typed first,
     * since the shard api does not report this one at all.
     */
    fun greenhouseSpeedAttribute(): Int? =
        miscInfo.greenhouseSpeedAttribute
            ?: AttributeAPI.attributeMap.entries
                .firstOrNull { it.key.id == GREENHOUSE_SPEED_ATTRIBUTE_ID }
                ?.value
                ?.level
                ?.takeIf { it > 0 }

    /** How long one growth tick takes now, null while the formula is missing something. */
    fun currentGrowthTickMs(): Long? {
        val cropGrowth = miscInfo.cropGrowthValue ?: return null
        val upgrade = miscInfo.cropSpeedUpgradeValue ?: return null

        return computeGrowthStageTimeMs(
            getCurrentUniques().size,
            cropGrowth,
            upgrade,
            greenhouseSpeedAttribute() ?: 0
        )
    }

    /** What is left of the current tick. Never negative: an overdue tick has nothing left, not a debt. */
    fun remainingTickMs(): Long? {
        val next = miscInfo.nextTickTime ?: return null

        return (next.toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0L)
    }

    /**
     * Re-anchors the server tick to the countdown the game just gave us, so the next comparison is
     * not measuring across the resync.
     */
    private fun realignWithGame() {
        lastServerTick = ServerUtils.totalServerTicks
    }

    fun computeGrowthStageTimeMs(
        uniqueCrops: Int,
        cropGrowthStat: Int,
        greenhouseUpgrade: Int,
        speedAttribute: Int = 0
    ): Long {

        val uniqueCropBonus = 0.025 * uniqueCrops
        val cropGrowthBonus = 0.0025 * cropGrowthStat
        // a tenth of a percent a level, not half of one. The attribute was hotfixed down to cap at
        // one percent, and it still runs to ten levels, so what changed is what a level is worth
        val attributeBonus = 0.001 * speedAttribute

        val upgradeBonus = when (greenhouseUpgrade) {
            in 0..8 -> 0.05 * greenhouseUpgrade
            9 -> 0.50
            else -> throw IllegalArgumentException("Invalid greenhouse upgrade level: $greenhouseUpgrade")
        }

        val denominator =
            1.0 +
                    uniqueCropBonus +
                    cropGrowthBonus +
                    attributeBonus +
                    upgradeBonus

        val seconds = 14400.0 / denominator

        return (seconds * 1000.0).toLong()
    }




    sealed class UniqueCropKey {

        data class Def(val id: String) : UniqueCropKey()
        data object Flower : UniqueCropKey()
        data object Mushroom : UniqueCropKey()

        companion object {
            fun from(def: CropDefinition): UniqueCropKey {
                return when (def.name) {
                    "Sunflower", "Moonflower" -> Flower
                    "Red Mushroom", "Brown Mushroom" -> Mushroom
                    else -> Def(def.skyblockId?.id ?: def.name)
                }
            }
        }
    }

}