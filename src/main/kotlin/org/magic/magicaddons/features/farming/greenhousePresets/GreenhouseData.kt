package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
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
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
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

        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {

                if (visitedSlots[y][x]) continue
                val slot = grid.getSlot(x, y)

                val pair = matchElementAtPos(grid, slot) ?: continue

                val def = pair.first.definition

                for (dy in y until def.footprint.height){
                    for (dx in x until def.footprint.width){
                        visitedSlots[dy][dx] = true
                    }
                }

                grid.elementInstances.add(
                    GreenhouseElementInstance(
                        elementId = def.skyblockId.toString(),
                        originX = x,
                        originY = y,
                        growthStage = GrowthStageInfo.Estimated(pair.second)
                    )
                )

                grid.elements.add(
                    def
                )
            }
        }


    }

    private fun matchElementAtPos(grid: GreenhouseGrid, slot: GreenhouseSlot?): Pair<CropDefinitionProvider, IntRange>? {
        slot ?: return null
        val candidates = elementsBySoil[slot.placedBlock?.block]
        if (candidates.isNullOrEmpty()) return null

        val pos = grid.getPosForSlot(slot)
        pos ?: return null
        candidates.forEach { candidate ->
            if (slot.x + candidate.definition.footprint.width > GRID_SIZE || slot.y + candidate.definition.footprint.height > GRID_SIZE) return@forEach


            val stages = candidate.definition.stageDefs.flatMap {
                when (it) {
                    is CropStage -> listOf(it)
                    is CropStagePattern -> it.expand()
                }
            }

            stages.forEach {
                if (it.matchesStage(pos)){
                    return Pair(candidate,it.stageRange)
                }
            }

        }
        return null

    }

    private fun CropStage.matchesStage(
        origin: BlockPos
    ): Boolean {
        val level = Minecraft.getInstance().level ?: return false

        this.blocks?.forEach { blockDef ->
            val pos = origin.offset(blockDef.offset)
            val state = level.getBlockState(pos)

            if (!blockDef.matcher(state)) {
                return false
            }
        }

        // check armor stands
        this.armorStands?.forEach { standDef ->

            val box = AABB(
                origin.x.toDouble(),
                origin.y.toDouble() - 2,
                origin.z.toDouble(),
                origin.x + 1.0,
                origin.y.toDouble() + 4,
                origin.z + 1.0
            ) // todo fix it so it works on 3x3 plants cuz currently it cant scan for larger than 1x1

            val stands = level.getEntities(null, box)

            val matched = stands.any { entity ->
                if (entity !is ArmorStand) return@any false

                val center = Vec3.atCenterOf(origin)
                val offset = entity.position().subtract(center)

                val head = entity.getItemBySlot(EquipmentSlot.HEAD)
                val hash = PlayerUtils.getSkinHash(head)

                isClose(offset, standDef.offset) &&
                        standDef.matcher(hash)
            }

            if (!matched) return false
        }

        return true
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


    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        initKnownIds()
        initData()
    }

    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.new != SkyBlockIsland.GARDEN) return
        initKnownIds()
        initData()
    }

    @EventHandler
    fun onBlockBreak(event: OnBlockDestroyedEvent) {
        if (!baseSetting.value) return
        val plotId = PlotAPI.getCurrentPlot()?.id ?: return
        if (!isInitialized(plotId)) return

        val grid = getCurrentGrid() ?: return

        val blockVec3 = Vec3.atCenterOf(event.pos)
        if (grid.plot?.aabb?.contains(blockVec3) != true) return
        val changedSlot = grid.getSlotAt(event.pos)
        changedSlot ?: return
        changedSlot.placedBlock = Blocks.AIR.defaultBlockState()
        grid.setSlot(changedSlot)

    }

    @EventHandler
    fun onBlockPlaced(event: OnBlockPlacedEvent) {
        if (!baseSetting.value) return
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


}