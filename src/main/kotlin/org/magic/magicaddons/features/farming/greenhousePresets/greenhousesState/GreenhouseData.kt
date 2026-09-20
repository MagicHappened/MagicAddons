package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import org.magic.magicaddons.commands.debug.LostPlantReport
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.commands.internal.farming.SetTimestalkAttribute
import org.magic.magicaddons.util.getBuildableArea
import org.magic.magicaddons.util.center
import org.magic.magicaddons.util.toShortDuration
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.elements.FireElement
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.events.greenhouse.GrowthTickEvent
import org.magic.magicaddons.events.greenhouse.PlotChangedEvent
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.events.world.EntityAddedEvent
import org.magic.magicaddons.events.world.EntityRemovedEvent
import org.magic.magicaddons.events.world.LevelUnloadingEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.ui.widgets.config.SettingDetail
import org.magic.magicaddons.util.ServerUtils
import tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.isSkyblockFiller
import java.time.Duration
import java.time.Instant
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseSpawnLog
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhousePlantDischarge
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhouseWatering
import org.magic.magicaddons.features.farming.greenhousePresets.render.LayoutRenderState
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings

object GreenhouseData : GridCallbacks {

    init {
        GreenhouseGrid.callbacks = this
    }

    var lastPlot: Plot? = null

    private var gardenArrivedAt: Instant? = null

    var checkGreenhouses = false
    var greenhousesInitialized = false
    var greenhouseGrids = mutableListOf<GreenhouseGrid>()
    var presetGrids = mutableListOf<MasterLayout>()

    fun allPlots(): List<GreenhouseLayout> = presetGrids.flatMap { it.plots }

    fun masterOf(plot: GreenhouseLayout): MasterLayout? = presetGrids.find { plot in it.plots }

    fun fullPlotName(plot: GreenhouseLayout): String {
        val master = masterOf(plot) ?: return plot.displayName()
        return if (master.plots.size > 1) "${master.plotTitle(plot)} of ${master.displayName()}" else master.displayName()
    }


    fun nameInFull(plot: GreenhouseLayout): String {
        val master = masterOf(plot) ?: return plot.displayName()

        return "${master.displayName()} - ${master.plotTitle(plot)}"
    }

    var miscInfo = MiscGreenhouseInfo()

    var currentPreset: MasterLayout? = null
    var currentGridIndex: Int = 0

    var lastCheckTime: Instant? = null
    var lastServerTick: Long? = null

    private class PlayerPlacement(val def: CropDefinition, val pos: BlockPos, val at: Instant)

    private val playerPlacements = mutableListOf<PlayerPlacement>()

    private val cropPlacements = mutableMapOf<BlockPos, CropPlacement>()

    private class CropPlacement(val def: CropDefinition, val at: Long)

    override fun placedCropAt(soilPos: BlockPos): CropDefinition? = cropPlacements[BlockPos(soilPos.x, GREENHOUSE_SOIL_Y, soilPos.z)]?.def

    override fun assumeFlatWater(): Boolean = GreenhousePresets.assumeFlatWater()

    override fun waterBarsExpected(): Boolean = GreenhouseWatering.wateringWindowOpen()


    override fun forgetPlayerPlacementAt(soilPos: BlockPos) {
        val key = BlockPos(soilPos.x, GREENHOUSE_SOIL_Y, soilPos.z)
        val placed = cropPlacements[key] ?: return
        if (System.currentTimeMillis() - placed.at < PLACE_SETTLE_MS) return
        cropPlacements.remove(key)
    }

    private const val PLACE_SETTLE_MS: Long = 2_000

    private fun overlaps(aOrigin: BlockPos, a: CropDefinition, bOrigin: BlockPos, b: CropDefinition): Boolean =
        aOrigin.x < bOrigin.x + b.footprint.width && bOrigin.x < aOrigin.x + a.footprint.width &&
                aOrigin.z < bOrigin.z + b.footprint.height && bOrigin.z < aOrigin.z + a.footprint.height

    private val SERVER_PLACE_WINDOW: Duration = Duration.ofSeconds(5)

    private var plantDiagnosticHitBaseBlock: BlockPos? = null

