package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import java.time.Duration
import java.time.Instant
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.server.GreenhouseDataSync
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.commands.internal.farming.SetTimestalkAttribute
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.FireElement
import org.magic.magicaddons.data.greenhouse.plot.*
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.events.greenhouse.GrowthTickEvent
import org.magic.magicaddons.events.greenhouse.PlotChangedEvent
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.EntityAddedEvent
import org.magic.magicaddons.events.world.EntityRemovedEvent
import org.magic.magicaddons.events.world.LevelUnloadingEvent
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseSpawnLog
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhousePlantDischarge
import org.magic.magicaddons.features.farming.greenhousePresets.playerActions.GreenhouseWatering
import org.magic.magicaddons.features.farming.greenhousePresets.render.LayoutRenderState
import org.magic.magicaddons.features.farming.greenhousePresets.shrunkPlants.ShorterCaneCrops
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.SBLocation
import org.magic.magicaddons.util.ServerUtils
import org.magic.magicaddons.util.center
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import java.util.Optional
import net.minecraft.ChatFormatting
import net.minecraft.world.phys.AABB
import net.minecraft.network.chat.Style
import org.magic.magicaddons.util.compat.McCompat
import tech.thatgravyboat.skyblockapi.api.events.hypixel.HypixelJoinEvent
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.profile.profile.ProfileAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.isSkyblockFiller

object GreenhouseData : GridCallbacks {

    init {
        GreenhouseGrid.callbacks = this
    }

    var lastPlot: Plot? = null

    private var gardenArrivedAt: Instant? = null

    var checkGreenhouses = false
    var greenhousesInitialized = false
    var greenhouseGrids = mutableListOf<GreenhouseGrid>()
    var presetGrids = mutableListOf<GreenhouseLayout>()

    fun greenhouseLayoutFor(plot: PlotLayout): GreenhouseLayout? = presetGrids.find { plot in it.plots }

    fun resolveAssignedLayoutIds() {
        val plots = presetGrids.flatMap { it.plots }

        greenhouseGrids.forEach { grid ->
            grid.state.assignedLayout = plots.find { it.id == grid.state.assignedLayoutId }
            grid.state.assignedLayoutId = null
        }
    }

    fun fullPlotName(plot: PlotLayout): String {
        val greenhouse = greenhouseLayoutFor(plot) ?: return plot.displayName()
        return if (greenhouse.plots.size > 1) "${greenhouse.plotTitle(plot)} of ${greenhouse.displayName()}" else greenhouse.displayName()
    }


    fun nameInFull(plot: PlotLayout): String {
        val greenhouse = greenhouseLayoutFor(plot) ?: return plot.displayName()

        return "${greenhouse.displayName()} - ${greenhouse.plotTitle(plot)}"
    }

    var miscInfo = MiscGreenhouseInfo()

    var currentPreset: GreenhouseLayout? = null
    var currentGridIndex: Int = 0

    var lastCheckTime: Instant? = null

    var joiningSkyBlock: Boolean = true
        private set

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



    private var plantDiagnosticListeningElement: ScannedPlant? = null

