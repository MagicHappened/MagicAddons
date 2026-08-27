package org.magic.magicaddons.data.greenhouse

import org.magic.magicaddons.util.getBuildableArea
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.elementsBySoil
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import java.time.Instant

/** The y every greenhouse plants on, the grid is a single flat row of soil. */
const val GREENHOUSE_SOIL_Y: Int = 73

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
    fun slotEquals(slot: LayoutSlot): Boolean {
        return layout.getSlot(slot.x,slot.y)?.placedBlock == slot.placedBlock
    }

    fun getPosForSlot(slot: LayoutSlot): BlockPos? {
        val box = plot?.getBuildableArea() ?: return null

        val minX = box.minX.toInt()
        val minZ = box.minZ.toInt()

        val worldX = minX + slot.x
        val worldZ = minZ + slot.y

        return BlockPos(worldX, GREENHOUSE_SOIL_Y, worldZ)
    }

    fun getPosForSlotCoords(x: Int, y: Int): BlockPos? {
        layout.getSlot(x,y)?.let {
            return getPosForSlot(it)
        }
        return null
    }

    fun getSlotAt(blockPos: BlockPos, matchY: Boolean = true): LayoutSlot? {
        val buildArea = plot?.getBuildableArea() ?: return null

        if (!buildArea.contains(Vec3.atCenterOf(blockPos))) return null

        if (matchY && blockPos.y != GREENHOUSE_SOIL_Y) return null

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        val gridX = blockPos.x - minX
        val gridY = blockPos.z - minZ

        return layout.getSlot(gridX, gridY)
    }


    /**
     * The element standing on [slot], including a crop bigger than one slot that covers it from its
     * top left corner, which is the only slot such a crop is filed under.
     */
    fun elementCovering(slot: LayoutSlot): ElementRuntimeState? = elements.find { element ->
        val origin = element.instance.slot
        val footprint = element.instance.cropDef.footprint

        slot.x in origin.x until origin.x + footprint.width &&
                slot.y in origin.y until origin.y + footprint.height
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

    fun createSlotData(): List<LayoutSlot>? {
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

            LayoutSlot(
                gridX,
                gridY,
                state
            )
        }
    }

    fun createSlotDataForGrid() {
        val world = Minecraft.getInstance().level ?: return
        val plot = PlotAPI.getCurrentPlot() ?: return
        if (plot != this.plot) return

        val buildArea = plot.getBuildableArea()

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        for (index in 0 until (width * height)) {
            val gridX = index % width
            val gridY = index / width

            val worldX = minX + gridX
            val worldZ = minZ + gridY

            val state = world.getBlockState(
                BlockPos(worldX, 73, worldZ)
            )

            layout.getSlot(gridX, gridY)?.let {
                it.placedBlock = state
            }
        }
    }


    //todo need to make this also set plant specific data, aka if candidate "Fleshtrap" was found
    // already handling it need to see the when statement working for fleshtrap once someone grows some
    // (or me)
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

                val def = runtime.instance.cropDef

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
                    val result = stage.matchesStage(pos, remainingStands, candidate.footprint)
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

                var elementStands = bestUsedStands
                val originVec = Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                val addedVec = originVec.add(
                    Vec3(
                        bestDef.footprint.width.toDouble(),
                        5.0, //see if this needs more
                        bestDef.footprint.height.toDouble()
                    )
                )
                val footprintBox = AABB(originVec, addedVec)
                when (bestDef.name) {
                    "Fleshtrap" -> {
                        val possibleStands = remainingStands.filter { footprintBox.contains(it.position()) }

                        val bonusValueStand = possibleStands.find { it.customName?.contains(
                            Component.literal("Bonus")
                        ) ?: false }
                        ChatUtils.sendWithPrefix("Bonus stand: ${bonusValueStand?.customName}")

                        val hungerStand = possibleStands.find { it.customName?.contains(
                            Component.literal("||||||||||||||||||||")
                        ) ?: false }

                        ChatUtils.sendWithPrefix("hunger stand: ${hungerStand?.customName}")

                        possibleStands.forEach {
                            if (it.hasCustomName()){
                                ChatUtils.sendWithPrefix(it.customName!!) //hopefully good
                            }
                        }
                    }
                }

                val instance = GreenhouseElementInstance(
                    bestDef.skyblockId?.id ?: bestDef.name,
                    slot = LayoutSlot(
                        0, 0, state
                    ),
                    growthStage = bestGrowth,
                    cropDef = bestDef
                )

                val runtime = ElementRuntimeState(
                    instance = instance,
                    standEntities = elementStands,
                    blocksMap = bestBlocks
                )

                return runtime
            }

            return null
        }
    }

    fun findElementAtSlot(
        slot: LayoutSlot,
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
            var elementStands = bestUsedStands
            val originVec = Vec3(origin.x.toDouble(), origin.y.toDouble(), origin.z.toDouble())
            val addedVec = originVec.add(
                Vec3(
                    bestDef.footprint.width.toDouble(),
                    5.0, //see if this needs more
                    bestDef.footprint.height.toDouble()
                )
            )
            val footprintBox = AABB(originVec, addedVec)
            when (bestDef.name) {
                "Fleshtrap" -> {
                    val possibleStands = remainingStands.filter { footprintBox.contains(it.position()) }

                    val bonusValueStand = possibleStands.find { it.customName?.contains(
                        Component.literal("Bonus")
                    ) ?: false }
                    ChatUtils.sendWithPrefix("Bonus stand: ${bonusValueStand?.customName}")

                    val hungerStand = possibleStands.find { it.customName?.contains(
                        Component.literal("||||||||||||||||||||")
                    ) ?: false }

                    ChatUtils.sendWithPrefix("hunger stand: ${hungerStand?.customName}")
                }
            }



            val instance = GreenhouseElementInstance(
                bestDef.skyblockId?.id ?: bestDef.name,
                slot = slot,
                growthStage = bestGrowth,
                cropDef = bestDef

            )

            val runtime = ElementRuntimeState(
                instance = instance,
                standEntities = elementStands,
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