    var scanUpdatesState: Boolean = true

    var toolUpdatesState: Boolean = true

    private var plantDiagnosticListeningElement: ScannedPlant? = null

    private fun initKnownIds() {
        if (checkGreenhouses) return
        if (DataHandler.activeProfile == null) return
        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach { plot ->
            if (plot.data?.isGreenhouse != true) return@forEach
            val plotId = GreenhouseLayout.plotId(plot.id)
            val existingGrid = greenhouseGrids.find { plotId == it.layout.id }
            existingGrid ?: run {
                val gridLayout = GreenhouseLayout(id = plotId)
                val gridState = GreenhouseGrid.GridState(
                    lastScanTime = null,
                    needsRescan = true,
                    assignedLayout = null,
                    scanned = false
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

    /**
     * Whether the plot can be read: every chunk under it has been sent, and every stand in it is
     * standing where the server last put it. Stands come in their own packets after their chunk and
     * are moved into place after that, so a scan run early reads a plant with half its stands as
     * another crop or as nothing, and drops what it cannot match.
     */
    private fun plotReady(plot: Plot): Boolean {
        // a plot on its way out is never read again until the player next arrives at it
        if (plotUnloading) return false

        val level = Minecraft.getInstance().level ?: return false
        val area = plot.getBuildableArea() ?: return false

        if (!level.hasChunksAt(area.minX.toInt(), area.minZ.toInt(), area.maxX.toInt(), area.maxZ.toInt())) return false

        val now = System.currentTimeMillis()

        if (arrivalScanPending) {
            // the plot is named as current off the scoreboard, which can come before its first stand
            // has, so the quiet is counted from the arrival as well as from the last entity
            val quietSince = maxOf(lastEntityChangeAt ?: 0L, arrivalPendingSince)

            return now - quietSince >= ENTITY_QUIET_MS
        }

        val standsOnTheirWay = standsStillMoving(level)
        val movedAt = lastEntityChangeAt ?: return true

        if (standsOnTheirWay == 0 && now - movedAt >= ENTITY_MOVE_QUIET_MS) return true

        // a stand that never arrives would otherwise hold the scan off for good
        return now - (lastChangeAt ?: now) >= MAX_SCAN_DEFER_MS
    }

    /**
     * How many stands have not reached where the server last sent them, forgetting those that have.
     * A stand part way to its place stands at a height that belongs to another stage, so a plant
     * read then matches the wrong one or none at all.
     */
    private fun standsStillMoving(level: Level): Int {
        standTargets.entries.removeIf { (entityId, target) ->
            val standing = level.getEntity(entityId)?.position() ?: return@removeIf true
            standing.distanceToSqr(target) <= ARRIVED_DISTANCE_SQR
        }

        return standTargets.size
    }

    /** Where the server last sent each stand in the plot, until it gets there. */
    private val standTargets: MutableMap<Int, Vec3> = HashMap()

    /** A stand this close to where it was sent has arrived; the lerp lands exactly, so this is slack. */
    private const val ARRIVED_DISTANCE_SQR: Double = 1.0e-6

    /** Whether the level is being torn down around the player, see [onLevelUnloading]. */
    private var plotUnloading: Boolean = false

    /** When the arrival scan was asked for, so a plot whose stands never come still gets read. */
    private var arrivalPendingSince: Long = 0L

    /**
     * How long the plot has to go without a new entity before it is taken as fully sent. A stand's
     * height can still move for a moment after it arrives, which a scan reads as the wrong stage.
     */
    private const val ENTITY_QUIET_MS: Long = 2_000

    /** How long a stand has to hold still before a scan of an already known plot reads it. */
    private const val ENTITY_MOVE_QUIET_MS: Long = 500

    /** The longest a moving stand may put a scan off, counted from the plot's last change. */
    private const val MAX_SCAN_DEFER_MS: Long = 3_000

    /** When an entity last turned up, moved or was re-posed inside the plot being stood in. */
    private var lastEntityChangeAt: Long? = null

    /**
     * An entity inside the plot was moved or re-posed. [movingTo] is where the packet sends it,
     * which is not where it stands yet: the client walks it there over the following ticks.
     *
     * Called for every such packet in the world, so it costs a plot lookup and nothing more until
     * one lands inside the plot.
     */
    fun noteEntityChanged(entityId: Int, movingTo: Vec3? = null) {
        if (!greenhousesInitialized || !inOwnGarden()) return

        val gridArea = PlotAPI.getCurrentPlot()?.getBuildableArea() ?: return
        val entity = Minecraft.getInstance().level?.getEntity(entityId) ?: return
        if (!gridArea.contains(entity.position())) return

        lastEntityChangeAt = System.currentTimeMillis()
        if (movingTo == null) return
        standTargets[entityId] = movingTo

        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        // a stand moving changes what stands on every cell along its way, so all of them are read
        // again rather than only the two ends
        val from = BlockPos.containing(entity.position())
        val to = BlockPos.containing(movingTo)

        requestReconcile(BlockPos.betweenClosed(from, to).map { it.immutable() })
    }

    /** Whether the first scan since arriving at the plot, or since it was dumped, is still to run. */
    private var arrivalScanPending: Boolean = false

    private fun scanGridData() {
        if (!scanUpdatesState) return
        if (!greenhousesInitialized) return
        // the grid is found by plot number, which a visited garden has too: read there, somebody
        // else's plants would land in the player's own record
        if (!inOwnGarden()) return
        val plot = PlotAPI.getCurrentPlot() ?: return

        val grid = getCurrentGrid() ?: return
        if (grid.state.scanned && !grid.state.needsRescan) return

        // read again on a later tick, once the rest of the plot has been sent
        if (!plotReady(plot)) {
            fullScanWanted = true
            return
        }

        grid.plot = plot

        grid.readSoilBlocks()

        // a merge, so whatever the plot cannot say for a plant that is still there is carried over,
        // and any stage predicted while away is corrected by what is actually standing
        if (!grid.rescanPlants()) return

        // the plan is laid the way the plot agrees with, judged afresh on arriving: what stands now
        // says which way it was built, and a turn picked on assign may be stale by then
        if (arrivalScanPending) {
            grid.state.assignedLayout?.let { plan ->
                val turns = grid.bestTurnKeeping(plan, grid.state.planTurns)
                if (turns != grid.state.planTurns) {
                    grid.state.planTurns = turns
                    ChatUtils.sendWithPrefix("Plan on ${grid.layout.displayName()} re-laid at ${turns * 90}°, the turn what stands fits best.")
                }
            }
        }

        claimPlantedCrop(grid)
        GreenhouseSpawnLog.noteScan(grid)

        // the plan on screen is read off the plot, so it is only right until the plot changes
        LayoutRenderState.refresh()

        // after grid update
        grid.state.scanned = true
        grid.state.needsRescan = false
        grid.state.lastScanTime = Instant.now()
        grid.state.ticksSinceLastScan = 0
        arrivalScanPending = false
    }


    /** The most one look at the clock may move the next tick by, so a wrong guess cannot walk it away. */
    private const val MAX_TICK_ADJUSTMENT_MS: Long = 5_000

    /** The hunting shard carrying greenhouse speed, ten levels of a tenth of a percent each. */
    const val GREENHOUSE_SPEED_ATTRIBUTE_ID: String = "attribute:l57"

    /** Where the plot changed since the last tick; those slots are read again on the tick itself. */
    private val touched = mutableSetOf<BlockPos>()

    /** Whether the whole plot has to be read, for a change with no place to it. */
    private var fullScanWanted: Boolean = false

    /** When the plot last changed; once it has been quiet for [SETTLE_MS] the whole plot is read once. */
    private var lastChangeAt: Long? = null

    /** How long the plot has to be quiet before the full scan that squares everything with the world. */
    private const val SETTLE_MS: Long = 400

    /** Something in the plot changed at [positions], so what is stored around them can no longer be trusted. */
    fun requestReconcile(positions: Collection<BlockPos>) {
        if (positions.isEmpty()) return
        touched.addAll(positions)
        lastChangeAt = System.currentTimeMillis()
    }

    fun requestReconcile(position: BlockPos) = requestReconcile(listOf(position))

    /** Something changed with no place to it, so the whole plot is read on the next tick. */
    fun requestReconcile() {
        fullScanWanted = true
    }

    /**
     * Forgets every plant of [grid] and reads the plot again as if it had never been seen, so a
     * placed mutation is only known again once the player is seen placing it. Read at once when
     * the player stands in it, otherwise on their next visit.
     */
    fun rescanFromScratch(grid: GreenhouseGrid) {
        grid.scannedPlants.clear()
        grid.layout.plants.clear()
        grid.state.scanned = false
        grid.state.lastScanTime = null
        grid.state.needsRescan = true

        if (getCurrentGrid() === grid) {
            arrivalScanPending = true
            arrivalPendingSince = System.currentTimeMillis()
            plotUnloading = false
            scanGridData()
        }
    }

    /**
     * Each tick: the slots around this tick's changes are read again at once, and after the plot
     * has been quiet for a moment the whole of it is read once, so nothing drifts from the world.
     */
    private fun runDueReconcile() {
        val now = System.currentTimeMillis()

        if (touched.isNotEmpty()) {
            val positions = touched.toList()
            touched.clear()
            rescanAround(positions)
        }

        val settled = lastChangeAt?.let { now - it >= SETTLE_MS } == true
        if (!fullScanWanted && !settled) return

        fullScanWanted = false
        lastChangeAt = null
        getCurrentGrid()?.state?.needsRescan = true
        scanGridData()
    }

    /** Reads only the slots a change at [positions] can have reached; a plot never read gets the full scan. */
    private fun rescanAround(positions: List<BlockPos>) {
        val grid = getCurrentGrid() ?: return
        rescanSlots(grid, grid.scanSlotsReachedFrom(positions))
    }

    internal fun rescanSlots(grid: GreenhouseGrid, region: Set<Pair<Int, Int>>) {
        if (!inOwnGarden()) return
        if (getCurrentGrid() !== grid) return
        if (!grid.state.scanned) {
            fullScanWanted = true
            return
        }
        val plot = PlotAPI.getCurrentPlot() ?: return
        if (!plotReady(plot)) {
            fullScanWanted = true
            return
        }
        grid.plot = plot

        grid.readSoilBlocks()
        if (!grid.rescanPlants(region)) return
        claimPlantedCrop(grid)
        LayoutRenderState.refresh()
    }

    /**
     * The chosen ticks as time off, for the line under the setting. A span one tick wide, since the
     * tick already running is part spent, and it slides as the countdown runs.
     */
    fun absenceDetail(): SettingDetail? {
        val ticks = GreenhousePresets.chorusAbsenceTicks() ?: return null

        val tickMs = GrowthClock.tickLengthMs()
        val remaining = GrowthClock.remainingTickMs()

        if (tickMs == null || remaining == null) {
            return SettingDetail.Text(
                "(Missing variables, Cannot resolve tick time.)",
                MISSING_COLOR
            )
        }

        val shortest = remaining + (ticks - 1) * tickMs
        val longest = remaining + ticks * tickMs

        return SettingDetail.Text(
            "(absent for between ${shortest.toShortDuration()} - ${longest.toShortDuration()})"
        )
    }

    private const val MISSING_COLOR: Int = 0xFFFF8855.toInt()

    override fun warnSurvivor(plant: DyingPlant) = PlantWarnings.warnSurvivor(plant)

    override fun plantLostInScan(previous: Plant, origin: BlockPos, remainingStands: List<ArmorStand>) {
        if (GreenhouseSpawnLog.lostPlantsMessages) LostPlantReport.sendReport(previous, origin, remainingStands)
    }

    fun inGreenhouse(): Boolean = PlotAPI.getCurrentPlot()?.takeUnless { it.isBarn } != null

    fun inOwnGarden(): Boolean = LocationAPI.island == SkyBlockIsland.GARDEN && !LocationAPI.isGuest

    fun getCurrentGrid(): GreenhouseGrid? {
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return null
        return greenhouseGrids.find { it.layout.id == GreenhouseLayout.plotId(plotId) }
    }
    fun computeNextAvailableId(): Int {
        val usedIds = presetGrids
            .mapNotNull {
                it.id.removePrefix(GreenhouseLayout.MASTER_PRESET_PREFIX).toIntOrNull()
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
        val cropGrowth = miscInfo.cropGrowthValue ?: return
        val speedUpgrade = miscInfo.cropSpeedUpgradeValue ?: return

        val nextTick = miscInfo.nextTickTime ?: return

        val growthTickMs = GrowthClock.stageTimeMs(
            getCurrentUniques().size,
            cropGrowth,
            speedUpgrade,
            GrowthClock.speedAttribute() ?: 0
        )

        // the countdown as it stands once this check has nudged it
        var current = nextTick

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
            current = nextTick.plusMillis(adjustmentDelta)
            miscInfo.nextTickTime = current
            lastCheckTime = now

            now.toEpochMilli() - current.toEpochMilli()
        } else {
            lastCheckTime = now
            now.toEpochMilli() - nextTick.toEpochMilli()
        }

        if (overdueMs <= 0) return
        val passedGrowthTicks = (overdueMs / growthTickMs)

        if (passedGrowthTicks <= 0 && !nextTick.isBefore(now)) return

        // the countdown running out is itself a tick, and passedGrowthTicks counts only the whole
        // periods after it
        val elapsedTicks = passedGrowthTicks.toInt() + 1
        val nextTickAdvance = (passedGrowthTicks + 1) * growthTickMs
        miscInfo.nextTickTime = current.plusMillis(nextTickAdvance)

        greenhouseGrids.forEach { grid ->
            if (onlineTickTracking && !grid.isScanned()) return@forEach

            GreenhouseSpawnLog.noteGrowthTicks(grid, elapsedTicks, leftGarden = !onlineTickTracking)
            grid.state.ticksSinceLastScan += elapsedTicks
            grid.state.needsRescan = true

            // nobody is looking at this greenhouse, so the clock is all we have to go on
            grid.simulateGreenhouse(elapsedTicks, growthTickMs)
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

    @EventHandler
    fun onTick(event: WorldTickEvent) {
        runDueReconcile()
        ensureProfile()
        if (DataHandler.activeProfile == null) return
        OtherProfiles.advanceTicks()

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
        PlantWarnings.onTick()
    }



    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        // returning to the garden just after a dehydration warning is treated as a response to it,
        // so a teleport to the plot is offered two seconds later, once the world has loaded
        if (event.new == SkyBlockIsland.GARDEN) {
            gardenArrivedAt = Instant.now()

            PlantWarnings.onGardenArrival()
        }

        if (event.new != SkyBlockIsland.GARDEN) {
            DataHandler.saveGardenData()
            greenhouseGrids.forEach {
                it.state.scanned = false
            }
            EventBus.post(PlotChangedEvent(lastPlot,null))
            lastPlot = null
        }
        checkForUpdate()
    }

    @Subscription
    fun onGameShutdown(event: ServerDisconnectEvent) {
        GreenhouseSpawnLog.onGameClosing()
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

        // leaving is not a reason to read: on the way out of the garden, and on a disconnect, the
        // plot is still named as current while its plants are already being unloaded, so a scan
        // then reads a near-empty greenhouse and saves it over the good copy
        if (event.new == null) return

        arrivalScanPending = true
        arrivalPendingSince = System.currentTimeMillis()
        plotUnloading = false
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
        grid.state.buildAnnounced = false

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

        // the diagnosis reads what the tool was last pointed at, and any container ends that pointing
        val listening = plantDiagnosticListeningElement
        val hit = plantDiagnosticHitBaseBlock
        plantDiagnosticListeningElement = null
        plantDiagnosticHitBaseBlock = null

        when (event.title) {
            "Crop Diagnostics" -> if (toolUpdatesState) PlantDiagnostics.readDiagnosis(realItems, listening, hit)
            "Desk" -> MenuReadings.updateCropGrowth(realItems)
            "Greenhouse Upgrades" -> MenuReadings.updateUpgrades(realItems)
        }
    }


    @EventHandler
    fun onBlockBreak(event: BlockDestroyedEvent) {
        regenRender()

        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return


        val pos = event.pos
        val blockCenter = Vec3.atCenterOf(pos)

        if (grid.plot?.aabb?.contains(blockCenter) != true) return

        val slot = grid.getSlotAt(pos, false) ?: return
        if (event.pos.y == GREENHOUSE_SOIL_Y) {
            slot.soil = Blocks.AIR.defaultBlockState()
        } else {
            grid.removePlantWithBlockAt(pos)
        }

        requestReconcile(pos)
    }

    @EventHandler
    fun onBlockPlaced(event: BlockPlacedEvent) {
        regenRender()

        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val blockVec3 = Vec3.atCenterOf(event.pos)
        if (grid.plot?.aabb?.contains(blockVec3) != true) return

        requestReconcile(event.pos)
    }

    @EventHandler
    fun onEntityAdded(event: EntityAddedEvent) {
        // entities are reported for the whole world at once, so only the ones that turned up in
        // this plot count, each at its own place
        val gridArea = PlotAPI.getCurrentPlot()?.getBuildableArea() ?: return
        val arrived = event.addedEntityList.map { it.entity.position() }.filter { gridArea.contains(it) }
        if (arrived.isEmpty()) return

        // noted before anything is read, since the first scan of a plot is the one most likely to
        // run while its stands are still coming in
        lastEntityChangeAt = System.currentTimeMillis()

        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        requestReconcile(arrived.map { BlockPos.containing(it) })
    }

    @EventHandler
    fun onBlockUpdated(event: BlockChangedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val gridArea = grid.plot?.getBuildableArea() ?: return
        if (!gridArea.contains(event.packet.pos.center())) return
        val slot = grid.getSlotAt(event.packet.pos, false) ?: return

        if (event.packet.pos.y == GREENHOUSE_SOIL_Y + 1 && event.packet.blockState.block == Blocks.FIRE) {
            val alreadyHasFire = grid.scannedPlants.any {
                it.plant.slot == slot && it.plant.cropDef.name == "Fire"
            }
            if (!alreadyHasFire) {
                val fireRuntime = FireElement.getFireAtSlot(
                    slot,
                    mapOf(event.packet.pos to event.packet.blockState)
                )
                grid.scannedPlants.add(fireRuntime)
            }
            return
        }

        if (event.packet.pos.y != GREENHOUSE_SOIL_Y) return
        slot.soil = event.packet.blockState

        requestReconcile(event.packet.pos)
    }


    /**
     * A plant is broken by hitting stands, not blocks, so the block listener never fires for it.
     * Waited for rather than read on the swing, which looked at a plot the crop still stood in.
     */
    @EventHandler
    fun onEntityRemoved(event: EntityRemovedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val area = grid.plot?.getBuildableArea() ?: return
        requestReconcile(event.removedEntityList.map { it.entity.position() }.filter { area.contains(it) }.map { BlockPos.containing(it) })
    }

    /**
     * The level is about to be torn down, whichever way: nothing in it can be read any more, so
     * every scan waits until the player next arrives at a plot. Anything that had asked for one is
     * forgotten with it, since it would only have read what was left.
     */
    @EventHandler
    fun onLevelUnloading(event: LevelUnloadingEvent) {
        plotUnloading = true
        touched.clear()
        lastChangeAt = null
        fullScanWanted = false
    }

    @EventHandler
    fun onAttackEntity(event: AttackEntityEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val area = grid.plot?.getBuildableArea() ?: return
        if (!area.contains(event.target.position())) return

        requestReconcile(BlockPos.containing(event.target.position()))
    }

    @EventHandler
    fun onInteractEntity(event: InteractEntityEvent) {
        val entityBlockPos = BlockPos.containing(event.target.position())
        plantDiagnosticHitBaseBlock = BlockPos(entityBlockPos.x, GREENHOUSE_SOIL_Y, entityBlockPos.z)
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return
        val standTarget = event.target as? ArmorStand ?: return

        // read before the held item is looked at: a charge is taken by an empty hand too
        GreenhousePlantDischarge.setPlantClicked(plantAtStand(standTarget, grid)?.plant)

        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return
        if (mainHandId.id == DIAGNOSTICS_TOOL_ID) listenAtStand(standTarget, grid)
    }

    private const val DIAGNOSTICS_TOOL_ID: String = "item:plant_diagnostics_tool"


    @EventHandler
    fun onItemUse(event: UseEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        GreenhouseWatering.startWateringWindow(mainHandId)
    }

    @EventHandler
    fun onBlockUse(event: BlockUseEvent) {
        plantDiagnosticHitBaseBlock = BlockPos(event.hit.blockPos.x, GREENHOUSE_SOIL_Y, event.hit.blockPos.z)
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        GreenhousePlantDischarge.setPlantClicked(plantAtBlock(event.hit.blockPos, grid)?.plant)

        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        val foundCrop = CropRegistry.findByIdOrName(mainHandId.id)

        if (mainHandId.id == DIAGNOSTICS_TOOL_ID) {
            listenAtBlock(event.hit.blockPos, grid)
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

            playerPlacements.add(PlayerPlacement(foundCrop, pos, Instant.now()))

            // nothing can be placed over a plant, so whatever was remembered in the way is gone
            val soil = BlockPos(pos.x, GREENHOUSE_SOIL_Y, pos.z)
            cropPlacements.entries.removeAll { (at, placed) -> overlaps(at, placed.def, soil, foundCrop) }
            cropPlacements[soil] = CropPlacement(foundCrop, System.currentTimeMillis())
            requestReconcile(pos)
        }
    }


    fun warnUnknownValues(): Boolean {
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
        if (GrowthClock.speedAttribute() == null) {
            warnings.add(
                ChatUtils.buildWithCommand(
                    "Unknown Timestalk attribute, ticks are timed as if it were zero. Click to set it",
                    "${MainInternal.PATH} ${SetTimestalkAttribute.NAME}"
                )
            )
        }
        if (miscInfo.nextTickTime == null) {
            warnings.add(
                ChatUtils.buildWithPrefix("Unknown tick time, please right click a non fully grown plant")
            )
        }
        ChatUtils.sendWarningsComponents(warnings)
        return warnings.isNotEmpty()
    }

    /**
     * Marks a plant just put down as new. The server decides whether it went in at all; all this
     * adds is what a read cannot know, that it is at no age and holds no water.
     */
    private fun claimPlantedCrop(grid: GreenhouseGrid) {
        val now = Instant.now()
        playerPlacements.removeAll { now.isAfter(it.at.plus(SERVER_PLACE_WINDOW)) }

        playerPlacements.toList().forEach { placement ->
            val slot = grid.getSlotAt(placement.pos, false) ?: return@forEach
            val element = grid.elementCoveringSlot(slot) ?: return@forEach

            if (placementConfirmed(element.plant.cropDef, slot, grid)) markAsPlaced(element.plant)
        }
    }

    override fun placementConfirmed(crop: CropDefinition, slot: LayoutSlot, grid: GreenhouseGrid): Boolean {
        val now = Instant.now()
        val index = playerPlacements.indexOfFirst { placement ->
            placement.def == crop &&
                    !now.isAfter(placement.at.plus(SERVER_PLACE_WINDOW)) &&
                    grid.getSlotAt(placement.pos, false)?.let { it.x == slot.x && it.y == slot.y } == true
        }
        if (index < 0) return false
        playerPlacements.removeAt(index)
        return true
    }

    override fun markAsPlaced(plant: Plant) {
        plant.placed = true
        plant.age = 0L

        val stage = plant.growthStage
        if (stage is GrowthStageInfo.Estimated && plant.cropDef.stagePlacedAt in stage.range) {
            plant.growthStage = GrowthStageInfo.Known(plant.cropDef.stagePlacedAt)
        }

        if (plant.consumesWater) {
            plant.waterLevel = 0.0
            plant.waterExact = true
        } else {
            plant.waterLevel = null
            plant.waterExact = false
        }
        plant.waterBestCase = null
        plant.firstSeenStage = plant.lowestStage
    }


    override fun claimSpawnedMutation(plant: Plant, layout: GreenhouseLayout) {
        val grown = ((plant.lowestStage ?: 1) - 1).coerceAtLeast(0)
        val now = Instant.now()


        if (plant.cropDef.drainsNeighbours) {
            plant.waterLevel = WaterModel.DRAIN_PER_STAGE * grown
            plant.waterExact = false
        } else if (plant.cropDef.needsWater) {
            val waterEffect = GreenhouseGrid.waterEffectAt(layout, plant.slot)
            val predictedWater = WaterModel.waterLevelAfter(0.0, grown, waterEffect)
            // a spawn still standing is alive
            plant.waterLevel = WaterModel.lowestWaterLevelStillAlive(predictedWater, waterEffect)
            plant.waterExact = predictedWater > WaterModel.DEATH_LEVEL
        }

        plant.waterBestCase = null
        plant.age = Duration.between(gardenArrivedAt ?: now, now).toMillis().coerceAtLeast(0L)
        plant.firstSeenStage = 1
        GreenhouseSpawnLog.recordSpawn(plant, layout)
    }

    private val PLACE_REFUSALS: List<Regex> = listOf(
        Regex("can only grow on ", RegexOption.IGNORE_CASE),
        Regex("There is already a crop planted here", RegexOption.IGNORE_CASE),
        Regex("You cannot build here", RegexOption.IGNORE_CASE)
    )

    @EventHandler
    fun onPlacementRefused(event: SystemChatEvent) {
        if (playerPlacements.isEmpty()) return
        if (PLACE_REFUSALS.none { it.containsMatchIn(event.text) }) return

        playerPlacements.removeAt(playerPlacements.lastIndex)
    }

    /** The scanned plant a block in the greenhouse being stood in belongs to. */
    fun scannedPlantAtBlock(pos: BlockPos): ScannedPlant? =
        scannedGrid()?.let { grid -> plantAtBlock(pos, grid) }

    fun scannedPlantAtStand(stand: ArmorStand): ScannedPlant? =
        scannedGrid()?.let { grid -> plantAtStand(stand, grid) }

    /** Whether the running plan wants this very crop kept as an ingredient where it stands. */
    fun isPlannedIngredient(scanned: ScannedPlant): Boolean {
        val slot = scanned.plant.slot
        val planned = scannedGrid()?.plannedPlantAt(slot.x, slot.y) ?: return false

        return planned.slot.mark == LayoutSlot.Marking.Ingredient && planned.acceptsCrop(scanned.plant.cropDef)
    }

    private fun scannedGrid(): GreenhouseGrid? = getCurrentGrid()?.takeIf { it.isScanned() }

    private fun plantAtBlock(pos: BlockPos, grid: GreenhouseGrid): ScannedPlant? =
        grid.scannedPlants.find { it.blocks?.keys?.contains(pos) == true } ?: elementAround(pos, grid)

    private fun plantAtStand(stand: ArmorStand, grid: GreenhouseGrid): ScannedPlant? =
        grid.scannedPlants.find { it.stands?.contains(stand) == true } ?: elementAround(stand.blockPosition(), grid)

    private fun listenAtBlock(pos: BlockPos, grid: GreenhouseGrid) {
        plantDiagnosticListeningElement = plantAtBlock(pos, grid)
    }

    private fun listenAtStand(stand: ArmorStand, grid: GreenhouseGrid) {
        plantDiagnosticListeningElement = plantAtStand(stand, grid)
    }


    private fun elementAround(pos: BlockPos, grid: GreenhouseGrid): ScannedPlant? =
        grid.getSlotAt(BlockPos(pos.x, GREENHOUSE_SOIL_Y, pos.z), false)?.let { grid.elementCoveringSlot(it) }

    fun getCurrentUniques(): Set<UniqueCropKey> {
        val foundUniques = mutableSetOf<UniqueCropKey>()

        greenhouseGrids.forEach { grid ->
            grid.layout.plants.forEach { instance ->
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


    internal fun realignWithGameTime() {
        lastServerTick = ServerUtils.totalServerTicks
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
                    else -> Def(def.elementId)
                }
            }
        }
    }

}