    private fun initKnownIds() {
        if (checkGreenhouses) return
        if (GreenhouseProfiles.activeProfileId == null) return
        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach { plot ->
            if (plot.data?.isGreenhouse != true) return@forEach
            val plotId = PlotLayout.plotId(plot.id)
            val existingGrid = greenhouseGrids.find { plotId == it.layout.id }
            existingGrid ?: run {
                val gridLayout = PlotLayout(id = plotId)
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

    private enum class PlotReadiness {
        Settled,
        Overdue,
        Waiting
    }

    private fun plotReadiness(plot: Plot): PlotReadiness {
        if (plotUnloading) return PlotReadiness.Waiting

        val level = Minecraft.getInstance().level ?: return PlotReadiness.Waiting
        val area = plot.getBuildableArea()

        if (!chunksLoadedOver(level, area)) return PlotReadiness.Waiting

        val now = System.currentTimeMillis()

        val quietSince = maxOf(lastEntityChangeAt ?: 0L, plotEnteredAt)

        if (standsStillMoving(level) == 0 && now - quietSince >= ENTITY_QUIET_MS) return PlotReadiness.Settled

        val deferredSince = scanDeferredSince ?: now.also { scanDeferredSince = it }

        return if (now - deferredSince >= MAX_SCAN_DEFER_MS) PlotReadiness.Overdue else PlotReadiness.Waiting
    }


    private fun chunksLoadedOver(level: Level, area: AABB): Boolean {
        val chunkSource = level.chunkSource

        for (chunkX in (area.minX.toInt() shr 4)..(area.maxX.toInt() shr 4)) {
            for (chunkZ in (area.minZ.toInt() shr 4)..(area.maxZ.toInt() shr 4)) {
                if (!chunkSource.hasChunk(chunkX, chunkZ)) return false
            }
        }

        return true
    }

    private fun standsStillMoving(level: Level): Int {
        val now = System.currentTimeMillis()

        standTargets.entries.removeIf { (entityId, target) ->
            val standing = level.getEntity(entityId)?.position() ?: return@removeIf true
            now - target.notedAt >= STAND_MOVE_TIMEOUT_MS || standing.distanceToSqr(target.position) <= STAND_ARRIVED_WITHIN_SQR
        }

        return standTargets.size
    }

    private class StandTarget(val position: Vec3, val notedAt: Long)

    private val standTargets: MutableMap<Int, StandTarget> = HashMap()

    private const val STAND_ARRIVED_WITHIN_SQR: Double = 1.0e-6

    private const val STAND_MOVE_TIMEOUT_MS: Long = 1_000

    private var plotUnloading: Boolean = false

    private var plotEnteredAt: Long = 0L

    private const val ENTITY_QUIET_MS: Long = 250

    private const val MAX_SCAN_DEFER_MS: Long = 3_000

    private var scanDeferredSince: Long? = null

    private var lastEntityChangeAt: Long? = null

    private var lastChangedStandId: Int? = null

    fun noteStandTeleported(entityId: Int, movingTo: Vec3?) {
        if (movingTo == null) standTargets.remove(entityId)
        noteStandChanged(entityId, movingTo)
    }

    fun noteStandChanged(entityId: Int, movingTo: Vec3? = null) {
        if (!greenhousesInitialized) return

        val gridArea = PlotAPI.getCurrentPlot()?.getBuildableArea() ?: return
        val stand = Minecraft.getInstance().level?.getEntity(entityId) as? ArmorStand ?: return
        if (!gridArea.contains(stand.position())) return

        val now = System.currentTimeMillis()
        lastEntityChangeAt = now
        lastChangedStandId = entityId
        if (movingTo == null) return
        standTargets[entityId] = StandTarget(movingTo, now)

        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        // a stand moving invalidates every cell, so plants in between are properly scanned
        val from = BlockPos.containing(stand.position())
        val to = BlockPos.containing(movingTo)

        markBlocksDirty(BlockPos.betweenClosed(from, to).map { it.immutable() })
    }

    class MovingStand(val stand: ArmorStand, val distanceToTarget: Double, val movingForMs: Long)

    class ScanStatus(
        val plotName: String,
        val isSettled: Boolean,
        val deferredForMs: Long?,
        val lastScanAgoMs: Long?,
        val quietForMs: Long,
        val lastChangedStand: ArmorStand?,
        val movingStands: List<MovingStand>
    ) {
        val quietNeededMs: Long get() = ENTITY_QUIET_MS
    }

    fun scanStatus(): ScanStatus? {
        val grid = getCurrentGrid() ?: return null
        val level = Minecraft.getInstance().level ?: return null
        val now = System.currentTimeMillis()

        standsStillMoving(level)
        val movingStands = standTargets.mapNotNull { (entityId, target) ->
            val stand = level.getEntity(entityId) as? ArmorStand ?: return@mapNotNull null
            MovingStand(stand, stand.position().distanceTo(target.position), now - target.notedAt)
        }
        val quietForMs = now - maxOf(lastEntityChangeAt ?: 0L, plotEnteredAt)

        return ScanStatus(
            plotName = grid.layout.displayName(),
            isSettled = movingStands.isEmpty() && quietForMs >= ENTITY_QUIET_MS,
            deferredForMs = scanDeferredSince?.let { now - it },
            lastScanAgoMs = grid.state.lastScanTime?.let { now - it.toEpochMilli() },
            quietForMs = quietForMs,
            lastChangedStand = lastChangedStandId?.let { level.getEntity(it) as? ArmorStand },
            movingStands = movingStands
        )
    }

    private var isPlanTurned: Boolean = false

    private fun scanGridData() {
        if (!greenhousesInitialized) return
        val plot = PlotAPI.getCurrentPlot() ?: return

        val grid = getCurrentGrid() ?: return
        if (grid.state.scanned && !grid.state.needsRescan) return

        if (joiningSkyBlock) {
            shouldRescanCurrentPlot = true
            return
        }

        val readiness = plotReadiness(plot)
        if (readiness == PlotReadiness.Waiting) {
            shouldRescanCurrentPlot = true
            return
        }
        scanDeferredSince = null
        val isPlotSettled = readiness == PlotReadiness.Settled

        grid.plot = plot

        grid.readSoilBlocks()

        if (!grid.rescanPlants(shouldKeepUnmatchedPlants = !isPlotSettled)) return

        if (isPlotSettled && !isPlanTurned) {
            grid.state.assignedLayout?.takeUnless { grid.state.noRotateAssignedLayout }?.let { plan ->
                val turns = grid.compareRotations(plan, grid.state.planTurns)
                if (turns != grid.state.planTurns) {
                    grid.state.planTurns = turns
                    ChatUtils.sendWithPrefix("Plan on ${grid.layout.displayName()} re-positioned at ${turns * 90}°")
                }
            }
            isPlanTurned = true
        }

        claimPlantedCrop(grid)
        if (isPlotSettled) GreenhouseSpawnLog.noteScan(grid)

        LayoutRenderState.refresh()

        // after grid update
        grid.state.scanned = true
        grid.state.needsRescan = false
        grid.state.lastScanTime = Instant.now()
        grid.state.ticksSinceLastScan = 0

        ShorterCaneCrops.updateHiddenPlantParts()
    }
    private const val MAX_TICK_ADJUSTMENT_MS: Long = 5_000

    // each level is 0.1% growth speed
    const val GREENHOUSE_SPEED_ATTRIBUTE_ID: String = "attribute:l57"

    private val dirtyBlocks = mutableSetOf<BlockPos>()

    private var shouldRescanCurrentPlot: Boolean = false

    private var lastScanAt: Long = 0L

    private const val FULL_SCAN_INTERVAL_MS: Long = 5_000

    private const val PESTS_LABEL: String = "Pests"

    private var lastChangeAt: Long? = null

    private const val PLOT_SETTLE_MS: Long = 400

    fun markBlocksDirty(positions: Collection<BlockPos>) {
        if (positions.isEmpty()) return
        dirtyBlocks.addAll(positions)
        lastChangeAt = System.currentTimeMillis()
    }

    fun markBlocksDirty(position: BlockPos) = markBlocksDirty(listOf(position))

    fun rescanFromScratch(grid: GreenhouseGrid) {
        grid.scannedPlants.clear()
        grid.layout.plants.clear()
        grid.state.scanned = false
        grid.state.lastScanTime = null
        grid.state.needsRescan = true

        if (getCurrentGrid() === grid) {
            isPlanTurned = false
            plotEnteredAt = System.currentTimeMillis()
            plotUnloading = false
            scanGridData()
        }
    }

    private fun rescanCurrentPlot() {
        val now = System.currentTimeMillis()

        if (dirtyBlocks.isNotEmpty()) {
            val positions = dirtyBlocks.toList()
            dirtyBlocks.clear()
            rescanAround(positions)
            lastScanAt = now
        }

        if (now - lastScanAt >= FULL_SCAN_INTERVAL_MS) shouldRescanCurrentPlot = true

        val settled = lastChangeAt?.let { now - it >= PLOT_SETTLE_MS } == true
        if (!shouldRescanCurrentPlot && !settled) return

        shouldRescanCurrentPlot = false
        lastChangeAt = null
        lastScanAt = now
        getCurrentGrid()?.state?.needsRescan = true
        scanGridData()
    }


    private fun rescanAround(positions: List<BlockPos>) {
        val grid = getCurrentGrid() ?: return
        rescanSlots(grid, grid.regionSetFromPositions(positions))
    }

    fun rescanSlots(grid: GreenhouseGrid, region: Set<Pair<Int, Int>>) {
        if (getCurrentGrid() !== grid) return
        if (!grid.state.scanned) {
            shouldRescanCurrentPlot = true
            return
        }
        val plot = PlotAPI.getCurrentPlot() ?: return
        val readiness = plotReadiness(plot)
        if (readiness == PlotReadiness.Waiting) {
            shouldRescanCurrentPlot = true
            return
        }
        scanDeferredSince = null
        grid.plot = plot

        grid.readSoilBlocks()
        if (!grid.rescanPlants(region, shouldKeepUnmatchedPlants = readiness != PlotReadiness.Settled)) return
        claimPlantedCrop(grid)
        LayoutRenderState.refresh()
        ShorterCaneCrops.updateHiddenPlantParts()
    }

    fun getCurrentGrid(): GreenhouseGrid? {
        if (!SBLocation.OwnGarden.inside()) return null

        val plotId = PlotAPI.getCurrentPlot()?.id ?: return null
        return greenhouseGrids.find { it.layout.id == PlotLayout.plotId(plotId) }
    }
    fun computeNextAvailableId(): Int {
        val usedIds = presetGrids
            .mapNotNull {
                it.id.removePrefix(PlotLayout.GREENHOUSE_PRESET_PREFIX).toIntOrNull()
            }
            .toSet()

        var nextId = 1

        while (nextId in usedIds) {
            nextId++
        }

        return nextId
    }

    private val AWAY_THRESHOLD: Duration = Duration.ofSeconds(20)

    fun checkForGrowthTickUpdate() {
        if (!greenhousesInitialized) return
        val cropGrowth = miscInfo.cropGrowthValue ?: return
        val speedUpgrade = miscInfo.cropSpeedUpgradeValue ?: return
        val nextTick = miscInfo.nextTickTime ?: return


        val growthTickMs = GreenhouseTickTime.stageTimeMs(
            getCurrentUniques().size,
            cropGrowth,
            speedUpgrade,
            GreenhouseTickTime.speedAttribute() ?: 0
        )

        var current = nextTick

        val now = Instant.now()

        val onlineTickTracking =
            SBLocation.OwnGarden.inside() && lastCheckTime != null

        val overdueMs = if (onlineTickTracking) {
            val currentTick = ServerUtils.totalServerTicks
            val previousTick = lastServerTick

            lastServerTick = currentTick

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

            val unaccountedMs = realMs - serverMs

            if (Duration.ofMillis(unaccountedMs) > AWAY_THRESHOLD) {
                lastCheckTime = now
                return
            }

            val adjustmentDelta = unaccountedMs.coerceIn(-MAX_TICK_ADJUSTMENT_MS, MAX_TICK_ADJUSTMENT_MS)

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

        val elapsedTicks = passedGrowthTicks.toInt() + 1
        val nextTickAdvance = (passedGrowthTicks + 1) * growthTickMs
        miscInfo.nextTickTime = current.plusMillis(nextTickAdvance)

        greenhouseGrids.forEach { grid ->
            if (onlineTickTracking && !grid.isScanned()) return@forEach

            GreenhouseSpawnLog.noteGrowthTicks(grid, elapsedTicks, leftGarden = !onlineTickTracking)
            grid.state.ticksSinceLastScan += elapsedTicks
            grid.state.needsRescan = true

            grid.simulateGreenhouse(elapsedTicks)
        }

        EventBus.post(GrowthTickEvent(elapsedTicks, growthTickMs, ProfileAPI.profileName, isActiveProfile = true))
    }

    private fun switchProfileIfChanged() {
        if (!ProfileAPI.isLoaded) return
        val profileId = runCatching { ProfileAPI.profileId }.getOrNull() ?: return
        val profileName = ProfileAPI.profileName ?: return
        if (GreenhouseProfiles.activeProfileId != null && profileId != GreenhouseProfiles.activeProfileId) leaveActiveProfile()
        GreenhouseProfiles.switchProfile(profileId, profileName)
    }

    fun resetForProfile() {
        checkGreenhouses = false
        currentPreset = null
        lastCheckTime = null
        lastServerTick = null
        gardenArrivedAt = null
        joiningSkyBlock = true
        regenRender()
    }

    private fun updateTickTimeAfterJoin() {
        if (!joiningSkyBlock || !greenhousesInitialized || GreenhouseProfiles.activeProfileId == null) return

        checkForGrowthTickUpdate()
        joiningSkyBlock = false
    }

    @EventHandler
    fun onTick(event: WorldTickEvent) {
        switchProfileIfChanged()
        if (GreenhouseProfiles.activeProfileId == null) return

        OtherProfiles.advanceTicks()
        updateTickTimeAfterJoin()
        checkForTickTimeUpdate()
        PlantWarnings.onTick()

        if (!SBLocation.OwnGarden.inside()) return

        noteGardenArrival()
        checkPlotChange()
        rescanCurrentPlot()
    }

    private fun noteGardenArrival() {
        if (gardenArrivedAt == null) gardenArrivedAt = Instant.now()
    }

    private fun checkForTickTimeUpdate() {
        val last = lastCheckTime ?: return
        val now = Instant.now()

        if (last.plusSeconds(60).isBefore(now) || miscInfo.nextTickTime?.isBefore(now) == true) {
            checkForGrowthTickUpdate()
        }
    }



    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.old == null) joiningSkyBlock = true

        scoreboardLines = emptyList()
        pestDebuffActive = false

        if (event.new != SkyBlockIsland.GARDEN) {
            gardenArrivedAt = null
            saveForThisNetwork()
            greenhouseGrids.forEach {
                it.state.scanned = false
            }
            EventBus.post(PlotChangedEvent(lastPlot,null))
            lastPlot = null
        }
        if (joiningSkyBlock) updateTickTimeAfterJoin() else checkForGrowthTickUpdate()
    }

    @Subscription
    fun onGameShutdown(event: ServerDisconnectEvent) {
        leaveActiveProfile()
    }

    private fun leaveActiveProfile() {
        scoreboardLines = emptyList()
        pestDebuffActive = false
        GreenhouseSpawnLog.onGameClosing()
        saveForThisNetwork()
    }

    fun saveForThisNetwork() {
        PresetStorage.savePresets()
        if (!GreenhouseProfiles.holdsAlphaData) GreenhouseProfiles.saveGreenhouseData()
    }

    @Subscription
    fun onHypixelJoin(event: HypixelJoinEvent) {
        if (event.onAlpha) {
            GreenhouseProfiles.enterAlpha()
        } else {
            GreenhouseSpawnLog.discardOpenRecords()
            GreenhouseProfiles.leaveAlpha()
            GreenhouseDataSync.reportOnlineOnMainNetwork()
        }
    }


    var pestDebuffActive: Boolean = false
        private set

    private var scoreboardLines: List<Component> = emptyList()

    private fun readPestDebuff() {
        pestDebuffActive = scoreboardLines.any { line ->
            if (!line.string.contains(PESTS_LABEL, ignoreCase = true)) return@any false

            var red = false
            line.visit({ style, _ ->
                if (style.color?.value == McCompat.chatColor(ChatFormatting.RED)) red = true
                Optional.empty<Unit>()
            }, Style.EMPTY)
            red
        }
    }

    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (!SBLocation.OwnGreenhouse.inside()) return
        scoreboardLines = event.newComponents
        readPestDebuff()
    }

    private fun checkPlotChange() {
        if (!SBLocation.OwnGarden.inside()) return

        val plot = PlotAPI.getCurrentPlot()
        if (lastPlot == plot) return

        EventBus.post(PlotChangedEvent(lastPlot, plot))
        lastPlot = plot
    }

    @EventHandler
    fun onPlotChanged(event: PlotChangedEvent) {
        if (!baseSetting.value) return
        initKnownIds()
        if (event.new == null) return

        isPlanTurned = false
        plotEnteredAt = System.currentTimeMillis()
        plotUnloading = false
        scanGridData()
        regenRender()
    }

    fun unplanCurrentGreenhouse(): Boolean {
        val grid = getCurrentGrid() ?: run {
            ChatUtils.sendWithPrefix("Not standing in a greenhouse.")
            return false
        }

        return unplanGreenhouse(grid)
    }

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

        val listening = plantDiagnosticListeningElement
        val hit = plantDiagnosticHitBaseBlock
        plantDiagnosticListeningElement = null
        plantDiagnosticHitBaseBlock = null

        when (event.title) {
            "Crop Diagnostics" -> PlantDiagnostics.readDiagnosticTool(realItems, listening, hit)
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
            slot.soil = Blocks.AIR
        } else {
            grid.removePlantWithBlockAt(pos)
        }

        markBlocksDirty(pos)
    }

