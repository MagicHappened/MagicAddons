package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.commands.debug.CropCollector
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
import org.magic.magicaddons.events.greenhouse.PlotChangedEvent
import org.magic.magicaddons.events.interact.*
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
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

    /**
     * Whether the plot has changed since the last look.
     *
     * There was a wait here, half a second of quiet before scanning so a burst of changes cost one
     * scan rather than twenty. It bought little: a stage that knows its rotation is matched at the
     * one rotation the grid gives its block instead of six, which made a scan cheap enough to run
     * on the change itself, and waiting only meant the screen was a beat behind the world.
     */
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

    /** The tick already warned about, so ten minutes of danger is one message rather than ten. */
    private var dehydrationWarnedFor: Instant? = null

    private val DEATH_WARNING_WINDOW: Duration = Duration.ofMinutes(10)

    /**
     * Says which plants the coming tick will kill, while there is still time to water them.
     *
     * Asked when the countdown is inside the warning window, and once per tick: the danger does
     * not change by being restated. A plant already past death in the estimate is not warned
     * about, since the dead bush on its slot is already saying something stronger.
     */
    private fun warnOfDyingPlants() {
        val nextTick = miscInfo.nextTickTime ?: return
        val now = Instant.now()

        if (Duration.between(now, nextTick) > DEATH_WARNING_WINDOW) return
        if (dehydrationWarnedFor == nextTick) return

        val dying = mutableListOf<Pair<String, String>>()

        greenhouseGrids.forEach { grid ->
            grid.layout.elementInstances.forEach { instance ->
                if (!instance.cropDef.needsWater) return@forEach

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
                    dying += instance.cropDef.name to grid.layout.displayName()
                }
            }
        }

        if (dying.isEmpty()) return

        dehydrationWarnedFor = nextTick
        sendDehydrationWarning(dying)
    }

    /**
     * The warning itself, plants grouped by the greenhouse they are dying in, with a way home at
     * the end when the player is anywhere else.
     */
    fun sendDehydrationWarning(dying: List<Pair<String, String>>) {
        val byHouse = dying.groupBy({ it.second }, { it.first })

        val message = Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("About to die of thirst: ").withStyle(ChatFormatting.RED))

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
        }

        Minecraft.getInstance().player?.sendSystemMessage(message)
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

    /**
     * How far the server may fall behind the wall clock before the gap is read as an absence.
     *
     * Measured as time the server never accounted for, not as time passed: the check itself only
     * runs once a minute, so every ordinary look back sees a minute of wall clock and would read
     * as leaving if elapsed time were the question. What separates being away from being lagged is
     * that a server which is merely behind still sends ticks, so its time nearly keeps up, while
     * one nobody is listening to sends none at all.
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

            // a server that merely stalls still sends ticks, so its time nearly keeps up with the
            // wall clock. One nobody is listening to sends none, and the whole gap arrives as
            // unaccounted time. Handing that to the countdown as lag would push it by the length
            // of an absence rather than the length of a stall
            val unaccountedMs = realMs - serverMs

            if (Duration.ofMillis(unaccountedMs) > AWAY_THRESHOLD) {
                lastCheckTime = now
                return
            }
            // how far the server fell behind the wall clock since the last look. Bounded, because
            // this is meant to nudge a countdown that has drifted, and one long pause or one
            // skipped update should not be able to move it by more than the gap it is measuring
            val adjustmentDelta = unaccountedMs
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

        // the countdown having run out is itself a tick. passedGrowthTicks counts whole periods
        // gone by since then, so an hour past a two hour tick counts none of them while the tick
        // it ran out on has plainly happened. The clock always knew this and moved on by one more
        // than it counted; the plants were moved on by the count alone, so a greenhouse ticked
        // once and was never told
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

            // a greenhouse that has not been read this session has no count of what it owes, and
            // owing nothing yet is not a reason to skip it: the ticks it is about to be told about
            // are exactly the ones nobody was there to see
            val pendingTicks = grid.state.pendingGrowthTicks ?: 0

            grid.state.pendingGrowthTicks = pendingTicks + elapsedTicks
            grid.state.needsUpdate = true

            // nobody is looking at this greenhouse, so the clock is all we have to go on
            grid.predictGrowth(elapsedTicks, growthTickMs)
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
            warnOfDyingPlants()
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

    /**
     * Takes the plan off the greenhouse being stood in.
     *
     * The plan is a thing the player turned on and can turn off, so both the button on the screen
     * and the word in chat come here rather than each reaching into the grid themselves.
     */
    fun unplanCurrentGreenhouse(): Boolean {
        val grid = getCurrentGrid() ?: run {
            ChatUtils.sendWithPrefix("Not standing in a greenhouse.")
            return false
        }

        return unplanGreenhouse(grid)
    }

    /**
     * Takes the plan off [grid], whichever greenhouse that is.
     *
     * The screen shows whichever greenhouse was picked from its selector rather than the one being
     * stood in, so the button there has to say which it means. Only the chat answer is about where
     * the player happens to be standing, since that is what it was asked about.
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
     * A plant is taken apart by hitting it, and what is hit is a stand rather than a block, so
     * nothing about breaking one reaches the block listener. Without this a harvested crop stayed
     * on the screen until something else in the plot happened to change.
     */
    /**
     * A plant taken apart leaves when the server says so, a moment after the swing that did it.
     * Asking on the swing looked at a plot the crop was still standing in, so the screen stayed a
     * whole harvest behind: the first break showed up only once the second was swung for.
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

    /**
     * What the server says when a placement did not happen.
     *
     * A crop is claimed on the strength of what was in hand when the block went down, but the
     * server has the last word and takes a moment to say it. Any of these means the plant never
     * went in, so the claim is dropped rather than left to land on whatever the read finds in
     * that slot next.
     */
    private val PLACE_REFUSALS: List<Regex> = listOf(
        Regex("can only grow on ", RegexOption.IGNORE_CASE),
        Regex("There is already a crop planted here", RegexOption.IGNORE_CASE),
        Regex("You cannot build here", RegexOption.IGNORE_CASE)
    )

    @EventHandler
    fun onPlacementRefused(event: OnSystemChatEvent) {
        if (placedCrop == null) return
        if (PLACE_REFUSALS.none { it.containsMatchIn(event.text) }) return

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
        // a stage names the stands and blocks it matched on and no others, so pointing the tool
        // at a hunger bar, a sleep bubble, or a block the stage never mentioned found nothing and
        // the plant went on wearing whatever had been guessed for it. Anything standing inside a
        // plant's footprint belongs to that plant, named or not
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

        // these two are still read out of a numbered piece, the way the stage used to be, so when
        // one comes up empty the page it came from is worth seeing before guessing at a label
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
                miscInfo.nextTickTime = Instant.now().plusMillis(nextStage.parseDurationToMs())
                lastCheckTime = Instant.now()

                realignWithGame()
            }
        }

        // what the pages just said about the plant that was pointed at, whether or not anyone is
        // collecting. Telling the greenhouse what it is looking at is the tool's other job, and
        // the one thing here the game knows better than any guess: the stage exactly, the water
        // exactly, and how long it has been standing there
        plantDiagnosticListeningElement?.let { element ->
            age?.parseDurationToMs()?.let { element.instance.age = it }
            stageRaw?.let { element.instance.growthStage = GrowthStageInfo.Known(it) }

            // read rather than predicted, so whatever was assumed about the ticks it may have been
            // passed over for no longer applies
            waterLevel?.let {
                element.instance.waterLevel = it
                element.instance.waterPredictedInDebt = false
            }
        }

        if (!CropCollector.isActive()) return

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

        // the only caller left, and it rescans the whole footprint itself, so what the tool
        // hands over is the crop and the stage the page just named
        CropCollector.correct(def, stageRaw)
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

    /**
     * Re-anchors the server tick to the countdown the game itself just gave us.
     *
     * Without this, the next comparison measured real time from the resync against server time
     * from wherever the counter was last touched, and handed the difference to the countdown as
     * lag.
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