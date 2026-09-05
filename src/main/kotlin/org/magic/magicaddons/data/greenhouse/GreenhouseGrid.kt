package org.magic.magicaddons.data.greenhouse

import org.magic.magicaddons.Common
import org.magic.magicaddons.util.getBuildableArea
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.elementsBySoil
import org.magic.magicaddons.features.farming.greenhousePresets.DyingPlant
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
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


    /** The element standing on a slot, including a big crop covering it from its corner. */
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


    //todo set plant specific data too, such as a found Fleshtrap's hunger, once one can be grown
    /**
     * Reads the plot and brings the elements into line with it. A merge, not a rebuild: a plant
     * still in its slot keeps its age, water and confirmed stage. Returns what changed.
     */
    fun setPlantData(): ReconcileResult {
        val visitedSlots = Array(width) { BooleanArray(height) }

        val level = Minecraft.getInstance().level ?: return ReconcileResult()
        val buildableArea = plot?.getBuildableArea() ?: return ReconcileResult()

        // a greenhouse is full of plot marker stands that hold nothing and belong to no plant, so a
        // crop stand described without a skull would bind to one
        val remainingStands = level.getEntitiesOfClass(ArmorStand::class.java, buildableArea)
            .filterNot { it.isMarker }
            .toMutableList()

        // from the instances, not the runtime wrappers: the wrappers are rebuilt every scan, while
        // the instances carry the age, water and stage, and are what goes to disk
        val previous = layout.elementInstances.associateBy { it.slot.x to it.slot.y }
        val reconciled = mutableListOf<ElementRuntimeState>()
        val result = ReconcileResult()

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (visitedSlots[y][x]) continue

                val slot = layout.getSlot(x, y) ?: continue

                val found = findElementAtSlot(slot, remainingStands) ?: continue
                Common.LOGGER.info("[scan] ${found.instance.cropDef.name} at slot (${slot.x}, ${slot.y}) stage ${found.instance.growthStage} placed=${found.instance.placed} stands=${found.standEntities?.size} blocks=${found.blocksMap?.size}")
                //todo catch mutation specifics here, such as fleshtrap hunger and bonus data, and
                // make sure they reach disk

                val def = found.instance.cropDef

                remainingStands.removeAll((found.standEntities ?: emptyList()).toSet())

                if (x + def.footprint.width > width ||
                    y + def.footprint.height > height
                ) continue

                for (dy in 0 until def.footprint.height) {
                    for (dx in 0 until def.footprint.width) {
                        visitedSlots[y + dy][x + dx] = true
                    }
                }

                val standing = previous[x to y]
                val runtime = if (standing != null && standing.elementId == found.instance.elementId) {
                    result.kept++
                    carryOver(standing, found)
                } else {
                    if (standing != null) result.replaced++ else result.added++

                    if (standing == null && def.isMutation && state.lastUpdateTimestamp != null) {
                        // a mutation the player just put down, or one that appeared above stage one
                        // while the plot was being watched, was placed: a spawn starts at stage one.
                        // Anything else where nothing stood at the last look grew there on its own
                        val placedNow = GreenhouseData.takePlacement(def, found.instance.slot, this)
                        val watched = state.hasRuntimeReferences && (found.instance.lowestStage ?: 1) > 1
                        if (found.instance.placed || placedNow || watched) {
                            GreenhouseData.claimPlacedPlant(found.instance)
                        } else {
                            GreenhouseData.claimSpawnedMutation(found.instance, layout)
                        }
                    }
                    found
                }

                reconciled.add(runtime)
            }
        }

        result.removed = previous.count { (key, _) ->
            reconciled.none { it.instance.slot.x to it.instance.slot.y == key }
        }

        elements.clear()
        elements.addAll(reconciled)

        layout.elementInstances.clear()
        layout.elementInstances.addAll(reconciled.map { it.instance })

        return result
    }

    /**
     * The plant the world just described, wearing what was known about the one already standing
     * there: the world knows the crop and its stage, not when it was planted or what it holds.
     */
    private fun carryOver(
        standing: GreenhouseElementInstance,
        found: ElementRuntimeState
    ): ElementRuntimeState {
        found.instance.age = standing.age

        // the plant is the one that was standing here, so it keeps the stage it was first seen at
        // rather than the one this scan happens to find it at
        found.instance.firstSeenStage = standing.firstSeenStage ?: found.instance.lowestStage
        found.instance.placed = standing.placed

        // predicted past death and still standing means ticks were skipped. The fewest that leave
        // it alive put it one tick from dying, so that is assumed and said out loud
        val water = standing.waterLevel

        if (water != null && water <= WaterModel.DEATH && found.instance.needsWater) {
            found.instance.waterLevel =
                WaterModel.aliveFloor(water, layout.waterEffectAt(found.instance.slot))
            found.instance.waterPredictedInDebt = true

            GreenhouseData.warnSurvivor(
                DyingPlant(found.instance.cropDef.name, layout.displayName(), layout.id)
            )
        } else {
            found.instance.waterLevel = water
            found.instance.waterPredictedInDebt = standing.waterPredictedInDebt
            found.instance.waterExact = standing.waterExact
        }

        // a diagnostic pins a stage down to one number, a scan often cannot, so a reading already
        // taken is not thrown away for a guess that covers it
        val standingStage = standing.growthStage
        val foundStage = found.instance.growthStage

        if (standingStage is GrowthStageInfo.Known && foundStage is GrowthStageInfo.Estimated &&
            standingStage.stage in foundStage.range
        ) {
            found.instance.growthStage = standingStage
        }

        return found
    }

    /**
     * Moves every plant on by that many ticks, for a greenhouse nobody is standing in. Always an
     * estimate: a plant in water debt may not have advanced at all. Water model: notes/water-formula.md.
     */
    fun predictGrowth(ticks: Int, tickMs: Long) {
        if (ticks <= 0) return

        // the plants themselves, not the runtime wrappers: a wrapper only exists while the plot is
        // loaded, which is never true of the greenhouse this is for
        layout.elementInstances.forEach { instance ->
            val maxStage = instance.cropDef.maxStage

            // a finished plant stops drinking, so no water is taken off one. Judged by the lowest
            // stage it might be at, so a plant only probably grown keeps drying
            val lowestStage = when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> stage.stage
                is GrowthStageInfo.Estimated -> stage.range.first
                null -> null
            }

            if (lowestStage != null && lowestStage >= maxStage) {
                instance.age = instance.age?.plus(ticks * tickMs)
                return@forEach
            }

            // a sleeping snoozling, a noctilume craving the other time of day and a starved fleshtrap
            // are all stuck, and all still dry out: being stuck is not being spared
            val cravingUnfulfilled = instance.craving?.let { it != timeOfDayNow() } == true

            // a plant in debt may be passed over entirely and nothing here can know, so the loss is
            // counted anyway and the plant remembers that it is a worst case
            val inDebt = (instance.waterLevel ?: 0) < 0
            if (inDebt) instance.waterPredictedInDebt = true

            if (instance.isAsleep || cravingUnfulfilled || instance.isStarving) {
                if (instance.needsWater) {
                    instance.waterLevel = instance.waterLevel?.let {
                        WaterModel.after(it, ticks, layout.waterEffectAt(instance.slot))
                    }
                }

                return@forEach
            }

            instance.age = instance.age?.plus(ticks * tickMs)

            if (instance.needsWater) {
                instance.waterLevel = instance.waterLevel?.let {
                    WaterModel.after(it, ticks, layout.waterEffectAt(instance.slot))
                }
            }

            val range = when (val stage = instance.growthStage) {
                is GrowthStageInfo.Known -> stage.stage..stage.stage
                is GrowthStageInfo.Estimated -> stage.range
                null -> return@forEach
            }

            // a snoozling drops asleep on arriving at a sleep stage, so the ticks after it were never
            // served and it must not be walked past
            val sleepStages = instance.cropDef.sleepStages

            fun ceiling(from: Int): Int =
                sleepStages.filter { it > from }.minOrNull()?.coerceAtMost(maxStage) ?: maxStage

            // in debt every tick may have been skipped, so the low end stays where it was while the
            // high end takes every tick: a 3 becomes 3 to 4
            val first = if (inDebt) range.first else (range.first + ticks).coerceAtMost(ceiling(range.first))
            val last = (range.last + ticks).coerceAtMost(ceiling(range.last))

            instance.growthStage = GrowthStageInfo.Estimated(first..last)

            // judged by the lowest it might be at, so a plant only probably asleep is still called
            // awake: the warning for one that has stopped growing is worth being sure about
            if (first in sleepStages) instance.readings[CropStandReader.ASLEEP] = 1
        }
    }

    /** What one reconcile did, counted by what happened to each plant. */
    data class ReconcileResult(
        var added: Int = 0,
        var removed: Int = 0,
        var replaced: Int = 0,
        var kept: Int = 0
    ) {
        val changed: Boolean get() = added > 0 || removed > 0 || replaced > 0
    }

    // temp for testing
    companion object {

        /** The garden clock as a craving value: its custom time reaches the client as world time. */
        fun timeOfDayNow(): Int {
            val time = (Minecraft.getInstance().level?.overworldClockTime ?: 0L) % 24000L

            return if (time in 13000L..22999L) {
                CropStandReader.CRAVES_NIGHT
            } else {
                CropStandReader.CRAVES_DAY
            }
        }

        /**
         * The plant standing at a position, or null when nothing described matches. The soil is
         * taken as given, since the scan and the exporter know it different ways.
         */
        fun findElementAt(
            origin: BlockPos,
            soil: Block,
            remainingStands: MutableList<ArmorStand>,
            slot: LayoutSlot
        ): ElementRuntimeState? {
            val candidates = elementsBySoil[soil] ?: return null

            var bestDef: CropDefinition? = null
            var bestGrowth: GrowthStageInfo? = null
            var bestStage: CropStage? = null
            var bestScore = -1
            var bestUsedStands: List<Entity>? = null
            var bestBlocks: Map<BlockPos, BlockState>? = null
            var bestLegacy = false

            for (candidate in candidates) {
                val stages = candidate.stageDefs.flatMap {
                    when (it) {
                        is CropStagePattern -> it.expand()
                        is CropStage -> listOf(it)
                    }
                }

                for (stage in stages) {
                    val result = stage.matchesStage(origin, remainingStands, candidate.footprint, candidate.rotatesWithPlot)

                    if (!result.matched) continue
                    if (result.score <= bestScore) continue

                    bestScore = result.score
                    bestDef = candidate
                    bestStage = stage
                    bestUsedStands = result.usedStands
                    bestBlocks = result.matchedBlocks
                    bestLegacy = result.rotationLegacy

                    val range = stage.stageRange
                    bestGrowth = if (range.first == range.last) {
                        GrowthStageInfo.Known(range.first)
                    } else {
                        GrowthStageInfo.Estimated(range)
                    }
                }
            }

            val definition = bestDef ?: return placedWithoutLook(origin, soil, remainingStands, slot)

            // the dex cannot see from the data which stages predate normalized exports, so it
            // learns from every match that only got there through the rotation fallback
            if (bestLegacy) bestStage?.let { PlantDex.noteLegacy(definition.name, it.stageRange) }

            val instance = GreenhouseElementInstance(
                definition.skyblockId?.id ?: definition.name,
                slot = slot,
                growthStage = bestGrowth,
                cropDef = definition
            )

            // where this plant enters our records, overwritten in the reconcile by whatever the
            // plant already standing here carried
            instance.firstSeenStage = instance.lowestStage

            // matched through its placed look, so it was put down, whoever remembers it or not;
            // and the memory of putting it down has served its purpose
            if (bestStage?.placed == true) instance.placed = true
            GreenhouseData.forgetPlacementAt(origin)

            // what winning this stage implies, filed before the stand readings: a noctilume's craving
            // is carried by which skull matched
            bestStage?.traits?.let { instance.readings.putAll(it) }

            // read after matching and from every stand around the plant: a hunger bar belongs to the
            // plant without being part of what makes it that plant
            bestStage?.read(standsAround(origin, definition.footprint))
                ?.let { instance.readings.putAll(it) }

            return ElementRuntimeState(
                instance = instance,
                standEntities = bestUsedStands,
                blocksMap = bestBlocks,
                rotationLegacy = bestLegacy
            )
        }

        /**
         * The crop the player put down here this session, when nothing recorded matches what stands
         * on the soil: taken as that crop at its placed stage, flagged placed, with whatever stands
         * and blocks sit in its footprint. Without this a placed mutation with no placed look
         * recorded was invisible, and the collector could never learn it was placed.
         */
        private fun placedWithoutLook(
            origin: BlockPos,
            soil: Block,
            remainingStands: MutableList<ArmorStand>,
            slot: LayoutSlot
        ): ElementRuntimeState? {
            val definition = GreenhouseData.placedHereAt(origin) ?: return null
            if (soil !in definition.requiredSoil) return null

            val level = Minecraft.getInstance().level ?: return null
            val footprint = definition.footprint
            val stands = standsAround(origin, footprint).filter { it in remainingStands }
            val blocks = mutableMapOf<BlockPos, BlockState>()
            for (dx in 0 until footprint.width) {
                for (dz in 0 until footprint.height) {
                    for (dy in 1..READER_HEIGHT) {
                        val pos = origin.offset(dx, dy, dz)
                        val state = level.getBlockState(pos)
                        if (!state.isAir) blocks[pos] = state
                    }
                }
            }
            if (stands.isEmpty() && blocks.isEmpty()) {
                GreenhouseData.forgetPlacementAt(origin)
                return null
            }

            val instance = GreenhouseElementInstance(
                definition.skyblockId?.id ?: definition.name,
                slot = slot,
                growthStage = GrowthStageInfo.Known(definition.stagePlacedAt),
                cropDef = definition
            )
            instance.placed = true
            instance.firstSeenStage = definition.stagePlacedAt

            return ElementRuntimeState(instance = instance, standEntities = stands, blocksMap = blocks)
        }

        /** Every stand sharing the space a crop of [footprint] occupies from [origin]. */
        private fun standsAround(origin: BlockPos, footprint: Footprint): List<ArmorStand> {
            val level = Minecraft.getInstance().level ?: return emptyList()

            val box = AABB(
                origin.x.toDouble(),
                origin.y.toDouble(),
                origin.z.toDouble(),
                (origin.x + footprint.width).toDouble(),
                (origin.y + READER_HEIGHT).toDouble(),
                (origin.z + footprint.height).toDouble()
            )

            return level.getEntitiesOfClass(ArmorStand::class.java, box).filterNot { it.isMarker }
        }

        /** How far above the soil a plant may hang something worth reading. */
        private const val READER_HEIGHT: Int = 5

    }

    /** The plant standing on [slot], through the one matcher. */
    fun findElementAtSlot(
        slot: LayoutSlot,
        remainingStands: MutableList<ArmorStand>
    ): ElementRuntimeState? {
        val soil = slot.placedBlock?.block ?: return null
        val origin = getPosForSlot(slot) ?: return null

        return findElementAt(origin, soil, remainingStands, slot)
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
            .filterNot { it.isMarker }
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
        var pendingGrowthTicks: Int? = null,
        /** Whether the player is tired of hearing this particular greenhouse's plan is finished. */
        var completionMuted: Boolean = false
    )

    override fun toString(): String = layout.displayName()
}