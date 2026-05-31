package org.magic.magicaddons.data.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.elementsBySoil
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.getBuildableArea
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import java.time.Instant

class GreenhouseGrid(
    var state: GridState,
    var layout: GreenhouseLayout
) {
    var plot: Plot? = null
    val width = 10
    val height = 10

    val elements = mutableListOf<ElementRuntimeState>()

    fun addElement(element: ElementRuntimeState, age: Long? = null) {
        if (age != null)
            element.instance.age = age
        layout.elementInstances += element.instance
        elements.add(element)
    }

    fun hasRuntime(): Boolean {
        return state.hasRuntimeReferences
    }

    fun getPosForSlot(slot: GreenhouseSlot): BlockPos? {
        val box = plot?.getBuildableArea() ?: return null

        val minX = box.minX.toInt()
        val minZ = box.minZ.toInt()

        val worldX = minX + slot.x
        val worldZ = minZ + slot.y

        return BlockPos(worldX, 73, worldZ)
    }


    fun getSlotAt(blockPos: BlockPos, matchY: Boolean = true): GreenhouseSlot? {
        val buildArea = plot?.getBuildableArea() ?: return null

        if (!buildArea.contains(Vec3.atCenterOf(blockPos))) return null

        if (matchY && blockPos.y != 73) return null

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        val gridX = blockPos.x - minX
        val gridY = blockPos.z - minZ

        return layout.getSlot(gridX, gridY)
    }


    fun removeMatchingBlock(blockPos: BlockPos): ElementRuntimeState? {
        return elements.find { element ->
            element.blocksMap
                ?.keys
                ?.any { it == blockPos }
                ?: false
        }?.also {
            elements.remove(it)
            layout.elementInstances.remove(it.instance)
        }
    }

    fun removeMatchingEntity(entity: Entity): ElementRuntimeState? {
        return elements.find { element ->
            element.standEntities?.any { it == entity } ?: false
        }?.also {
            elements.remove(it)
            layout.elementInstances.remove(it.instance)
        }
    }

    fun createSlotData(): List<GreenhouseSlot>? {
        val world = Minecraft.getInstance().level ?: return null
        val plot = PlotAPI.getCurrentPlot() ?: return null
        if (plot != this.plot) return null

        val buildArea = plot.getBuildableArea()

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        return List(width * height) { index ->

            val gridX = index % width
            val gridY = index / width

            val worldX = minX + gridX
            val worldZ = minZ + gridY

            val state = world.getBlockState(
                BlockPos(worldX, 73, worldZ)
            )

            GreenhouseSlot(
                gridX,
                gridY,
                state
            )
        }
    }

    //todo need to make this also set plant specific data, aka if candidate "Fleshtrap" was found
    // (prob like another interface with extra conditions?)
    // get the armor stand that represents its hunger status bonus status etc
    // same with snoozling sleeping (the right click to wake up stand)
    //todo NOT TO MENTION noctilume being 2 different definitions, 1 possible to split into 2 crops
    // but that will be a headache afterwards managing them without skyblock id and such so
    // probably change the matchesStage thing to handle it better
    fun setPlantData() {
        val visitedSlots = Array(width) { BooleanArray(height) }

        val level = Minecraft.getInstance().level ?: return
        val buildableArea = plot?.getBuildableArea() ?: return

        val stands = level.getEntitiesOfClass(ArmorStand::class.java, buildableArea)
        val remainingStands = stands.toMutableList()

        layout.elementInstances.clear()
        elements.clear()

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (visitedSlots[y][x]) continue

                val slot = layout.getSlot(x, y) ?: continue

                val runtime = findElementAtSlot(slot, remainingStands) ?: continue
                //todo insert code here that catches certain types of mutations:
                // aka Fleshtraps and other stuff that might arise, and add
                // catching of hunger and bonus data.
                // fuck i forgot it needs to be saved to disk fucking fleshtrap grr

                val def = runtime.cropDef

                remainingStands.removeAll((runtime.standEntities ?: emptyList()).toSet())

                if (x + def.footprint.width > width ||
                    y + def.footprint.height > height
                ) continue

                for (dy in 0 until def.footprint.height) {
                    for (dx in 0 until def.footprint.width) {
                        visitedSlots[y + dy][x + dx] = true
                    }
                }


                layout.elementInstances.add(runtime.instance)
                elements.add(runtime)
            }
        }
    }

    // temp for testing
    companion object {
        fun findElementAtBasePos(
            pos: BlockPos,
            remainingStands: MutableList<ArmorStand>
        ): ElementRuntimeState? {
            val level = Minecraft.getInstance().level ?: return null
            val state = level.getBlockState(pos)
            val soil = state.block
            val candidates = elementsBySoil[soil] ?: return null

            var bestDef: CropDefinition? = null
            var bestGrowth: GrowthStageInfo? = null
            var bestScore = -1
            var bestUsedStands: List<Entity>? = null
            var bestBlocks: Map<BlockPos, BlockState>? = null


            for (candidate in candidates) {

                val stages = candidate.stageDefs.flatMap {
                    when (it) {
                        is CropStagePattern -> it.expand()
                        is CropStage -> listOf(it)
                    }
                }
                for (stage in stages) {
                    val result = stage.matchesStage(pos, remainingStands, candidate.footprint, candidate.name == "Snoozling")
                    if (!result.matched) continue
                    if (result.score <= bestScore) {
                        continue
                    }

                    bestScore = result.score
                    bestDef = candidate
                    bestUsedStands = result.usedStands
                    bestBlocks = result.matchedBlocks

                    val range = stage.stageRange
                    bestGrowth = if (range.first == range.last) {
                        GrowthStageInfo.Known(range.first)
                    } else {
                        GrowthStageInfo.Estimated(range)
                    }
                }
            }
            if (bestDef != null) {
                val instance = GreenhouseElementInstance(
                    bestDef.skyblockId?.id ?: bestDef.name,
                    slot = GreenhouseSlot(
                        0, 0, state
                    ),
                    growthStage = bestGrowth

                )

                val runtime = ElementRuntimeState(
                    cropDef = bestDef,
                    instance = instance,
                    standEntities = bestUsedStands,
                    blocksMap = bestBlocks
                )

                return runtime
            }

            return null
        }
    }

    fun findElementAtSlot(
        slot: GreenhouseSlot,
        remainingStands: MutableList<ArmorStand>
    ): ElementRuntimeState? {

        val soil = slot.placedBlock?.block ?: return null
        val candidates = elementsBySoil[soil] ?: return null
        val origin = getPosForSlot(slot) ?: return null

        var bestDef: CropDefinition? = null
        var bestGrowth: GrowthStageInfo? = null
        var bestScore = -1
        var bestUsedStands: List<Entity>? = null
        var bestBlocks: Map<BlockPos, BlockState>? = null

        for (candidate in candidates) {

            val stages = candidate.stageDefs.flatMap {
                when (it) {
                    is CropStagePattern -> it.expand()
                    is CropStage -> listOf(it)
                }
            }

            for (stage in stages) {
                val result = stage.matchesStage(origin, remainingStands, candidate.footprint)

                if (!result.matched) continue
                if (result.score <= bestScore) continue

                bestScore = result.score
                bestDef = candidate
                bestUsedStands = result.usedStands
                bestBlocks = result.matchedBlocks

                val range = stage.stageRange
                bestGrowth = if (range.first == range.last) {
                    GrowthStageInfo.Known(range.first)
                } else {
                    GrowthStageInfo.Estimated(range)
                }
            }
        }

        if (bestDef != null) {
            val instance = GreenhouseElementInstance(
                bestDef.skyblockId?.id ?: bestDef.name,
                slot = slot,
                growthStage = bestGrowth

            )

            val runtime = ElementRuntimeState(
                cropDef = bestDef,
                instance = instance,
                standEntities = bestUsedStands,
                blocksMap = bestBlocks
            )

            return runtime
        }

        return null
    }


    fun getUnassignedBlockMap(): Map<BlockPos, BlockState> {
        val level = Minecraft.getInstance().level ?: return emptyMap()
        val area = plot?.getBuildableArea() ?: return emptyMap()

        val allBlocks = mutableMapOf<BlockPos, BlockState>()

        val minX = area.minX.toInt()
        val minZ = area.minZ.toInt()
        val maxX = area.maxX.toInt()
        val maxZ = area.maxZ.toInt()

        val minY = 74
        val maxY = 84

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    val pos = BlockPos(x, y, z)
                    val state = level.getBlockState(pos)

                    if (!state.isAir) {
                        allBlocks[pos] = state
                    }
                }
            }
        }

        val usedPositions = elements
            .flatMap { it.blocksMap?.keys ?: emptySet() }
            .toSet()

        return allBlocks.filterKeys { it !in usedPositions }
    }

    fun getUnassignedArmorStands(): List<ArmorStand>? {
        val level = Minecraft.getInstance().level ?: return null
        val area = plot?.getBuildableArea() ?: return null
        val stands = level.getEntities(null, area)
            .filterIsInstance<ArmorStand>()
            .toMutableList()
        elements.forEach {
            it.standEntities?.let { elements -> stands.removeAll(elements.toSet()) }
        }
        return stands.toList()
    }

    data class GridState(
        var lastUpdateTimestamp: Instant? = null,
        var needsUpdate: Boolean = false,
        var assignedLayout: GreenhouseLayout? = null,
        var hasRuntimeReferences: Boolean = false,
        var pendingGrowthTicks: Int? = null
    )

    override fun toString(): String {
        return "${layout.name ?: "unnamed"}: ${layout.id}"
    }
}