    @EventHandler
    fun onBlockPlaced(event: BlockPlacedEvent) {
        regenRender()

        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val blockVec3 = Vec3.atCenterOf(event.pos)
        if (grid.plot?.aabb?.contains(blockVec3) != true) return

        markBlocksDirty(event.pos)
    }

    @EventHandler
    fun onEntityAdded(event: EntityAddedEvent) {
        val gridArea = PlotAPI.getCurrentPlot()?.getBuildableArea() ?: return
        val arrived = event.addedEntityList.map { it.entity.position() }.filter { gridArea.contains(it) }
        if (arrived.isEmpty()) return

        lastEntityChangeAt = System.currentTimeMillis()

        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        markBlocksDirty(arrived.map { BlockPos.containing(it) })
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
        slot.soil = event.packet.blockState.block

        markBlocksDirty(event.packet.pos)
    }


    @EventHandler
    fun onEntityRemoved(event: EntityRemovedEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val area = grid.plot?.getBuildableArea() ?: return
        markBlocksDirty(event.removedEntityList.map { it.entity.position() }.filter { area.contains(it) }.map { BlockPos.containing(it) })
    }

    @EventHandler
    fun onLevelUnloading(event: LevelUnloadingEvent) {
        plotUnloading = true
        dirtyBlocks.clear()
        standTargets.clear()
        scanDeferredSince = null
        lastChangeAt = null
        shouldRescanCurrentPlot = false
    }

