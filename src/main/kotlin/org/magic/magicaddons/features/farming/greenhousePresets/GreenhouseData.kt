package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.commands.debug.CropStageExporter
import org.magic.magicaddons.util.getBuildableArea
import org.magic.magicaddons.util.parseDurationToMs
import org.magic.magicaddons.util.isCardinalYaw
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
import org.magic.magicaddons.events.greenhouse.PlotChangedEvent
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
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

    var checkGreenhouses = false
    var greenhousesInitialized = false
    var greenhouseGrids = mutableListOf<GreenhouseGrid>()
    var presetGrids = mutableListOf<GreenhouseLayout>()
    var miscInfo = MiscGreenhouseInfo()
    var lastGridLayouts = mutableListOf<GreenhouseLayout>()

    var currentPreset: GreenhouseLayout? = null
    var currentGridIndex: Int = 0

    var lastCheckTime: Instant? = null
    var lastServerTick: Long? = null

    var removedElementByAttack: ElementRuntimeState? = null
    private var placedCrop: Pair<CropDefinition, BlockPos>? = null
    private var placedCropAt: Instant? = null

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

    /** How long the plot has to be still before it is read again. */
    private val RECONCILE_QUIET: Duration = Duration.ofMillis(500)

    /** How long a steady stream of changes may put a read off for. */
    private val RECONCILE_CEILING: Duration = Duration.ofSeconds(2)

    private var plantDiagnosticHitBaseBlock: BlockPos? = null
    private var plantDiagnosticListeningElement: ElementRuntimeState? = null
    // ^^ temp


    private fun initKnownIds() {
        if (checkGreenhouses) return
        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach { plot ->
            if (plot.data?.isGreenhouse != true) return@forEach
            val existingGrid = greenhouseGrids.find { "plot_${plot.id}" == it.layout.id }
            existingGrid ?: run {
                val gridLayout = GreenhouseLayout(
                    id = "plot_${plot.id}",
                    name = "unnamed"
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
        val result = grid.setPlantData()

        claimPlantedCrop(grid)

        // the plan on screen is read off the plot, so it is only right until the plot changes
        LayoutRenderState.refresh()

        if (result.changed) {
            ChatUtils.sendWithPrefix(
                "Greenhouse changed: ${result.added} new, ${result.removed} gone, ${result.replaced} different"
            )
        }

        // after grid update
        grid.state.hasRuntimeReferences = true
        grid.state.needsUpdate = false
        grid.state.lastUpdateTimestamp = Instant.now()
        grid.state.pendingGrowthTicks = 0

        ChatUtils.sendWithPrefix("Successfully scanned data for ${plot.id}")
    }


    /**
     * When the plot the player is standing in should be read again.
     *
     * Every change in the plot pushes this back, so a burst of them settles into one read, and
     * [reconcileDeadline] stops a steady stream of changes from putting it off forever.
     */
    /**
     * The most one look at the clock may move the next tick by.
     *
     * The correction assumes the greenhouse runs on server ticks rather than on the wall clock, so
     * it pulls the countdown back by however far the server fell behind. That direction has not
     * been proven, only reasoned, so the size of any single correction is capped: a wrong sign then
     * costs a little accuracy rather than walking the countdown away from the truth.
     */
    private const val MAX_TICK_ADJUSTMENT_MS: Long = 5_000

    /** The hunting shard carrying greenhouse speed, a legendary one, twenty four syphons to max. */
    const val GREENHOUSE_SPEED_ATTRIBUTE_ID: String = "attribute:l57"

    private var reconcileDueAt: Instant? = null
    private var reconcileDeadline: Instant? = null

    /** Something in the plot changed, so what is stored for it can no longer be trusted. */
    fun requestReconcile() {
        val now = Instant.now()

        reconcileDueAt = now.plus(RECONCILE_QUIET)

        if (reconcileDeadline == null) {
            reconcileDeadline = now.plus(RECONCILE_CEILING)
        }
    }

    /** Runs a reconcile once the plot has gone quiet, or once it has been put off long enough. */
    private fun runDueReconcile() {
        val dueAt = reconcileDueAt ?: return
        val now = Instant.now()

        if (now.isBefore(dueAt) && now.isBefore(reconcileDeadline ?: dueAt)) return

        reconcileDueAt = null
        reconcileDeadline = null

        getCurrentGrid()?.state?.needsUpdate = true
        scanGridData()
    }

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        runDueReconcile()
    }

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

            // a path that leaves without updating lastCheckTime makes the next call measure real
            // time across the whole gap while the server side of it covers only the last moment,
            // and the correction that comes out of that is nonsense
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
            // how far the server fell behind the wall clock since the last look. Bounded, because
            // this is meant to nudge a countdown that has drifted, and one long pause or one
            // skipped update should not be able to move it by more than the gap it is measuring
            val adjustmentDelta = (realMs - serverMs)
                .coerceIn(-MAX_TICK_ADJUSTMENT_MS, MAX_TICK_ADJUSTMENT_MS)

            // added, not taken off. The greenhouse counts in server ticks, so time the server
            // spent behind the wall clock is time the tick has not yet served and the countdown
            // still owes. Taking it off ran the screen a few seconds ahead of the game, which is
            // how the direction was settled
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
            val pendingTicks = grid.state.pendingGrowthTicks ?: return@forEach

            grid.state.pendingGrowthTicks = pendingTicks + passedGrowthTicks.toInt()
            grid.state.needsUpdate = true

            // nobody is looking at this greenhouse, so the clock is all we have to go on
            grid.predictGrowth(passedGrowthTicks.toInt(), growthTickMs)
        }
    }

    @Subscription
    fun onTick(event: TickEvent) {
        val now = Instant.now()
        val last = lastCheckTime
        if (
            last == null ||
            last.plusSeconds(60).isBefore(now) ||
            miscInfo.nextTickTime?.isBefore(now) ?: false
        ) {
            checkForUpdate()
        }
    }


    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
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
            val pos = event.hit.blockPos.relative(event.hit.direction)

            placedCrop = Pair(foundCrop, pos)
            placedCropAt = Instant.now()
            requestReconcile()

            return
        }


    }


    fun warnUnknownValues(sendWarning: Boolean = true): Boolean {
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
     * Marks a plant the player just put down as new.
     *
     * The server decides whether a placement happened at all, so nothing is added here: the read
     * that follows finds the plant, or it does not because the server refused. All this supplies is
     * what a read cannot know, that this particular plant went in just now and so is at no age and
     * holds no water. A crop the read never found, or found as something else, is simply forgotten
     * once the window passes.
     */
    private fun claimPlantedCrop(grid: GreenhouseGrid) {
        val (definition, pos) = placedCrop ?: return
        val placedAt = placedCropAt

        if (placedAt == null || Instant.now().isAfter(placedAt.plus(PLACE_WINDOW))) {
            forgetPlacedCrop()
            return
        }

        val slot = grid.getSlotAt(pos, false) ?: return
        val element = grid.elementCovering(slot) ?: return

        if (element.instance.cropDef != definition) return

        element.instance.age = 0L
        if (definition.needsWater) element.instance.waterLevel = 0

        forgetPlacedCrop()
    }

    private fun forgetPlacedCrop() {
        placedCrop = null
        placedCropAt = null
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
            ChatUtils.sendWithPrefix(
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

        // these two are still read out of a numbered piece, the way the stage used to be, so when
        // one comes up empty the page it came from is worth seeing before guessing at a label
        if (waterLevel == null) {
            ChatUtils.sendWithPrefix("Could not read the water level, the water page reads:")
            dumpLore(bucketLore)
        }

        if (status == null) {
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
                miscInfo.nextTickTime = Instant.now().plusMillis(nextStage.parseDurationToMs())
                lastCheckTime = Instant.now()
            }
        }

        // the whole point of pointing the tool at a plant is being told what the mod makes of it,
        // so every way of learning nothing says so rather than returning quietly
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

        ChatUtils.sendWithPrefix("${def.name}, stage $stageRaw of ${def.maxStage}")

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

        val matchingStage = def.stageDefs.find { stageDef ->
            stageRaw in stageDef.stageRange
        }
        plantDiagnosticListeningElement?.let {
            it.instance.age = age?.parseDurationToMs()
            it.instance.growthStage = GrowthStageInfo.Known(stageRaw)
            it.instance.waterLevel = waterLevel
        }
        val level = Minecraft.getInstance().level ?: return
        var abnormalRotationFound = false
        val element = plantDiagnosticHitBaseBlock?.let { block ->
            val minX = block.x.toDouble()
            val minY = block.y.toDouble()
            val minZ = block.z.toDouble()

            // the crop's own footprint, not the one set by hand for exporting an unknown plant.
            // A three by three searched as a one by one covers a ninth of itself and finds almost
            // none of its stands, so it never matches
            val maxX = (block.x + def.footprint.width).toDouble()
            val maxY = (block.y + 15).toDouble()  // height
            val maxZ = (block.z + def.footprint.height).toDouble()

            val box = AABB(
                minX, minY, minZ,
                maxX, maxY, maxZ
            )

            // the plot's own marker stands are not part of any plant, and counting them is how one
            // crop ended up reporting four stands that nothing is drawing
            val armorStands = level.getEntitiesOfClass(ArmorStand::class.java, box)
                .filterNot { it.isMarker }
                .toMutableList()

            // a stand that is not small is rebuilt wrong unless its definition says so, and the
            // definitions take small as read
            val fullSized = armorStands.filterNot { it.isSmall }

            // one line for the crop, not one per stand: several stands of the same plant share a
            // block, so naming each of them says the same thing three times over
            if (fullSized.isNotEmpty()) {
                ChatUtils.sendWithPrefix(
                    "${fullSized.size} of ${def.name}'s stands are not small, its definition needs isSmall = false"
                )

                // exact positions, since stands of one plant share a block and only the fractions
                // tell a stack of three apart from three sitting a hair from each other
                fullSized.forEach {
                    ChatUtils.send(
                        Component.literal(
                            "  %.4f %.4f %.4f".format(it.x, it.y, it.z)
                        ).withStyle(ChatFormatting.DARK_GRAY)
                    )
                }
            }

            abnormalRotationFound = armorStands.any {
                ((it.headPose.x != 0.0f || it.headPose.y != 0.0f || it.headPose.z != 0.0f) ||
                        (!it.xRot.isCardinalYaw() || !it.yRot.isCardinalYaw()))
                        && PlayerUtils.getSkinHash(it.getItemBySlot(EquipmentSlot.HEAD)) != null
            }
            GreenhouseGrid.findElementAtBasePos(
                block,
                armorStands
            )
        }

        if (element == null) {
            ChatUtils.sendWithPrefix("Nothing here matched ${def.name}, so it is not being tracked.")
        } else {
            ChatUtils.sendWithPrefix(
                "Matched ${element.instance.elementId} at ${element.instance.growthStage}"
            )
        }

        val isSelf = UUID.fromString("eef58b9d-39e1-4062-8a1a-2f921f14a46d") == Minecraft.getInstance().player?.uuid
        val override = false
        if (matchingStage != null){
            if (matchingStage.needsRotationData(abnormalRotationFound)){
                ChatUtils.sendWithPrefix("No rotation data for ${def.name}")
                plantDiagnosticHitBaseBlock?.let {
                    CropStageExporter.copyCropStageData(it,stageRaw, def, !isSelf)
                }
            }
            else if (override){
                ChatUtils.sendWithPrefix("Overridden ${def.name}")
                plantDiagnosticHitBaseBlock?.let {
                    CropStageExporter.copyCropStageData(it,stageRaw, def, !isSelf)
                }
            }
        }
        else {
            ChatUtils.sendWithPrefix("No matching stage for ${def.name} please send the copied output")
            plantDiagnosticHitBaseBlock?.let {
                CropStageExporter.copyCropStageData(it,stageRaw, def, !isSelf)
            }
        }
    }
    /**
     * The value written beside [label] on a diagnosis page.
     *
     * Read off the whole line rather than out of a numbered piece of it. The server splits a line
     * wherever its own formatting changes, so "Stage: 1/15" arrives as three pieces and the one
     * sitting at any given number is whatever the colouring happened to make it, which is how
     * reading the stage ended up reading the slash.
     */
    private fun List<Component>.valueFor(label: String): String? =
        firstOrNull { it.string.trimStart().startsWith("$label:", ignoreCase = true) }
            ?.string
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /**
     * Every line of [lore] as it reads and as the pieces it is built from.
     *
     * The stage is pulled out of one particular piece of one particular line, so when that stops
     * working the only useful thing to say is what was actually there. Each line copies whole, to
     * be pasted somewhere it can be read properly.
     */
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

    fun CropStage.needsRotationData(abnormalRotation: Boolean): Boolean =
        abnormalRotation && this.armorStands?.let { stands ->
            stands.isNotEmpty() && !stands.any {
                it.headRotation != null && it.xRotation != null && it.yRotation != null
            }
        } ?: true

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
     * How long one growth tick currently takes, or null while something the formula needs is not
     * known. Changes as uniques are planted, so it is worked out rather than remembered.
     */
    /**
     * The greenhouse speed attribute, worth half a percent a level.
     *
     * Taken from what the player told us first, and only then from the shard they have syphoned.
     * That order is the wrong way round in principle, since the game knows better than the player
     * does, but the api that reports shards does not report this one at all, so what it says cannot
     * be trusted over what was typed. Worth turning back around once that is fixed upstream.
     */
    fun greenhouseSpeedAttribute(): Int? =
        miscInfo.greenhouseSpeedAttribute
            ?: AttributeAPI.attributeMap.entries
                .firstOrNull { it.key.id == GREENHOUSE_SPEED_ATTRIBUTE_ID }
                ?.value
                ?.level
                ?.takeIf { it > 0 }

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

    /**
     * What is left of the tick now running, or null while the next one is not known. Never
     * negative: an overdue tick has nothing left of it rather than a debt.
     */
    fun remainingTickMs(): Long? {
        val next = miscInfo.nextTickTime ?: return null

        return (next.toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0L)
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