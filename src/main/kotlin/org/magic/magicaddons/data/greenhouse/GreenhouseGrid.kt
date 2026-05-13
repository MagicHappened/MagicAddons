package org.magic.magicaddons.data.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.elementsBySoil
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.getBuildableArea
import org.magic.magicaddons.util.ScreenUtil
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import kotlin.collections.removeAll

class GreenhouseGrid(
    var state: GridState,
    var layout: GreenhouseLayout
) {
    var plot: Plot? = null
    val width = 10
    val height = 10

    val elements = mutableListOf<ElementRuntimeState>()

    fun addElement(element: ElementRuntimeState){
        layout.elementInstances += element.instance
        elements.add(element)
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

    fun createSlotData() {
        val world = Minecraft.getInstance().level ?: return
        val plot = PlotAPI.getCurrentPlot() ?: return
        if (plot != this.plot) return

        val buildArea = plot.getBuildableArea()

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()
        val maxX = buildArea.maxX.toInt()
        val maxZ = buildArea.maxZ.toInt()

        var gridX = 0
        for (x in minX until maxX) {
            var gridY = 0
            for (z in minZ until maxZ) {

                val state = world.getBlockState(BlockPos(x, 73, z))

                layout.getSlot(gridX, gridY)?.placedBlock = state

                gridY++
            }

            gridX++
        }
    }

    fun setPlantData() {
        val visitedSlots = Array(width) { BooleanArray(height) }

        val level = Minecraft.getInstance().level ?: return
        val buildableArea = plot?.getBuildableArea() ?: return

        val stands = level.getEntities(null, buildableArea)
            .filterIsInstance<ArmorStand>()
        val remainingStands = stands.toMutableList()

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (visitedSlots[y][x]) continue

                val slot = layout.getSlot(x, y) ?: continue

                val runtime = findElementAtSlot(slot, remainingStands) ?: continue


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
                val result = stage.matchesStage(origin, remainingStands)

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
            it.standEntities?.let { elements -> stands.removeAll(elements) }
        }
        return stands.toList()
    }

    data class GridState(
        var lastUpdateTimestamp: Long = -1,
        var needsUpdate: Boolean = false,
        var initialized: Boolean = false
    )

    override fun toString(): String {
        return "${layout.name?: "unnamed"}: ${layout.id}"
    }
}