    @EventHandler
    fun onAttackEntity(event: AttackEntityEvent) {
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        val area = grid.plot?.getBuildableArea() ?: return
        if (!area.contains(event.target.position())) return

        markBlocksDirty(BlockPos.containing(event.target.position()))
    }

    @EventHandler
    fun onInteractEntity(event: InteractEntityEvent) {
        val entityBlockPos = BlockPos.containing(event.target.position())
        plantDiagnosticHitBaseBlock = BlockPos(entityBlockPos.x, GREENHOUSE_SOIL_Y, entityBlockPos.z)
        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return
        val standTarget = event.target as? ArmorStand ?: return

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

            val footprint = foundCrop.footprint
            val pos = aimed.offset(-((footprint.width - 1) / 2), 0, -((footprint.height - 1) / 2))

            playerPlacements.add(PlayerPlacement(foundCrop, pos, Instant.now()))

            val soil = BlockPos(pos.x, GREENHOUSE_SOIL_Y, pos.z)
            cropPlacements.entries.removeAll { (at, placed) -> overlaps(at, placed.def, soil, foundCrop) }
            cropPlacements[soil] = CropPlacement(foundCrop, System.currentTimeMillis())
            markBlocksDirty(pos)
        }
    }


    fun warnUnknownValues(): Boolean {
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
        if (GreenhouseTickTime.speedAttribute() == null) {
            warnings.add(
                ChatUtils.buildWithCommand(
                    "Unknown Timestalk attribute, defaulting to zero. Click to set it",
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
        plant.appearedAt = System.currentTimeMillis()

        val stage = plant.growthStage
        if (stage is PlantStage.Estimated && plant.cropDef.stagePlacedAt in stage.range) {
            plant.growthStage = PlantStage.Known(plant.cropDef.stagePlacedAt)
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


    override fun claimSpawnedMutation(plant: Plant, layout: PlotLayout) {
        val grown = ((plant.lowestStage ?: 1) - 1).coerceAtLeast(0)
        val now = Instant.now()


        if (plant.cropDef.drainsNeighbours) {
            plant.waterLevel = PlotPrediction.DRAIN_PER_STAGE * grown
            plant.waterExact = false
        } else if (plant.cropDef.needsWater) {
            val waterEffect = GreenhouseGrid.waterEffectAt(layout, plant.slot)
            val predictedWater = PlotPrediction.waterLevelAfter(0.0, grown, waterEffect)
            // a spawn still standing is alive
            plant.waterLevel = PlotPrediction.lowestWaterLevelStillAlive(predictedWater, waterEffect)
            plant.waterExact = predictedWater > PlotPrediction.WATER_DEATH_LEVEL
        }

        plant.waterBestCase = null
        plant.appearedAt = (gardenArrivedAt ?: now).toEpochMilli()
        plant.firstSeenStage = 1
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

    fun scannedPlantAtBlock(pos: BlockPos): ScannedPlant? =
        scannedGrid()?.let { grid -> plantAtBlock(pos, grid) }

    fun scannedPlantAtStand(stand: ArmorStand): ScannedPlant? =
        scannedGrid()?.let { grid -> plantAtStand(stand, grid) }

    fun isPlannedIngredient(plant: Plant): Boolean {
        val planned = scannedGrid()?.plannedPlantAt(plant.slot.x, plant.slot.y) ?: return false

        return planned.slot.mark == LayoutSlot.Marking.Ingredient && planned.acceptsCrop(plant.cropDef)
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
        return CropRegistry.allCrops
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