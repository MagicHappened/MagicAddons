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
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.commands.internal.farming.SetTimestalkAttribute
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.FireElement
import org.magic.magicaddons.data.greenhouse.plot.*
import org.magic.magicaddons.data.handlers.DataHandler
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
import org.magic.magicaddons.features.farming.greenhousePresets.warnings.PlantWarnings
import org.magic.magicaddons.ui.widgets.config.SettingDetail
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ServerUtils
import org.magic.magicaddons.util.center
import org.magic.magicaddons.util.getBuildableArea
import org.magic.magicaddons.util.toShortDuration
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import java.util.Optional
import net.minecraft.ChatFormatting
import net.minecraft.world.phys.AABB
import net.minecraft.network.chat.Style
import org.magic.magicaddons.util.compat.McCompat
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
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
        if (DataHandler.activeProfile == null) return
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

    private fun plotReady(plot: Plot): Boolean {
        if (plotUnloading) return false

        val level = Minecraft.getInstance().level ?: return false
        val area = plot.getBuildableArea()

        if (!chunksLoadedOver(level, area)) return false

        val now = System.currentTimeMillis()

        val quietSince = maxOf(lastEntityChangeAt ?: 0L, plotEnteredAt)

        if (standsStillMoving(level) == 0 && now - quietSince >= ENTITY_QUIET_MS) return true

        return now - (lastChangeAt ?: now) >= MAX_SCAN_DEFER_MS
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
        standTargets.entries.removeIf { (entityId, target) ->
            val standing = level.getEntity(entityId)?.position() ?: return@removeIf true
            standing.distanceToSqr(target) <= STAND_ARRIVED_WITHIN_SQR
        }

        return standTargets.size
    }

    private val standTargets: MutableMap<Int, Vec3> = HashMap()

    private const val STAND_ARRIVED_WITHIN_SQR: Double = 1.0e-6

    private var plotUnloading: Boolean = false

    private var plotEnteredAt: Long = 0L

    private const val ENTITY_QUIET_MS: Long = 250

    /** counted from the plot's last change */
    private const val MAX_SCAN_DEFER_MS: Long = 3_000

    private var lastEntityChangeAt: Long? = null

    fun noteEntityChanged(entityId: Int, movingTo: Vec3? = null) {
        if (!greenhousesInitialized) return

        val gridArea = PlotAPI.getCurrentPlot()?.getBuildableArea() ?: return
        val entity = Minecraft.getInstance().level?.getEntity(entityId) ?: return
        if (!gridArea.contains(entity.position())) return

        lastEntityChangeAt = System.currentTimeMillis()
        if (movingTo == null) return
        standTargets[entityId] = movingTo

        val grid = getCurrentGrid() ?: return
        if (!grid.isScanned()) return

        // a stand moving invalidates every cell, so plants in between are properly scanned
        val from = BlockPos.containing(entity.position())
        val to = BlockPos.containing(movingTo)

        markBlocksDirty(BlockPos.betweenClosed(from, to).map { it.immutable() })
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

        // read again on a later tick, once the rest of the plot has been sent
        if (!plotReady(plot)) {
            shouldRescanCurrentPlot = true
            return
        }

        grid.plot = plot

        grid.readSoilBlocks()

        // a merge, so whatever the plot cannot say for a plant that is still there is carried over,
        // and any stage predicted while away is corrected by what is actually standing
        if (!grid.rescanPlants()) return

        // the plan is laid the way the plot agrees with, judged afresh on arriving: what stands now
        // says which way it was built, and a turn picked on assign may be stale by then
        if (!isPlanTurned) {
            grid.state.assignedLayout?.let { plan ->
                val turns = grid.compareRotations(plan, grid.state.planTurns)
                if (turns != grid.state.planTurns) {
                    grid.state.planTurns = turns
                    ChatUtils.sendWithPrefix("Plan on ${grid.layout.displayName()} re-positioned at ${turns * 90}°")
                }
            }
            isPlanTurned = true
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
        if (!plotReady(plot)) {
            shouldRescanCurrentPlot = true
            return
        }
        grid.plot = plot

        grid.readSoilBlocks()
        if (!grid.rescanPlants(region)) return
        claimPlantedCrop(grid)
        LayoutRenderState.refresh()
    }

    private const val MISSING_COLOR: Int = 0xFFFF8855.toInt()

    fun absenceForChorusDetail(): SettingDetail? {
        val ticks = GreenhousePresets.chorusAbsenceTicks() ?: return null

        val tickMs = GreenhouseTickTime.tickMs
        val remaining = GreenhouseTickTime.remainingTickMs()

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



    fun inGarden(): Boolean = LocationAPI.island == SkyBlockIsland.GARDEN

    fun inOwnGarden(): Boolean = inGarden() && !LocationAPI.isGuest

    // cant detect someone elses greenhouse plot without some like weird block detection so its left out
    fun inOwnGreenhouse(): Boolean = inOwnGarden() && PlotAPI.getCurrentPlot()?.data?.isGreenhouse == true

    fun getCurrentGrid(): GreenhouseGrid? {
        if (!inOwnGarden()) return null

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
            LocationAPI.island == SkyBlockIsland.GARDEN &&
                    !LocationAPI.isGuest &&
                    lastCheckTime != null

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
        val id = runCatching { ProfileAPI.profileId }.getOrNull() ?: return
        DataHandler.switchProfile(id, ProfileAPI.profileName ?: return)
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
        if (!joiningSkyBlock || !greenhousesInitialized || DataHandler.activeProfile == null) return

        checkForGrowthTickUpdate()
        joiningSkyBlock = false
    }

    @EventHandler
    fun onTick(event: WorldTickEvent) {
        switchProfileIfChanged()
        if (DataHandler.activeProfile == null) return

        OtherProfiles.advanceTicks()
        updateTickTimeAfterJoin()
        checkForTickTimeUpdate()
        PlantWarnings.onTick()

        if (!inOwnGarden()) return

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
            DataHandler.saveGardenData()
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
        scoreboardLines = emptyList()
        pestDebuffActive = false
        GreenhouseSpawnLog.onGameClosing()
        DataHandler.saveGardenData()
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
        if (!inOwnGreenhouse()) return
        scoreboardLines = event.newComponents
        readPestDebuff()
    }

    private fun checkPlotChange() {
        if (!inOwnGarden()) return

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

        // the diagnosis reads what the tool was last pointed at, and any container ends that pointing
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