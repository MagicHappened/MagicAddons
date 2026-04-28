package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseSlot
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnBlockDestroyedEvent
import org.magic.magicaddons.events.interact.OnBlockPlacedEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

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

        val grid = createGrid(plot, plot.aabb) ?: return

        greenhouseList.add(grid)
        initializedGreenhouseIds.add(plotId)

        ChatUtils.sendWithPrefix("Initialized Greenhouse: $plotId")
    }

    private fun createGrid(plot: Plot, box: AABB): GreenhouseGrid? {
        val world = Minecraft.getInstance().level ?: return null

        val buildArea = getBuildableArea(box)

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

     fun getBuildableArea(box: AABB): AABB {
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
        val blockVec3 = Vec3.atCenterOf(event.pos)
        val grid = getCurrentGrid() ?: return
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

}