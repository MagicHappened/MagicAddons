package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.GreenhouseElementFactory
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseSlot
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnBlockDestroyedEvent
import org.magic.magicaddons.events.interact.OnBlockPlacedEvent
import org.magic.magicaddons.events.interact.OnBlockUseEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.ScreenUtil
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.InventoryChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ScreenInitializedEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.extentions.isSkyblockFiller
import kotlin.math.abs

object GreenhouseData {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    private const val BUILD_OFFSET = 43
    private const val GRID_SIZE = 10
    private const val Y_LEVEL = 73

    private val knownGreenhouseIds = mutableSetOf<Int>()
    private var knownIdsInitialized = false

    private val initializedGreenhouseIds = mutableSetOf<Int>()
    private val greenhouseList = mutableListOf<GreenhouseGrid>()

    private val elementsBySoil: Map<Block, List<CropDefinitionProvider>> =
        GreenhouseElementFactory.getAllFactories()
            .map { factory -> factory() }
            .flatMap { provider ->
                provider.definition.requiredSoil.map { soil ->
                    soil to provider
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )

    private var listeningElement: ElementRuntimeState? = null

    val greenhouses: List<GreenhouseGrid>
        get() = greenhouseList

    val initializedIds: Set<Int>
        get() = initializedGreenhouseIds

    val knownIds: Set<Int>
        get() = knownGreenhouseIds

    val grids: List<GreenhouseGrid>
        get() = greenhouseList




    private fun initKnownIds() {
        if (knownIdsInitialized) return
        if (PlotAPI.plots.any { it.data == null }) return

        PlotAPI.plots.forEach {
            if (it.data?.isGreenhouse == true) {
                knownGreenhouseIds.add(it.id)
            }
        }

        knownIdsInitialized = true
    }

    private fun initData() {
        if (!knownIdsInitialized) return

        val plot = PlotAPI.getCurrentPlot() ?: return
        val plotId = plot.id

        if (plotId !in knownGreenhouseIds) return
        if (plotId in initializedGreenhouseIds) return

        val grid = createGrid(plot) ?: return

        setPlantData(grid)

        greenhouseList.add(grid)
        initializedGreenhouseIds.add(plotId)

        ChatUtils.sendWithPrefix("Initialized Greenhouse: $plotId")
    }

    private fun createGrid(plot: Plot): GreenhouseGrid? {
        val world = Minecraft.getInstance().level ?: return null

        val buildArea = plot.getBuildableArea()

        val grid = GreenhouseGrid()
        grid.plot = plot

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()
        val maxX = buildArea.maxX.toInt()
        val maxZ = buildArea.maxZ.toInt()

        var gridX = 0
        for (x in minX until maxX) {

            var gridY = 0
            for (z in minZ until maxZ) {

                val pos = BlockPos(x, Y_LEVEL, z)
                val state = world.getBlockState(pos)
                val unlocked = state.block != Blocks.PODZOL

                grid.setSlot(
                    GreenhouseSlot(gridX, gridY, unlocked, state)
                )


                gridY++
            }
            gridX++
        }

        return grid
    }

    private fun setPlantData(grid: GreenhouseGrid) {
        val visitedSlots = Array(GRID_SIZE) { BooleanArray(GRID_SIZE) }

        val level = Minecraft.getInstance().level ?: return
        val buildableArea = grid.plot?.getBuildableArea() ?: return

        val stands = level.getEntities(null, buildableArea) ?: return
        val remainingStands = stands.toMutableList()

        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                if (visitedSlots[y][x]) continue

                val slot = grid.getSlot(x, y) ?: continue
                val soil = slot.placedBlock?.block ?: continue

                val candidates = elementsBySoil[soil] ?: continue
                val origin = grid.getPosForSlot(slot) ?: continue

                val state = level.getBlockState(origin.offset(0,1,0))

                if (state.`is`(Blocks.FIRE)) {
                    grid.elementInstances.add(
                        GreenhouseElementInstance(
                            elementId = "fire",
                            slot = slot
                        )
                    )

                    grid.elements.add(
                        ElementRuntimeState(
                            null,
                            slot,
                            null,
                            null,
                            mapOf(
                                origin.offset(0,1,0) to state
                            ),
                            "Fire",
                            {graphics, x, y, width, height ->
                                val sprite = ScreenUtil.getSpriteForState(
                                    Blocks.FIRE.defaultBlockState(), Direction.NORTH
                                )
                                sprite ?: return@ElementRuntimeState
                                graphics.blitSprite(
                                    RenderPipelines.GUI_TEXTURED,
                                    sprite,
                                    x, y, width, height
                                )
                            }
                        )
                    )
                    visitedSlots[y][x] = true
                    continue
                }

                var matchedState: ElementRuntimeState? = null
                var matchedDef: CropDefinition? = null

                for (candidate in candidates) {
                    val def = candidate.definition

                    if (x + def.footprint.width > GRID_SIZE ||
                        y + def.footprint.height > GRID_SIZE
                    ) continue

                    val stages = def.stageDefs.flatMap { stageDef ->
                        when (stageDef) {
                            is CropStage -> listOf(stageDef)
                            is CropStagePattern -> stageDef.expand()
                        }
                    }

                    var bestStage: CropStage? = null
                    var bestGrowth: GrowthStageInfo? = null
                    var bestScore = -1
                    var usedStands: List<Entity>? = null
                    var blocks: Map<BlockPos,BlockState>? = null

                    for (stage in stages) {
                        val result = stage.matchesStage(origin, remainingStands)
                        if (!result.matched) continue

                        if (result.score <= bestScore) continue

                        bestScore = result.score
                        bestStage = stage
                        usedStands = result.usedStands
                        blocks = result.matchedBlocks

                        val range = stage.stageRange
                        bestGrowth = if (range.first == range.last) {
                            GrowthStageInfo.Known(range.first)
                        } else {
                            GrowthStageInfo.Estimated(range)
                        }
                    }

                    if (bestStage == null || bestGrowth == null || usedStands == null || blocks == null) continue

                    matchedState = ElementRuntimeState(def, slot, bestGrowth,usedStands, blocks)
                    matchedDef = def

                    remainingStands.removeAll(usedStands)
                    break
                }

                val runtime = matchedState ?: continue
                val def = matchedDef ?: continue

                for (dy in 0 until def.footprint.height) {
                    for (dx in 0 until def.footprint.width) {
                        visitedSlots[y + dy][x + dx] = true
                    }
                }

                grid.elementInstances.add(
                    GreenhouseElementInstance(
                        elementId = def.skyblockId.toString(),
                        slot = slot,
                        growthStage = runtime.growthStage
                    )
                )

                grid.elements.add(runtime)
            }
        }
    }

    fun debug(x: Int, y: Int, msg: String) {
        if (x == 4 && y == 9) {
            ChatUtils.sendWithPrefix("[DEBUG $x,$y] $msg")
        }
    }


    private fun CropStage.matchesStage(
        origin: BlockPos,
        remainingStands: List<Entity>
    ): MatchResult {

        val level = Minecraft.getInstance().level ?: return MatchResult(false, 0, emptyList(), emptyMap())

        var score = 0
        val usedStands = mutableListOf<Entity>()
        val matchedBlocks = mutableMapOf<BlockPos,BlockState>()

        this.blocks?.forEach { blockDef ->
            val pos = origin.offset(blockDef.offset)
            val state = level.getBlockState(pos)

            if (!blockDef.matcher(state)) {
                return MatchResult(false, 0, emptyList(), emptyMap())
            }
            matchedBlocks[pos] = state
            score += 1
        }

        this.armorStands?.forEach { standDef ->

            val center = Vec3(
                origin.x + 0.5,
                origin.y.toDouble(),
                origin.z + 0.5
            )

            val match = remainingStands.firstOrNull { entity ->
                if (entity !is ArmorStand) return@firstOrNull false

                val offset = entity.position().subtract(center)

                val head = entity.getItemBySlot(EquipmentSlot.HEAD)
                val hash = PlayerUtils.getSkinHash(head)

                isClose(offset, standDef.offset) &&
                        standDef.matcher(hash)
            } ?: return MatchResult(false, 0, emptyList(), emptyMap())

            usedStands.add(match)
            score += 2
        }

        return MatchResult(
            matched = true,
            score = score,
            usedStands = usedStands,
            matchedBlocks = matchedBlocks
        )
    }
    private fun isClose(a: Vec3, b: Vec3, epsilon: Double = 0.01): Boolean {
        return abs(a.x - b.x) < epsilon &&
                abs(a.y - b.y) < epsilon &&
                abs(a.z - b.z) < epsilon
    }



     fun Plot.getBuildableArea(): AABB {
         val box = this.aabb
        val minX = box.minX + BUILD_OFFSET
        val minZ = box.minZ + BUILD_OFFSET

        return AABB(
            minX, box.minY,
            minZ,
            minX + GRID_SIZE, box.maxY,
            minZ + GRID_SIZE
        )
    }


    fun isInitialized(plotId: Int): Boolean {
        return plotId in initializedGreenhouseIds
    }

    fun getCurrentGrid(): GreenhouseGrid? {
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return null
        return greenhouseList.find { it.plot?.id == plotId }
    }

    fun clearAllData(){
        knownGreenhouseIds.clear()
        initializedGreenhouseIds.clear()
        greenhouseList.clear()
        knownIdsInitialized = false
    }


    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (!baseSetting.value) return
        initKnownIds()
        initData()
    }

    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        if (!baseSetting.value) return
        if (event.new != SkyBlockIsland.GARDEN) return
        initKnownIds()
        initData()
    }

    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onInventory(event: ContainerInitializedEvent) {
        if (event.title != "Crop Diagnostics"){
            listeningElement = null
            return
        }
        if (listeningElement == null) return
        val realItems = event.containerItems.filter { !it.isSkyblockFiller() }
        val identifyStack = realItems[0]
        val stackId = identifyStack.getSkyBlockId()
        val useNameFallback = stackId == null
        if (useNameFallback) {
            if (!identifyStack.getLore().any { it.string.contains("Base Crop")}) {

                return
            }
        }


    }


    @EventHandler
    fun onBlockBreak(event: OnBlockDestroyedEvent) {

        val plotId = PlotAPI.getCurrentPlot()?.id ?: return
        if (!isInitialized(plotId)) return

        val grid = getCurrentGrid() ?: return

        val pos = event.pos
        val blockCenter = Vec3.atCenterOf(pos)

        if (grid.plot?.aabb?.contains(blockCenter) != true) return

        val slot = grid.getSlotAt(pos)
        if (slot != null) {
            slot.placedBlock = Blocks.AIR.defaultBlockState()
            grid.setSlot(slot)
            return
        }

        val iterator = grid.elements.iterator()

        while (iterator.hasNext()) {
            val element = iterator.next()

            val blocksMap = element.blocksMap ?: continue

            if (!blocksMap.containsKey(pos)) continue

            val originSlot = element.origin

            iterator.remove()

            grid.elementInstances.removeIf { instance ->
                instance.slot == originSlot
            }

            break
        }
    }
    @EventHandler
    fun onBlockPlaced(event: OnBlockPlacedEvent) {
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return
        if (!isInitialized(plotId)) return

        val grid = getCurrentGrid() ?: return

        val blockVec3 = Vec3.atCenterOf(event.pos)
        if (grid.plot?.aabb?.contains(blockVec3) != true) return

        val changedSlot = grid.getSlotAt(event.pos) ?: return

        changedSlot.placedBlock = event.blockState
        grid.setSlot(changedSlot)
    }
    // todo add item use for hoeing and fire
    // (maybe use block updated for farmland since its already there?)


    @EventHandler
    fun onBlockUse(event: OnBlockUseEvent) {
        val plot = PlotAPI.getCurrentPlot() ?: return
        if (!isInitialized(plot.id)) return
        val plotArea = plot.getBuildableArea()
        val handItem = event.player.mainHandItem
        if (handItem.isEmpty) return

        val mainHandId = event.player.mainHandItem.getSkyBlockId()
        ChatUtils.sendWithPrefix("thingy: $mainHandId thingy id: ${mainHandId?.id}")
        ChatUtils.sendWithPrefix("block: ${event.hit.blockPos}")
        if ((mainHandId?.id ?: "") == "item:plant_diagnostics_tool"){
            tryGetDiagnosticData(event, plotArea)
        }
        if (event.hit.direction != Direction.UP) return // for planting.

    }

    fun tryGetDiagnosticData(event: OnBlockUseEvent, plotArea: AABB){
        if (!plotArea.contains(event.hit.blockPos.center)) return
        val grid = getCurrentGrid() ?: return
        val hitSlot = grid.getSlotAt(event.hit.blockPos, false)
        val hitElement = grid.elements.firstOrNull {
            it.origin == hitSlot
        }
        listeningElement = hitElement
    }



    data class MatchResult(
        val matched: Boolean,
        val score: Int,
        val usedStands: List<Entity>,
        val matchedBlocks: Map<BlockPos, BlockState>
    )

}