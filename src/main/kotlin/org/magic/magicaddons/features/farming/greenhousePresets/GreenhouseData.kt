package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropRegistry
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
import org.magic.magicaddons.events.interact.OnAttackEntityEvent
import org.magic.magicaddons.events.interact.OnBlockDestroyedEvent
import org.magic.magicaddons.events.interact.OnBlockPlacedEvent
import org.magic.magicaddons.events.interact.OnBlockUpdatedEvent
import org.magic.magicaddons.events.interact.OnBlockUseEvent
import org.magic.magicaddons.events.interact.OnInteractEntityEvent
import org.magic.magicaddons.events.interact.OnUseEvent
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
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
    private val gridsMap = mutableListOf<GreenhouseGrid>()

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

    private var plantDiagnosticListeningElement: ElementRuntimeState? = null
    private var placedCrop: Pair<CropDefinition, BlockPos>? = null

    val greenhouses: List<GreenhouseGrid>
        get() = gridsMap

    val initializedIds: Set<Int>
        get() = initializedGreenhouseIds

    val knownIds: Set<Int>
        get() = knownGreenhouseIds

    val grids: List<GreenhouseGrid>
        get() = gridsMap






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

        gridsMap.add(grid)
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

        val stands = level.getEntities(null, buildableArea)
            .filterIsInstance<ArmorStand>()
        val remainingStands = stands.toMutableList()

        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                if (visitedSlots[y][x]) continue

                val slot = grid.getSlot(x, y) ?: continue

                val result = findElementAtSlot(grid, slot, remainingStands) ?: continue

                val runtime = result.runtime
                val def = result.runtime.cropDef

                remainingStands.removeAll(result.usedStands)

                if (def != null) {
                    if (x + def.footprint.width > GRID_SIZE ||
                        y + def.footprint.height > GRID_SIZE
                    ) continue

                    for (dy in 0 until def.footprint.height) {
                        for (dx in 0 until def.footprint.width) {
                            visitedSlots[y + dy][x + dx] = true
                        }
                    }
                } else {
                    // fire or non-footprint element
                    visitedSlots[y][x] = true
                }

                grid.elementInstances.add(
                    GreenhouseElementInstance(
                        elementId = def?.skyblockId?.toString() ?: "fire",
                        slot = slot,
                        growthStage = runtime.growthStage
                    )
                )

                grid.elements.add(runtime)
            }
        }
    }
    private fun findElementAtSlot(
        grid: GreenhouseGrid,
        slot: GreenhouseSlot,
        remainingStands: MutableList<ArmorStand>
    ): ElementMatchResult? {

        val level = Minecraft.getInstance().level ?: return null
        val soil = slot.placedBlock?.block ?: return null
        val candidates = elementsBySoil[soil] ?: return null
        val origin = grid.getPosForSlot(slot) ?: return null



        for (candidate in candidates) {
            val def = candidate.definition

            val stages = def.stageDefs.flatMap {
                when (it) {
                    is CropStagePattern -> it.expand()
                    is CropStage -> listOf(it)
                }
            }

            var bestStage: CropStage? = null
            var bestGrowth: GrowthStageInfo? = null
            var bestScore = -1
            var usedStands: List<Entity>? = null
            var blocks: Map<BlockPos, BlockState>? = null

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

            if (bestStage != null && usedStands != null) {
                val runtime = ElementRuntimeState(
                    cropDef = def,
                    origin = slot,
                    growthStage = bestGrowth,
                    waterLevel = null,
                    standEntities = usedStands,
                    blocksMap = blocks
                )

                return ElementMatchResult(runtime, usedStands)
            }
        }

        return null
    }



    fun matchesWithRotation(
        actual: Vec3,
        expected: Vec3,
        allowRotation: Boolean
    ): Boolean {
        if (!allowRotation) {
            return isClose(actual, expected)
        }

        val rotations = listOf(
            expected,
            Vec3(-expected.z, expected.y, expected.x),
            Vec3(-expected.x, expected.y, -expected.z),
            Vec3(expected.z, expected.y, -expected.x)
        )

        return rotations.any { rotated ->
            isClose(actual, rotated)
        }
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
        return gridsMap.find { it.plot?.id == plotId }
    }

    fun clearAllData(){
        knownGreenhouseIds.clear()
        initializedGreenhouseIds.clear()
        gridsMap.clear()
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
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onInventory(event: ContainerInitializedEvent) {
        if (event.title != "Crop Diagnostics"){
            plantDiagnosticListeningElement = null
            return
        }
        if (plantDiagnosticListeningElement == null) return
        val realItems = event.containerItems.filter { !it.isSkyblockFiller() }
        val identifyStack = realItems[0]
        val stackId = identifyStack.getSkyBlockId()
        val useNameFallback = stackId == null
        if (useNameFallback) {
            if (!identifyStack.getLore().any { it.string.contains("Base Crop")}) {
                ChatUtils.sendWithPrefix("Found Base Crop: $identifyStack")
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
    fun onAttackEntity(event: OnAttackEntityEvent){
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return
        if (!isInitialized(plotId)) return
        if (event.target !is ArmorStand) return

        // for now just remove the element later add cancellation with layouts
        val grid = getCurrentGrid() ?: return
        var removedSlot: GreenhouseSlot? = null
        grid.elements.removeIf { element ->

            val removed = (element.standEntities ?: return@removeIf false).any {
                it == event.target
            }
            if (removed){
                removedSlot = element.origin
            }
            removed
        }
        if (removedSlot != null){
            grid.elementInstances.removeIf {
                it.slot == removedSlot
            }
        }


    }

    @EventHandler
    fun onBlockPlaced(event: OnBlockPlacedEvent) {
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return
        if (!isInitialized(plotId)) return

        val grid = getCurrentGrid() ?: return

        val blockVec3 = Vec3.atCenterOf(event.pos)
        if (grid.plot?.aabb?.contains(blockVec3) != true) return
        val changedSlot = grid.getSlotAt(event.pos, false) ?: return

        if (placedCrop == null) return
        cropPlanted()

        placedCrop = null
    }

    @EventHandler
    fun onEntityAdded(event: OnEntityAdded){
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return
        if (!isInitialized(plotId)) return

        val grid = getCurrentGrid() ?: return
        val gridArea = grid.plot?.getBuildableArea() ?: return

        if (event.addedEntityList.any {
            !gridArea.contains(it.entity.position())
            }) return

        if (placedCrop == null) return
        cropPlanted()

        placedCrop = null
    }

    @EventHandler
    fun onBlockUpdated(event: OnBlockUpdatedEvent){
        val plot = PlotAPI.getCurrentPlot()?.id ?: return
        if (!isInitialized(plot)) return
        val grid = getCurrentGrid() ?: return
        val gridArea = grid.plot?.getBuildableArea() ?: return
        if (!gridArea.contains(event.packet.pos.center)) return
        val slot = grid.getSlotAt(event.packet.pos, false) ?: return
        if (event.packet.pos.y == 74){
            if (event.packet.blockState.block == Blocks.FIRE){
                return
            }
            else {
                ChatUtils.sendWithPrefix("Tick detected?")
            }
        }

        if (event.packet.pos.y != 73 ) return

        slot.placedBlock = event.packet.blockState
        grid.setSlot(slot)
    }

    private val waterCanIds = setOf(
        "HYDRO_CAN_1000",
        "HYDRO_CAN_TURBO_2000",
        "HYDRO_CAN_ULTRA_3000",
        "AQUAMASTER_X",
        "AQUAMASTER_HYDROMAX"

    )

    @EventHandler
    fun onInteractEntity(event: OnInteractEntityEvent) {
        val plot = PlotAPI.getCurrentPlot() ?: return
        if (!isInitialized(plot.id)) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return
        val standTarget = event.target as? ArmorStand ?: return
        val grid = getCurrentGrid() ?: return
        if (mainHandId.id == "item:plant_diagnostics_tool"){
            tryGetDiagnosticData(null, standTarget, grid)
            return
        }
    }


    @EventHandler
    fun onItemUse(event: OnUseEvent){
        val plot = PlotAPI.getCurrentPlot() ?: return
        if (!isInitialized(plot.id)) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return

        if (("item:"+mainHandId.id) in waterCanIds){
            tryGetWaterCanData()
            return
        }
    }

    @EventHandler
    fun onBlockUse(event: OnBlockUseEvent) {
        val plot = PlotAPI.getCurrentPlot() ?: return
        if (!isInitialized(plot.id)) return
        val mainHandId = event.player.mainHandItem.getSkyBlockId() ?: return
        val grid = getCurrentGrid() ?: return
        val foundCrop = CropRegistry.all.firstOrNull { it.matchesId(mainHandId) }

        if (mainHandId.id == "item:plant_diagnostics_tool"){
            tryGetDiagnosticData(event.hit.blockPos, null, grid)
            return
        }
        if (("item:"+mainHandId.id) in waterCanIds){
            tryGetWaterCanData()
            return
        }

        if (foundCrop != null) {
            val pos = event.hit.blockPos.relative(event.hit.direction)

            placedCrop = Pair(foundCrop, pos)

            return
        }


    }

    fun cropPlanted(){
        if (placedCrop == null) return
        val grid = getCurrentGrid() ?: return
        val availableStands = grid.getUnassignedArmorStands()?.toMutableList() ?: return
        val slot = grid.getSlotAt(placedCrop!!.second, false) ?: return
        val result = findElementAtSlot(grid,slot, availableStands)
        result ?: return
        if (result.runtime.cropDef?.needsWater ?: false){
            result.runtime.waterLevel = 0
        }
        grid.elements.add(result.runtime)
        grid.elementInstances.add(
            GreenhouseElementInstance(
                result.runtime.cropDef?.skyblockId?.id ?: result.runtime.nameOverride
                ?: throw IllegalStateException("Unexpected null at crop def or nameOverride for slot: ${slot.x},${slot.y}"),
                slot,
                0, //todo add water
                result.runtime.growthStage
            )
        )

    }
    fun tryGetWaterCanData(){

    }

    fun tryGetDiagnosticData(hitBlock: BlockPos? = null,hitEntity: ArmorStand? = null, grid: GreenhouseGrid){
        var hitElement: ElementRuntimeState? = null

        if (hitBlock != null){
            hitElement = grid.elements.find {
                it.blocksMap?.keys?.contains(hitBlock) ?: return@find false
            }
        }
        if (hitEntity != null && hitElement != null){
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

    data class ElementMatchResult( //todo depreciate this and just remove the used stands from runtime.standEntities
        val runtime: ElementRuntimeState,
        val usedStands: List<Entity>
    )


}